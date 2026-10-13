package com.cooperativa.utils;

import com.cooperativa.config.ConexionPostgres;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Copia consistente de Oracle a PostgreSQL mientras Oracle sigue atendiendo a la aplicación.
 * La base PostgreSQL es exclusiva para respaldo: cada ejecución reemplaza su contenido.
 * La recuperación exige un esquema Oracle vacío y emite el ajuste de secuencias por separado.
 */
public final class BackupEnCaliente {
    private static final int TAMANO_LOTE = 250;
    private static final int BLOQUEO_COOPERATIVA = 20261004;
    private static final int BLOQUEO_RESPALDO = 15;

    private BackupEnCaliente() { }

    public static Map<String, Long> respaldar() throws SQLException {
        Properties configuracion = ConexionPostgres.cargarConfiguracion();
        try (Connection origen = abrirOracle(configuracion, "db.");
             Connection destino = ConexionPostgres.abrir(configuracion)) {
            return respaldar(origen, destino);
        }
    }

    /** Las conexiones deben ser nuevas/sin transacción. El llamador las cierra. */
    public static Map<String, Long> respaldar(Connection oracle, Connection postgres) throws SQLException {
        exigirSinTransaccion(oracle, postgres);
        oracle.setAutoCommit(false);
        try {
            // Debe ser LA PRIMERA sentencia: no consultar tablas ni metadatos previamente.
            ejecutar(oracle, "SET TRANSACTION READ ONLY");
            exigirFuenteNoSys(oracle);
            postgres.setAutoCommit(false);
            bloquearRespaldo(postgres, false);
            for (int indice = EsquemaRespaldo.TABLAS.size() - 1; indice >= 0; indice--) {
                ejecutar(postgres, "DELETE FROM " + EsquemaRespaldo.TABLAS.get(indice).nombre);
            }
            Map<String, Long> conteos = copiarYVerificar(oracle, postgres);
            sincronizarSecuenciasPostgres(postgres);
            // Cerrar la foto antes de confirmar el destino: un error aquí también deshace la copia.
            oracle.rollback();
            confirmar(postgres);
            return Collections.unmodifiableMap(conteos);
        } catch (SQLException | RuntimeException | Error ex) {
            deshacer(postgres, ex);
            deshacer(oracle, ex);
            throw ex;
        }
    }

    /** Recupera en un usuario dedicado distinto al usuario de la aplicación. */
    public static Map<String, Long> recuperar(Path ajusteSecuencias) throws SQLException, IOException {
        Properties configuracion = ConexionPostgres.cargarConfiguracion();
        String usuarioOriginal = ConexionPostgres.requerida(configuracion, "db.user");
        String usuarioRestauracion = ConexionPostgres.requerida(configuracion, "restore.oracle.user");
        if (usuarioOriginal.equalsIgnoreCase(usuarioRestauracion)) {
            throw new SQLException("La recuperación requiere un usuario Oracle diferente a db.user.", "25000");
        }
        try (Connection origen = ConexionPostgres.abrir(configuracion);
             Connection destino = abrirOracle(configuracion, "restore.oracle.")) {
            return recuperar(origen, destino, ajusteSecuencias);
        }
    }

    /**
     * No borra datos Oracle. Bloquea y comprueba las 15 tablas vacías antes de insertar.
     * El SQL de secuencias se crea antes del commit y se elimina si falla antes de confirmarlo.
     * Si se pierde la conexión durante el commit, conserva el archivo: el resultado es incierto.
     * No ejecuta DDL en Oracle: ALTER SEQUENCE confirmaría implícitamente los datos a medias.
     */
    public static Map<String, Long> recuperar(Connection postgres, Connection oracle,
            Path ajusteSecuencias) throws SQLException, IOException {
        exigirSinTransaccion(postgres, oracle);
        if (ajusteSecuencias == null) {
            throw new IllegalArgumentException("Debe indicarse un archivo para ajustar las secuencias.");
        }
        boolean archivoCreado = false;
        boolean confirmacionIntentada = false;
        postgres.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        postgres.setReadOnly(true);
        postgres.setAutoCommit(false);
        try {
            bloquearRespaldo(postgres, true);
            oracle.setAutoCommit(false);
            exigirUsuarioPropio(oracle);
            for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
                ejecutar(oracle, "LOCK TABLE " + tabla.nombre + " IN EXCLUSIVE MODE NOWAIT");
            }
            for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
                if (contar(oracle, tabla) != 0) {
                    throw new SQLException("El esquema de recuperación contiene datos: " + tabla.nombre + ".", "25000");
                }
            }
            Map<String, Long> conteos = copiarYVerificar(postgres, oracle);
            // También comprueba que los identificadores dejan espacio para futuras altas.
            for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
                if (tabla.secuencia != null) { siguienteId(oracle, tabla, true); }
            }
            Path salida = ajusteSecuencias.toAbsolutePath();
            Files.createDirectories(salida.getParent());
            Files.write(salida, generarAjusteOracle().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            archivoCreado = true;
            postgres.rollback();
            confirmacionIntentada = true;
            confirmar(oracle);
            return Collections.unmodifiableMap(conteos);
        } catch (SQLException | IOException | RuntimeException | Error ex) {
            deshacer(oracle, ex);
            deshacer(postgres, ex);
            if (archivoCreado && !confirmacionIntentada) {
                try { Files.deleteIfExists(ajusteSecuencias.toAbsolutePath()); }
                catch (IOException falloLimpieza) { ex.addSuppressed(falloLimpieza); }
            }
            throw ex;
        }
    }

    private static Connection abrirOracle(Properties configuracion, String prefijo) throws SQLException {
        String url = ConexionPostgres.requerida(configuracion, prefijo + "url");
        if (!url.startsWith("jdbc:oracle:")) {
            throw new SQLException(prefijo + "url debe ser una URL JDBC Oracle.", "08001");
        }
        String usuario = ConexionPostgres.requerida(configuracion, prefijo + "user");
        if ("SYS".equalsIgnoreCase(usuario)) {
            throw new SQLException("SYS no garantiza la foto READ ONLY requerida; usar un esquema de la cooperativa.", "25000");
        }
        if ("restore.oracle.".equals(prefijo)
                && ("SYS".equalsIgnoreCase(usuario) || "SYSTEM".equalsIgnoreCase(usuario))) {
            throw new SQLException("La recuperación requiere un esquema propio; no utilizar SYS ni SYSTEM.", "25000");
        }
        try { Class.forName("oracle.jdbc.driver.OracleDriver"); }
        catch (ClassNotFoundException ex) {
            throw new SQLException("Falta lib/ojdbc14.jar en el classpath.", "08001", ex);
        }
        // Conexión física independiente: nunca toma una transacción del pool de la aplicación.
        return DriverManager.getConnection(url, usuario,
                ConexionPostgres.requerida(configuracion, prefijo + "password"));
    }

    private static void exigirUsuarioPropio(Connection oracle) throws SQLException {
        try (PreparedStatement consulta = oracle.prepareStatement("SELECT USER FROM dual");
             ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next() || "SYS".equalsIgnoreCase(resultado.getString(1))
                    || "SYSTEM".equalsIgnoreCase(resultado.getString(1))) {
                throw new SQLException("La recuperación requiere un esquema Oracle propio.", "25000");
            }
        }
    }

    private static void exigirFuenteNoSys(Connection oracle) throws SQLException {
        try (PreparedStatement consulta = oracle.prepareStatement("SELECT USER FROM dual");
             ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next() || "SYS".equalsIgnoreCase(resultado.getString(1))) {
                throw new SQLException("SYS no garantiza la foto READ ONLY requerida.", "25000");
            }
        }
    }

    private static void exigirSinTransaccion(Connection primera, Connection segunda) throws SQLException {
        if (!primera.getAutoCommit() || !segunda.getAutoCommit()) {
            throw new SQLException("Usar conexiones independientes sin transacción previa.", "25000");
        }
    }

    private static void bloquearRespaldo(Connection postgres, boolean soloLectura) throws SQLException {
        try (PreparedStatement consulta = postgres.prepareStatement("SELECT pg_try_advisory_xact_lock(?, ?)")) {
            consulta.setInt(1, BLOQUEO_COOPERATIVA);
            consulta.setInt(2, BLOQUEO_RESPALDO);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next() || !resultado.getBoolean(1)) {
                    throw new SQLException("Ya hay un respaldo o una recuperación en ejecución.", "55P03");
                }
            }
        }
        // En recuperación REPEATABLE READ conserva las versiones de todas las tablas.
        // No tomar SHARE: PostgreSQL prohíbe ese modo en transacciones READ ONLY.
        if (soloLectura) { return; }
        ejecutar(postgres, "SET LOCAL lock_timeout = '10s'");
        StringBuilder tablas = new StringBuilder();
        for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
            if (tablas.length() > 0) { tablas.append(", "); }
            tablas.append(tabla.nombre);
        }
        // Las consultas continúan; se impiden escrituras ajenas durante copia/conteos.
        ejecutar(postgres, "LOCK TABLE " + tablas + " IN SHARE ROW EXCLUSIVE MODE");
    }

    private static Map<String, Long> copiarYVerificar(Connection origen, Connection destino) throws SQLException {
        Map<String, Long> conteos = new LinkedHashMap<String, Long>();
        for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
            long filas = copiarTabla(origen, destino, tabla);
            long filasOrigen = contar(origen, tabla);
            long filasDestino = contar(destino, tabla);
            if (filas != filasOrigen || filas != filasDestino) {
                throw new SQLException("No coinciden los conteos de " + tabla.nombre + ".", "22000");
            }
            conteos.put(tabla.nombre, filas);
        }
        return conteos;
    }

    private static long copiarTabla(Connection origen, Connection destino,
            EsquemaRespaldo.Tabla tabla) throws SQLException {
        long filas = 0;
        try (PreparedStatement lectura = origen.prepareStatement(tabla.seleccionar());
             PreparedStatement escritura = destino.prepareStatement(tabla.insertar())) {
            lectura.setFetchSize(TAMANO_LOTE);
            try (ResultSet datos = lectura.executeQuery()) {
                while (datos.next()) {
                    for (int indice = 0; indice < tabla.tipos.length; indice++) {
                        copiarCampo(datos, escritura, indice + 1, tabla.tipos[indice]);
                    }
                    escritura.addBatch();
                    filas++;
                    if (filas % TAMANO_LOTE == 0) {
                        escritura.executeBatch();
                        escritura.clearBatch();
                    }
                }
            }
            if (filas % TAMANO_LOTE != 0) { escritura.executeBatch(); }
        }
        return filas;
    }

    private static void copiarCampo(ResultSet datos, PreparedStatement escritura,
            int columna, int tipo) throws SQLException {
        switch (tipo) {
            case Types.INTEGER:
            case Types.SMALLINT:
                long entero = datos.getLong(columna);
                if (datos.wasNull()) { escritura.setNull(columna, tipo); }
                else {
                    if (entero < Integer.MIN_VALUE || entero > Integer.MAX_VALUE
                            || (tipo == Types.SMALLINT && (entero < Short.MIN_VALUE || entero > Short.MAX_VALUE))) {
                        throw new SQLException("Entero fuera del rango del contrato Java/PostgreSQL.", "22003");
                    }
                    escritura.setInt(columna, (int) entero);
                }
                break;
            case Types.NUMERIC:
                BigDecimal decimal = datos.getBigDecimal(columna);
                if (decimal == null) { escritura.setNull(columna, tipo); }
                else { escritura.setBigDecimal(columna, decimal); }
                break;
            case Types.TIMESTAMP:
                Timestamp fecha = datos.getTimestamp(columna);
                if (fecha == null) { escritura.setNull(columna, tipo); }
                else { escritura.setTimestamp(columna, fecha); }
                break;
            case Types.VARCHAR:
                String texto = datos.getString(columna);
                if (texto == null) { escritura.setNull(columna, tipo); }
                else { escritura.setString(columna, texto); }
                break;
            default: throw new SQLException("Tipo no contemplado en el contrato de respaldo.", "22000");
        }
    }

    private static long contar(Connection conexion, EsquemaRespaldo.Tabla tabla) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement("SELECT COUNT(*) FROM " + tabla.nombre);
             ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next()) { throw new SQLException("No se pudo contar " + tabla.nombre + ".", "22000"); }
            return resultado.getLong(1);
        }
    }

    private static long siguienteId(Connection conexion, EsquemaRespaldo.Tabla tabla,
            boolean oracle) throws SQLException {
        String funcion = oracle ? "NVL" : "COALESCE";
        try (PreparedStatement consulta = conexion.prepareStatement("SELECT " + funcion + "(MAX(id), 0) FROM " + tabla.nombre);
             ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next()) { throw new SQLException("No se pudo calcular el siguiente id.", "22000"); }
            long maximo = resultado.getLong(1);
            if (maximo < 0 || maximo >= Integer.MAX_VALUE) {
                throw new SQLException("Identificadores agotados o incompatibles en " + tabla.nombre + ".", "22003");
            }
            return maximo + 1;
        }
    }

    private static void sincronizarSecuenciasPostgres(Connection postgres) throws SQLException {
        for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
            if (tabla.secuencia != null) {
                long siguiente = siguienteId(postgres, tabla, false);
                // RESTART es transaccional. setval no lo es y rompería el rollback completo.
                ejecutar(postgres, "ALTER SEQUENCE " + tabla.secuencia + " RESTART WITH " + siguiente);
            }
        }
    }

    static String generarAjusteOracle() {
        StringBuilder sql = new StringBuilder("-- Generado por BackupEnCaliente. Ejecutar SOLO después de recuperación exitosa.\n"
                + "-- Mantener detenida la aplicación hasta terminar estos 14 ajustes.\n"
                + "-- ALTER SEQUENCE confirma implícitamente: este archivo va separado de los INSERT.\n"
                + "-- Conserva secuencias y permisos; avanza NEXTVAL y puede dejar huecos de IDs.\n"
                + "WHENEVER SQLERROR EXIT SQL.SQLCODE\nSET DEFINE OFF\n");
        for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
            if (tabla.secuencia == null) { continue; }
            sql.append("\nDECLARE\n  v_maximo NUMBER;\n  v_actual NUMBER;\nBEGIN\n")
               .append("  SELECT NVL(MAX(id), 0) INTO v_maximo FROM ").append(tabla.nombre).append(";\n")
               .append("  SELECT ").append(tabla.secuencia).append(".NEXTVAL INTO v_actual FROM dual;\n")
               .append("  IF v_actual < v_maximo THEN\n")
               .append("    EXECUTE IMMEDIATE 'ALTER SEQUENCE ").append(tabla.secuencia)
               .append(" INCREMENT BY ' || TO_CHAR(v_maximo - v_actual, 'FM9999999999999999999999999999');\n")
               .append("    BEGIN\n      SELECT ").append(tabla.secuencia).append(".NEXTVAL INTO v_actual FROM dual;\n")
               .append("    EXCEPTION WHEN OTHERS THEN\n      EXECUTE IMMEDIATE 'ALTER SEQUENCE ")
               .append(tabla.secuencia).append(" INCREMENT BY 1';\n      RAISE;\n    END;\n")
               .append("    EXECUTE IMMEDIATE 'ALTER SEQUENCE ").append(tabla.secuencia).append(" INCREMENT BY 1';\n")
               .append("  END IF;\nEND;\n/\n");
        }
        sql.append("\nPROMPT Secuencias ajustadas. Verificar conteos e integridad antes de abrir la aplicación.\n");
        return sql.toString();
    }

    private static void ejecutar(Connection conexion, String sql) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) { sentencia.execute(); }
    }

    private static void deshacer(Connection conexion, Throwable original) {
        try { if (!conexion.getAutoCommit()) { conexion.rollback(); } }
        catch (SQLException fallo) { original.addSuppressed(fallo); }
    }

    private static void confirmar(Connection conexion) throws SQLException {
        try { conexion.commit(); }
        catch (SQLException ex) {
            // Una caída de red no permite saber si el servidor alcanzó a confirmar.
            throw new SQLException("Resultado de commit desconocido; revisar el destino antes de reintentar.", "08007", ex);
        }
    }

    public static void main(String[] argumentos) {
        if (argumentos.length == 0 || argumentos.length > 2
                || (!"respaldar".equals(argumentos[0]) && !"recuperar".equals(argumentos[0]))
                || ("respaldar".equals(argumentos[0]) && argumentos.length != 1)) {
            System.err.println("Uso: BackupEnCaliente respaldar | recuperar [archivo-secuencias.sql]");
            System.exit(2);
        }
        try {
            boolean restauracion = "recuperar".equals(argumentos[0]);
            Path ajuste = Paths.get(argumentos.length == 2 ? argumentos[1] : "backups/recuperacion_secuencias.sql");
            Map<String, Long> conteos = restauracion ? recuperar(ajuste) : respaldar();
            for (Map.Entry<String, Long> tabla : conteos.entrySet()) {
                System.out.println(tabla.getKey() + ": " + tabla.getValue() + " filas verificadas.");
            }
            if (restauracion) {
                System.out.println("Datos recuperados y confirmados en Oracle. Con la aplicación detenida, ejecutar el ajuste: "
                        + ajuste.toAbsolutePath());
            } else {
                System.out.println("Respaldo consistente confirmado en PostgreSQL (15 tablas, 14 secuencias).");
            }
        } catch (SQLException ex) {
            if ("08007".equals(ex.getSQLState())) {
                System.err.println("Se perdió la confirmación del commit (SQLState=08007). Revisar los datos destino antes de reintentar. "
                        + "En recuperación se conserva el archivo SQL de ajuste; ejecutarlo solo si se verificó la copia completa.");
            } else {
                System.err.println("Operación fallida. SQLState=" + ex.getSQLState() + "; código=" + ex.getErrorCode()
                        + ". Los datos no confirmados se deshicieron. Revisar scripts/README.md.");
            }
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("No se pudo crear el archivo de ajuste (no debe existir previamente). Recuperación deshecha.");
            System.exit(1);
        } catch (RuntimeException ex) {
            System.err.println("Operación fallida por un error interno. Los datos no confirmados se deshicieron.");
            System.exit(1);
        }
    }
}
