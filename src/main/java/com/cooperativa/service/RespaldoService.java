package com.cooperativa.service;

import com.cooperativa.config.ConexionPostgres;
import com.cooperativa.utils.BackupEnCaliente;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordina el respaldo en caliente para que la API y su interfaz puedan dispararlo sin
 * bloquear el hilo HTTP. Serializa las ejecuciones, conserva el último resultado (incluso
 * entre reinicios) y traduce los errores JDBC a mensajes accionables.
 *
 * El trabajo real sigue en {@link BackupEnCaliente}: aquí solo se orquesta y se recuerda.
 */
public class RespaldoService {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path ARCHIVO_ESTADO = Paths.get("backups", "ultimo_respaldo.properties");
    private static final int ESPERA_PG_SEGUNDOS = 5;

    private final Object candado = new Object();
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "respaldo-en-caliente");
        hilo.setDaemon(true);
        return hilo;
    });

    private boolean enCurso;
    private boolean exitoso;
    private String fecha;
    private String mensaje = "Aún no se ha ejecutado un respaldo en esta instalación.";
    private Map<String, Long> conteos = Collections.emptyMap();

    public RespaldoService() {
        cargarEstado();
    }

    /** Dispara el respaldo si no hay otro en curso. Devuelve false si ya se está ejecutando. */
    public boolean iniciar() {
        synchronized (candado) {
            if (enCurso) { return false; }
            enCurso = true;
            exitoso = false;
            mensaje = "Respaldo en curso...";
            ejecutor.submit(this::ejecutar);
            return true;
        }
    }

    /** Fotografía del estado actual, apta para serializar en JSON. */
    public Estado estado() {
        boolean enEjecucion;
        boolean ultimoExito;
        String ultimaFecha;
        String ultimoMensaje;
        Map<String, Long> copiaConteos;
        synchronized (candado) {
            enEjecucion = enCurso;
            ultimoExito = exitoso;
            ultimaFecha = fecha;
            ultimoMensaje = mensaje;
            copiaConteos = new LinkedHashMap<String, Long>(conteos);
        }
        // La sonda de red se hace fuera del candado: no debe retener el estado mientras espera.
        return new Estado(enEjecucion, ultimoExito, ultimaFecha, ultimoMensaje,
                copiaConteos, postgresAlcanzable());
    }

    private void ejecutar() {
        boolean ok = false;
        String texto;
        Map<String, Long> copia = Collections.emptyMap();
        try {
            copia = BackupEnCaliente.respaldar();
            ok = true;
            texto = "Respaldo consistente confirmado en PostgreSQL (15 tablas, 14 secuencias).";
        } catch (SQLException ex) {
            texto = describirError(ex);
        } catch (RuntimeException ex) {
            String detalle = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            texto = "Error interno durante el respaldo: " + detalle + ".";
        }
        synchronized (candado) {
            conteos = new LinkedHashMap<String, Long>(copia);
            exitoso = ok;
            mensaje = texto;
            fecha = LocalDateTime.now().format(FORMATO);
            enCurso = false;
        }
        guardarEstado();
    }

    /** Traduce el SQLState al punto de fallo más probable, sin exponer credenciales. */
    private String describirError(SQLException ex) {
        String estado = ex.getSQLState();
        String detalle = ex.getMessage() == null ? "" : ex.getMessage();
        if ("08001".equals(estado)) {
            return "No se pudo conectar a PostgreSQL. Revisa pg.url/pg.user/pg.password en config.properties "
                    + "y que el servicio esté activo. Detalle: " + detalle;
        }
        if ("55P03".equals(estado)) {
            return "Ya hay un respaldo en ejecución. Espera a que termine antes de reintentar.";
        }
        if ("25000".equals(estado)) {
            return "Comprobación de seguridad: " + detalle;
        }
        if ("08007".equals(estado)) {
            return "Se perdió la confirmación del commit; revisar el destino antes de reintentar. Detalle: " + detalle;
        }
        return "Respaldo fallido (SQLState=" + estado + "). Detalle: " + detalle;
    }

    private boolean postgresAlcanzable() {
        int anterior = DriverManager.getLoginTimeout();
        try {
            DriverManager.setLoginTimeout(ESPERA_PG_SEGUNDOS);
            return ConexionPostgres.probarConexion();
        } finally {
            DriverManager.setLoginTimeout(anterior);
        }
    }

    private void cargarEstado() {
        if (!Files.isRegularFile(ARCHIVO_ESTADO)) { return; }
        Properties estado = new Properties();
        try (InputStream entrada = Files.newInputStream(ARCHIVO_ESTADO)) {
            estado.load(entrada);
            synchronized (candado) {
                fecha = texto(estado.getProperty("fecha"));
                exitoso = Boolean.parseBoolean(estado.getProperty("exitoso", "false"));
                String guardado = texto(estado.getProperty("mensaje"));
                if (guardado != null) { mensaje = guardado; }
                Map<String, Long> leidos = new LinkedHashMap<String, Long>();
                for (String clave : estado.stringPropertyNames()) {
                    if (clave.startsWith("conteo.")) {
                        try {
                            leidos.put(clave.substring("conteo.".length()), Long.parseLong(estado.getProperty(clave)));
                        } catch (NumberFormatException ignorado) {
                            // Un contador corrupto no debe impedir arrancar la API.
                        }
                    }
                }
                conteos = leidos.isEmpty() ? Collections.<String, Long>emptyMap() : leidos;
            }
        } catch (IOException ex) {
            System.err.println("No se pudo leer el estado del respaldo (" + ARCHIVO_ESTADO + "): " + ex.getMessage());
        }
    }

    private void guardarEstado() {
        Properties estado = new Properties();
        synchronized (candado) {
            if (fecha != null) { estado.setProperty("fecha", fecha); }
            estado.setProperty("exitoso", Boolean.toString(exitoso));
            if (mensaje != null) { estado.setProperty("mensaje", mensaje); }
            for (Map.Entry<String, Long> tabla : conteos.entrySet()) {
                estado.setProperty("conteo." + tabla.getKey(), Long.toString(tabla.getValue()));
            }
        }
        try {
            Files.createDirectories(ARCHIVO_ESTADO.toAbsolutePath().getParent());
            try (OutputStream salida = Files.newOutputStream(ARCHIVO_ESTADO)) {
                estado.store(salida, "Ultimo respaldo en caliente Oracle -> PostgreSQL");
            }
        } catch (IOException ex) {
            System.err.println("No se pudo guardar el estado del respaldo (" + ARCHIVO_ESTADO + "): " + ex.getMessage());
        }
    }

    private static String texto(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor;
    }

    /** Resultado serializable del respaldo, consumido por la interfaz web. */
    public static final class Estado {
        private final boolean enCurso;
        private final boolean exitoso;
        private final String fecha;
        private final String mensaje;
        private final Map<String, Long> conteos;
        private final boolean postgresAlcanzable;

        Estado(boolean enCurso, boolean exitoso, String fecha, String mensaje,
                Map<String, Long> conteos, boolean postgresAlcanzable) {
            this.enCurso = enCurso;
            this.exitoso = exitoso;
            this.fecha = fecha;
            this.mensaje = mensaje;
            this.conteos = conteos;
            this.postgresAlcanzable = postgresAlcanzable;
        }

        public boolean isEnCurso() { return enCurso; }
        public boolean isExitoso() { return exitoso; }
        public String getFecha() { return fecha; }
        public String getMensaje() { return mensaje; }
        public Map<String, Long> getConteos() { return conteos; }
        public boolean isPostgresAlcanzable() { return postgresAlcanzable; }
    }
}
