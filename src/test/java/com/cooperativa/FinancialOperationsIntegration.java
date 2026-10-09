package com.cooperativa;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.controller.AlbaranController;
import com.cooperativa.controller.CuentaController;
import com.cooperativa.controller.NegocioException;
import com.cooperativa.controller.PrestamoController;
import com.cooperativa.dao.AlbaranDAO;
import com.cooperativa.dao.PrestamoDAO;
import com.cooperativa.model.Albaran;
import com.cooperativa.model.DetalleAlbaran;
import com.cooperativa.model.Prestamo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Integracion real Java 8 / Oracle, sin JUnit ni dependencias nuevas.
 *
 * Ejecutar SOLO con config.properties de un esquema desechable TF_QA_* que
 * contenga el DDL actualizado y las 38 filas de 02_initial_data.sql. El usuario
 * conectado y CURRENT_SCHEMA deben coincidir. Todas las guardas preceden las
 * escrituras. La suite deja sus fixtures para inspeccion; quien cree el esquema
 * QA es responsable de eliminarlo despues. No ejecutar sobre un esquema real.
 *
 * Las fixtures SQL tienen IDs >= 10000. Los saldos y los registros creados por
 * controllers utilizan sus secuencias normales. El fallo de rollback utiliza
 * una restriccion CHECK temporal, limitada al albaran QA 12010, retirada en finally.
 */
public final class FinancialOperationsIntegration {
    private static final BigDecimal CERO = new BigDecimal("0.00");
    private static final String RESTRICCION_QA = "QA_LIQ_ROLLBACK_12010";
    private static final AlbaranController ALBARANES = new AlbaranController();
    private static final PrestamoController PRESTAMOS = new PrestamoController();
    private static final CuentaController CUENTAS = new CuentaController();
    private static int comprobaciones;
    private static int pruebas;
    private static String pruebaActual = "guardas de aislamiento";
    private static boolean esquemaValidado;

    private FinancialOperationsIntegration() { }

    private interface Accion { void ejecutar() throws Exception; }
    private interface Operacion { BigDecimal ejecutar() throws Exception; }

    public static void main(String[] args) {
        int salida = 0;
        try {
            validarEsquemaInicial();
            prepararFixtures();
            probar("abono parcial, pago completo y repeticion", () -> probarAbonos());
            probar("creacion y abonos invalidos sin escrituras", () -> probarMontosInvalidos());
            probar("restricciones de saldos negativos", () -> probarRestricciones());
            probar("abonos concurrentes parciales", () -> probarAbonosConcurrentes());
            probar("pago completo concurrente unico", () -> probarPagoCompletoConcurrente());
            probar("primera liquidacion y repeticion", () -> probarLiquidacion());
            probar("doble liquidacion concurrente", () -> probarLiquidacionConcurrente());
            probar("dos entregas concurrentes a una cuenta", () -> probarEntregasDistintas());
            probar("pertenencia, estado y referencias inexistentes", () -> probarCuentasInvalidas());
            probar("socio asociado no titular y entrega sin aceptados", () -> probarAsociadoYCero());
            probar("liquidado enviado por cliente se ignora al crear", () -> probarBanderaDelCliente());
            probar("fallo posterior al saldo revierte toda la liquidacion", () -> probarRollback());
            probar("datos originales de demostracion intactos", () -> verificarSemillaIntacta());
            System.out.println("RESULTADO: " + pruebas + " pruebas; " + comprobaciones + " comprobaciones correctas.");
        } catch (Throwable error) {
            salida = 1;
            // No imprimir valores de registros, mensajes JDBC ni configuracion.
            System.err.println("FALLO: " + pruebaActual + " [" + error.getClass().getSimpleName() + "]");
            if (error instanceof AssertionError) {
                System.err.println("COMPROBACION: " + error.getMessage());
            }
        } finally {
            try { ConexionOracle.cerrarTodas(); } catch (Throwable ignorado) { }
        }
        if (salida != 0) System.exit(salida);
    }

    private static void probar(String nombre, Accion accion) throws Exception {
        pruebaActual = nombre;
        accion.ejecutar();
        pruebas++;
        System.out.println("OK: " + nombre);
    }

    private static void validarEsquemaInicial() throws Exception {
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement("SELECT USER, SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM dual");
             ResultSet rs = ps.executeQuery()) {
            comprobar(rs.next(), "usuario de conexion disponible");
            String usuario = rs.getString(1).toUpperCase(Locale.ROOT);
            String esquema = rs.getString(2).toUpperCase(Locale.ROOT);
            comprobar(usuario.startsWith("TF_QA_"), "bloquear usuarios ajenos a TF_QA_");
            comprobar(usuario.equals(esquema), "bloquear CURRENT_SCHEMA ajeno al usuario QA");
        }
        String[] tablas = {"sedes", "estatus", "tipos_persona", "tipos_cuenta", "tipos_prestamo",
            "categorias_producto", "personas", "productos", "cuentas", "cuentas_personas", "saldos",
            "prestamos", "detalles_prestamo", "albaranes", "detalles_albaran"};
        int[] esperadas = {2, 4, 3, 2, 3, 2, 3, 4, 2, 3, 3, 2, 2, 1, 2};
        int total = 0;
        for (int i = 0; i < tablas.length; i++) {
            comprobar(entero("SELECT COUNT(*) FROM user_tables WHERE table_name = ?", tablas[i].toUpperCase(Locale.ROOT)) == 1,
                "tabla propia de QA, no sinonimo a otro esquema: " + tablas[i]);
            // Identificadores constantes de esta suite; nunca datos de entrada.
            int filas = entero("SELECT COUNT(*) FROM " + tablas[i]);
            comprobar(filas == esperadas[i], "conteo inicial exacto en " + tablas[i]);
            total += filas;
        }
        comprobar(total == 38, "solo las 38 filas de demostracion iniciales");
        for (String secuencia : new String[] {"SEQ_SALDOS", "SEQ_ALBARANES", "SEQ_DETALLES_ALBARAN", "SEQ_PRESTAMOS"}) {
            comprobar(entero("SELECT COUNT(*) FROM user_sequences WHERE sequence_name = ?", secuencia) == 1,
                "secuencia propia del esquema QA");
        }
        comprobar(entero("SELECT COUNT(*) FROM personas WHERE id = 1 AND dni = 'DEMO-SOCIO-001' AND tipo_persona_id = 2 AND estatus_id = 1 AND sede_id = 1") == 1,
            "persona semilla requerida");
        comprobar(entero("SELECT COUNT(*) FROM estatus WHERE (id = 1 AND nombre = 'ACTIVO') OR (id = 2 AND nombre = 'INACTIVO') OR (id = 3 AND nombre = 'PAGADO')") == 3,
            "estatus semilla requeridos");
        verificarSemillaIntacta();
        esquemaValidado = true;
        System.out.println("OK: esquema QA aislado y semilla verificada antes de escribir.");
    }

    private static void prepararFixtures() throws Exception {
        comprobar(esquemaValidado, "guardas completadas antes de las fixtures");
        try (Connection conn = ConexionOracle.getConexion()) {
            conn.setAutoCommit(false);
            try {
                cuenta(conn, 10001, 1, 1, "100.00");
                cuenta(conn, 10002, 1, 1, "100.00");
                cuenta(conn, 10003, 1, 1, "20.00");
                cuenta(conn, 10004, 2, 1, "40.00");
                cuenta(conn, 10005, 1, 2, "50.00");
                cuenta(conn, 10006, 2, 1, "60.00");
                escribir(conn, "INSERT INTO cuentas_personas (cuenta_id, persona_id, titular) VALUES (?, ?, ?)", 10006, 1, 0);
                cuenta(conn, 10007, 1, 1, "70.00");
                cuenta(conn, 10008, 1, 1, "50.00");
                prestamo(conn, 11001, "100.00");
                prestamo(conn, 11002, "100.00");
                prestamo(conn, 11003, "100.00");
                prestamo(conn, 11004, "80.00");
                albaran(conn, 12001, "2.00", "3.00", "10.00");
                albaran(conn, 12002, "1.00", "0.00", "30.00");
                albaran(conn, 12003, "1.00", "0.00", "17.25");
                albaran(conn, 12004, "1.00", "0.00", "32.75");
                albaran(conn, 12005, "1.00", "0.00", "10.00");
                albaran(conn, 12006, "1.00", "0.00", "10.00");
                albaran(conn, 12007, "1.00", "0.00", "10.00");
                albaran(conn, 12008, "1.00", "0.00", "15.00");
                albaran(conn, 12009, "0.00", "2.00", "10.00");
                albaran(conn, 12010, "1.00", "0.00", "18.00");
                conn.commit();
            } catch (Exception error) { conn.rollback(); throw error; }
        }
    }

    private static void cuenta(Connection conn, int id, int persona, int estatus, String saldo) throws Exception {
        escribir(conn, "INSERT INTO cuentas (id, numero, tipo_cuenta_id, estatus_id, sede_id, fecha_apertura) VALUES (?, ?, 1, ?, 1, SYSDATE)", id, "QA-CTA-" + id, estatus);
        escribir(conn, "INSERT INTO cuentas_personas (cuenta_id, persona_id, titular) VALUES (?, ?, 1)", id, persona);
        escribir(conn, "INSERT INTO saldos (id, cuenta_id, monto, fecha) VALUES (seq_saldos.NEXTVAL, ?, ?, SYSDATE)", id, decimal(saldo));
    }

    private static void prestamo(Connection conn, int id, String monto) throws Exception {
        escribir(conn, "INSERT INTO prestamos (id, persona_id, tipo_prestamo_id, estatus_id, monto, tasa_interes, fecha) VALUES (?, 1, 1, 1, ?, 0, SYSDATE)", id, decimal(monto));
    }

    private static void albaran(Connection conn, int id, String aceptada, String rechazada, String precio) throws Exception {
        escribir(conn, "INSERT INTO albaranes (id, numero_talonario, persona_id, sede_id, fecha, liquidado) VALUES (?, ?, 1, 1, SYSDATE, 0)", id, "QA-TAL-" + id);
        escribir(conn, "INSERT INTO detalles_albaran (id, albaran_id, producto_id, cantidad_aceptada, cantidad_rechazada, precio_unitario) VALUES (?, ?, 3, ?, ?, ?)",
            id + 1000, id, decimal(aceptada), decimal(rechazada), decimal(precio));
    }

    private static void probarAbonos() throws Exception {
        igual("74.75", PRESTAMOS.registrarAbono(11001, decimal("25.25")), "retorno del abono parcial");
        verificarPrestamo(11001, "74.75", 1);
        igual("0.00", PRESTAMOS.registrarAbono(11001, decimal("74.75")), "retorno del pago completo");
        verificarPrestamo(11001, "0.00", 3);
        negocio(() -> PRESTAMOS.registrarAbono(11001, decimal("0.01")), "un prestamo pagado no admite otro abono");
        verificarPrestamo(11001, "0.00", 3);
    }

    private static void probarMontosInvalidos() throws Exception {
        int antes = entero("SELECT COUNT(*) FROM prestamos");
        String[] invalidos = {"0", "-1", "0.001"};
        for (String monto : invalidos) {
            Prestamo p = new Prestamo();
            p.setPersonaId(1); p.setTipoPrestamoId(1); p.setMonto(decimal(monto)); p.setTasaInteres(CERO);
            negocio(() -> PRESTAMOS.registrarPrestamo(p), "rechazo de monto inicial no positivo despues del redondeo");
        }
        comprobar(entero("SELECT COUNT(*) FROM prestamos") == antes, "rechazos no insertan prestamos");
        for (String monto : new String[] {"0", "-1", "0.001", "81.00"}) {
            negocio(() -> PRESTAMOS.registrarAbono(11004, decimal(monto)), "rechazo de abono invalido");
        }
        negocio(() -> PRESTAMOS.registrarAbono(11004, null), "rechazo de abono nulo");
        negocio(() -> PRESTAMOS.registrarAbono(999999, decimal("1.00")), "prestamo inexistente");
        verificarPrestamo(11004, "80.00", 1);
    }

    private static void probarRestricciones() throws Exception {
        rechazoCheck("UPDATE prestamos SET monto = -0.01 WHERE id = 11004", "Oracle rechaza deuda negativa");
        rechazoCheck("UPDATE saldos SET monto = -0.01 WHERE cuenta_id = 10007", "Oracle rechaza saldo de cuenta negativo");
        verificarPrestamo(11004, "80.00", 1);
        verificarCuenta(10007, "70.00", 1);
    }

    private static void probarAbonosConcurrentes() throws Exception {
        Resultado[] r = concurrentes("SELECT id FROM prestamos WHERE id = ? FOR UPDATE", 11002,
            () -> new PrestamoController().registrarAbono(11002, decimal("30.00")),
            () -> new PrestamoController().registrarAbono(11002, decimal("45.00")));
        comprobar(exitos(r) == 2, "los dos abonos parciales se confirman");
        verificarPrestamo(11002, "25.00", 1);
        comprobar(r[0].valor.compareTo(decimal("25.00")) == 0 || r[1].valor.compareTo(decimal("25.00")) == 0,
            "una respuesta incluye ambos abonos confirmados");
    }

    private static void probarPagoCompletoConcurrente() throws Exception {
        Resultado[] r = concurrentes("SELECT id FROM prestamos WHERE id = ? FOR UPDATE", 11003,
            () -> new PrestamoController().registrarAbono(11003, decimal("100.00")),
            () -> new PrestamoController().registrarAbono(11003, decimal("100.00")));
        comprobar(exitos(r) == 1 && rechazos(r) == 1, "solo un pago total concurrente se acepta");
        verificarPrestamo(11003, "0.00", 3);
    }

    private static void probarLiquidacion() throws Exception {
        igual("20.00", ALBARANES.liquidarAlbaranEnCuenta(12001, 10001), "solo se paga el producto aceptado");
        verificarCuenta(10001, "120.00", 2); verificarLiquidado(12001, true);
        negocio(() -> ALBARANES.liquidarAlbaranEnCuenta(12001, 10001), "liquidacion repetida rechazada");
        verificarCuenta(10001, "120.00", 2); verificarLiquidado(12001, true);
    }

    private static void probarLiquidacionConcurrente() throws Exception {
        Resultado[] r = concurrentes("SELECT id FROM albaranes WHERE id = ? FOR UPDATE", 12002,
            () -> new AlbaranController().liquidarAlbaranEnCuenta(12002, 10002),
            () -> new AlbaranController().liquidarAlbaranEnCuenta(12002, 10002));
        comprobar(exitos(r) == 1 && rechazos(r) == 1, "solo una liquidacion concurrente se acepta");
        verificarCuenta(10002, "130.00", 2); verificarLiquidado(12002, true);
    }

    private static void probarEntregasDistintas() throws Exception {
        Resultado[] r = concurrentes("SELECT id FROM cuentas WHERE id = ? FOR UPDATE", 10003,
            () -> new AlbaranController().liquidarAlbaranEnCuenta(12003, 10003),
            () -> new AlbaranController().liquidarAlbaranEnCuenta(12004, 10003));
        comprobar(exitos(r) == 2, "dos albaranes distintos se confirman");
        verificarCuenta(10003, "70.00", 3);
        verificarLiquidado(12003, true); verificarLiquidado(12004, true);
    }

    private static void probarCuentasInvalidas() throws Exception {
        negocio(() -> ALBARANES.liquidarAlbaranEnCuenta(12005, 10004), "cuenta de otra persona rechazada");
        verificarCuenta(10004, "40.00", 1); verificarLiquidado(12005, false);
        negocio(() -> ALBARANES.liquidarAlbaranEnCuenta(12006, 10005), "cuenta inactiva rechazada");
        verificarCuenta(10005, "50.00", 1); verificarLiquidado(12006, false);
        int saldosAntes = entero("SELECT COUNT(*) FROM saldos");
        negocio(() -> ALBARANES.liquidarAlbaranEnCuenta(12007, 999999), "cuenta inexistente rechazada");
        comprobar(entero("SELECT COUNT(*) FROM saldos") == saldosAntes, "cuenta inexistente no genera historial");
        verificarLiquidado(12007, false);
        negocio(() -> ALBARANES.liquidarAlbaranEnCuenta(999999, 10004), "albaran inexistente rechazado");
        verificarCuenta(10004, "40.00", 1);
    }

    private static void probarAsociadoYCero() throws Exception {
        igual("15.00", ALBARANES.liquidarAlbaranEnCuenta(12008, 10006), "socio asociado aunque no titular");
        verificarCuenta(10006, "75.00", 2); verificarLiquidado(12008, true);
        negocio(() -> ALBARANES.liquidarAlbaranEnCuenta(12009, 10007), "entrega con cero aceptado no se paga");
        verificarCuenta(10007, "70.00", 1); verificarLiquidado(12009, false);
    }

    private static void probarBanderaDelCliente() throws Exception {
        int saldosAntes = entero("SELECT COUNT(*) FROM saldos");
        Albaran a = new Albaran();
        a.setNumeroTalonario("QA-CLIENTE-LIQUIDADO"); a.setPersonaId(1); a.setSedeId(1); a.setLiquidado(true);
        DetalleAlbaran d = new DetalleAlbaran();
        d.setProductoId(3); d.setCantidadAceptada(decimal("1.00")); d.setCantidadRechazada(CERO); d.setPrecioUnitario(decimal("12.34"));
        a.agregarDetalle(d);
        Albaran registrado = ALBARANES.registrarEntrega(a);
        comprobar(!registrado.isLiquidado(), "controller devuelve el estado inicial no liquidado");
        verificarLiquidado(registrado.getId(), false);
        comprobar(entero("SELECT COUNT(*) FROM saldos") == saldosAntes, "crear albaran no acredita una cuenta");
    }

    private static void probarRollback() throws Exception {
        comprobar(esquemaValidado, "DDL de fallo limitado al esquema QA validado");
        boolean creada = false;
        try {
            ddl("ALTER TABLE albaranes ADD CONSTRAINT " + RESTRICCION_QA + " CHECK (id <> 12010 OR liquidado = 0)");
            creada = true;
            BigDecimal secuenciaAntes = escalar("SELECT seq_saldos.NEXTVAL FROM dual");
            negocio(() -> ALBARANES.liquidarAlbaranEnCuenta(12010, 10008), "fallo de la marca de liquidacion traducido a negocio");
            BigDecimal secuenciaDespues = escalar("SELECT seq_saldos.NEXTVAL FROM dual");
            comprobar(secuenciaDespues.subtract(secuenciaAntes).compareTo(decimal("2")) >= 0,
                "se intento insertar el saldo antes del fallo de la marca");
            verificarCuenta(10008, "50.00", 1); verificarLiquidado(12010, false);
        } finally {
            if (creada) ddl("ALTER TABLE albaranes DROP CONSTRAINT " + RESTRICCION_QA);
        }
        igual("18.00", ALBARANES.liquidarAlbaranEnCuenta(12010, 10008), "reintento correcto despues del rollback");
        verificarCuenta(10008, "68.00", 2); verificarLiquidado(12010, true);
    }

    private static void verificarSemillaIntacta() throws Exception {
        igual("192.00", escalar("SELECT monto FROM prestamos WHERE id = 1"), "prestamo semilla uno intacto");
        igual("1500.00", escalar("SELECT monto FROM prestamos WHERE id = 2"), "prestamo semilla dos intacto");
        comprobar(entero("SELECT COUNT(*) FROM prestamos WHERE id IN (1, 2) AND estatus_id = 1") == 2, "estatus de prestamos semilla intactos");
        comprobar(entero("SELECT COUNT(*) FROM saldos WHERE cuenta_id IN (1, 2)") == 3, "historial semilla intacto");
        igual("1250.00", CUENTAS.consultarSaldo(1), "ahorro semilla intacto");
        igual("500.00", CUENTAS.consultarSaldo(2), "cuenta monetaria semilla intacta");
        comprobar(entero("SELECT COUNT(*) FROM productos WHERE (id = 1 AND stock = 96) OR (id = 2 AND stock = 78) OR (id IN (3, 4) AND stock = 0)") == 4,
            "stock semilla intacto");
        verificarLiquidado(1, false);
    }

    private static void verificarPrestamo(int id, String monto, int estatus) throws Exception {
        Prestamo p = new PrestamoDAO().obtenerPorId(id);
        comprobar(p != null, "prestamo consultable despues de operar");
        igual(monto, p.getMonto(), "deuda persistida exacta");
        comprobar(p.getEstatusId() == estatus, "estatus persistido coherente con deuda");
    }

    private static void verificarCuenta(int cuenta, String monto, int registros) throws Exception {
        igual(monto, CUENTAS.consultarSaldo(cuenta), "saldo vigente exacto");
        comprobar(entero("SELECT COUNT(*) FROM saldos WHERE cuenta_id = ?", cuenta) == registros, "cantidad exacta de movimientos de saldo");
    }

    private static void verificarLiquidado(int id, boolean esperado) throws Exception {
        Albaran a = new AlbaranDAO().obtenerPorId(id);
        comprobar(a != null && a.isLiquidado() == esperado, "bandera liquidado persistida y mapeada");
        comprobar(entero("SELECT liquidado FROM albaranes WHERE id = ?", id) == (esperado ? 1 : 0), "bandera liquidado en Oracle");
    }

    private static void negocio(Accion accion, String mensaje) throws Exception {
        boolean rechazado = false;
        try { accion.ejecutar(); } catch (NegocioException esperado) { rechazado = true; }
        comprobar(rechazado, mensaje);
    }

    private static void rechazoCheck(String sql, String mensaje) throws Exception {
        boolean rechazado = false;
        try (Connection conn = ConexionOracle.getConexion()) {
            conn.setAutoCommit(false);
            try { escribir(conn, sql); }
            catch (SQLException esperado) { rechazado = esperado.getErrorCode() == 2290; }
            finally { conn.rollback(); }
        }
        comprobar(rechazado, mensaje);
    }

    private static final class Resultado {
        final BigDecimal valor;
        final boolean negocio;
        Resultado(BigDecimal valor, boolean negocio) { this.valor = valor; this.negocio = negocio; }
    }

    private static Resultado[] concurrentes(String bloqueo, int id, Operacion primera, Operacion segunda) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch listas = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch invocadas = new CountDownLatch(2);
        try (Connection cerrojo = ConexionOracle.getConexion()) {
            cerrojo.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cerrojo.prepareStatement(bloqueo)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) { comprobar(rs.next(), "fixture disponible para prueba concurrente"); }
                }
                Future<Resultado> a = executor.submit(tarea(primera, listas, salida, invocadas));
                Future<Resultado> b = executor.submit(tarea(segunda, listas, salida, invocadas));
                comprobar(listas.await(5, TimeUnit.SECONDS), "dos trabajadores preparados");
                salida.countDown();
                comprobar(invocadas.await(5, TimeUnit.SECONDS), "dos operaciones lanzadas antes de soltar el cerrojo");
                // El cerrojo fuerza solapamiento incluso en una maquina rapida.
                // Las aserciones de saldo/historial comprueban el resultado real.
                Thread.sleep(350);
                comprobar(!a.isDone() && !b.isDone(), "ambas operaciones esperan la fila retenida");
                cerrojo.rollback();
                return new Resultado[] {a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)};
            } finally { cerrojo.rollback(); }
        } finally {
            salida.countDown();
            executor.shutdownNow();
            comprobar(executor.awaitTermination(10, TimeUnit.SECONDS), "trabajadores concurrentes terminados");
        }
    }

    private static Callable<Resultado> tarea(final Operacion operacion, final CountDownLatch listas,
            final CountDownLatch salida, final CountDownLatch invocadas) {
        return () -> {
            listas.countDown();
            if (!salida.await(5, TimeUnit.SECONDS)) throw new AssertionError("inicio concurrente fuera de plazo");
            invocadas.countDown();
            try { return new Resultado(operacion.ejecutar(), false); }
            catch (NegocioException esperado) { return new Resultado(null, true); }
        };
    }

    private static int exitos(Resultado[] resultados) {
        int total = 0;
        for (Resultado r : resultados) if (!r.negocio) total++;
        return total;
    }

    private static int rechazos(Resultado[] resultados) { return resultados.length - exitos(resultados); }

    private static void ddl(String sql) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion(); Statement st = conn.createStatement()) { st.executeUpdate(sql); }
    }

    private static void escribir(Connection conn, String sql, Object... valores) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            parametros(ps, valores);
            int filas = ps.executeUpdate();
            comprobar(filas == 1, "escritura de una sola fixture");
        }
    }

    private static BigDecimal escalar(String sql, Object... valores) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            parametros(ps, valores);
            try (ResultSet rs = ps.executeQuery()) {
                comprobar(rs.next(), "consulta de verificacion con resultado");
                return rs.getBigDecimal(1);
            }
        }
    }

    private static int entero(String sql, Object... valores) throws SQLException { return escalar(sql, valores).intValueExact(); }

    private static void parametros(PreparedStatement ps, Object[] valores) throws SQLException {
        for (int i = 0; i < valores.length; i++) {
            Object valor = valores[i];
            if (valor instanceof BigDecimal) ps.setBigDecimal(i + 1, (BigDecimal) valor);
            else if (valor instanceof Integer) ps.setInt(i + 1, (Integer) valor);
            else ps.setString(i + 1, (String) valor);
        }
    }

    private static BigDecimal decimal(String valor) { return new BigDecimal(valor); }

    private static void igual(String esperado, BigDecimal actual, String mensaje) {
        comprobar(actual != null && decimal(esperado).compareTo(actual) == 0, mensaje);
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }
}
