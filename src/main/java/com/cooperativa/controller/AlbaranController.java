package com.cooperativa.controller;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.dao.AlbaranDAO;
import com.cooperativa.dao.CuentaDAO;
import com.cooperativa.dao.CuentaPersonaDAO;
import com.cooperativa.dao.DetalleAlbaranDAO;
import com.cooperativa.dao.ProductoDAO;
import com.cooperativa.dao.SedeDAO;
import com.cooperativa.dao.SaldoDAO;
import com.cooperativa.dao.TipoPersonaDAO;
import com.cooperativa.model.Albaran;
import com.cooperativa.model.Cuenta;
import com.cooperativa.model.DetalleAlbaran;
import com.cooperativa.model.Persona;
import com.cooperativa.model.Producto;
import com.cooperativa.model.Saldo;
import com.cooperativa.model.TipoPersona;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AlbaranController {

    private final AlbaranDAO albaranDAO = new AlbaranDAO();
    private final CuentaDAO cuentaDAO = new CuentaDAO();
    private final CuentaPersonaDAO cuentaPersonaDAO = new CuentaPersonaDAO();
    private final SaldoDAO saldoDAO = new SaldoDAO();
    private final DetalleAlbaranDAO detalleAlbaranDAO = new DetalleAlbaranDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final TipoPersonaDAO tipoPersonaDAO = new TipoPersonaDAO();
    private final SedeDAO sedeDAO = new SedeDAO();
    private final PersonaController personaController = new PersonaController();

    // ---------- Registrar ----------

    /**
     * Registra la entrega de un socio. Si el albarán no trae sede, usa la del socio.
     * Devuelve el mismo objeto, ya con id.
     */
    public Albaran registrarEntrega(Albaran a) throws NegocioException {
        if (a == null) {
            throw new NegocioException("No se recibieron los datos del albarán.");
        }
        String talonario = (a.getNumeroTalonario() == null) ? "" : a.getNumeroTalonario().trim();
        if (talonario.isEmpty()) {
            throw new NegocioException("El número de talonario es obligatorio.");
        }
        if (talonario.length() > 30) {
            throw new NegocioException("El número de talonario no puede pasar de 30 caracteres.");
        }
        a.setNumeroTalonario(talonario);

        Persona socio = personaController.obtenerActiva(a.getPersonaId());

        try {
            TipoPersona tipo = tipoPersonaDAO.obtenerPorId(socio.getTipoPersonaId());
            if (tipo == null || !Catalogos.TIPO_PERSONA_SOCIO.equalsIgnoreCase(tipo.getNombre())) {
                throw new NegocioException("Solo los socios pueden registrar entregas.");
            }

            if (a.getSedeId() == 0) {
                a.setSedeId(socio.getSedeId());
            } else if (sedeDAO.obtenerPorId(a.getSedeId()) == null) {
                throw new NegocioException("La sede no existe.");
            }

            if (albaranDAO.obtenerPorNumeroTalonario(talonario) != null) {
                throw new NegocioException("El talonario " + talonario + " ya fue registrado.");
            }

            validarDetalles(a);
            guardarEnTransaccion(a);
            return a;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo registrar la entrega", e);
        }
    }

    private void validarDetalles(Albaran a) throws NegocioException, SQLException {
        if (a.getDetalles() == null || a.getDetalles().isEmpty()) {
            throw new NegocioException("El albarán debe tener al menos un producto.");
        }
        for (DetalleAlbaran d : a.getDetalles()) {
            Producto prod = productoDAO.obtenerPorId(d.getProductoId());
            if (prod == null) {
                throw new NegocioException("El producto con id " + d.getProductoId() + " no existe.");
            }

            BigDecimal aceptada = (d.getCantidadAceptada() == null) ? BigDecimal.ZERO : d.getCantidadAceptada();
            BigDecimal rechazada = (d.getCantidadRechazada() == null) ? BigDecimal.ZERO : d.getCantidadRechazada();
            if (aceptada.signum() < 0 || rechazada.signum() < 0) {
                throw new NegocioException("Las cantidades no pueden ser negativas (producto " + prod + ").");
            }
            if (aceptada.signum() == 0 && rechazada.signum() == 0) {
                throw new NegocioException("Indica la cantidad aceptada o rechazada del producto " + prod + ".");
            }

            BigDecimal precio = (d.getPrecioUnitario() == null) ? prod.getPrecio() : d.getPrecioUnitario();
            if (precio == null || precio.signum() <= 0) {
                throw new NegocioException("El precio de " + prod + " debe ser mayor que cero.");
            }

            d.setCantidadAceptada(aceptada.setScale(2, RoundingMode.HALF_UP));
            d.setCantidadRechazada(rechazada.setScale(2, RoundingMode.HALF_UP));
            d.setPrecioUnitario(precio.setScale(2, RoundingMode.HALF_UP));
        }
    }

    /** Encabezado + todas las líneas: todo o nada. */
    private void guardarEnTransaccion(Albaran a) throws NegocioException, SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            conn.setAutoCommit(false);
            try {
                albaranDAO.guardar(a, conn);
                for (DetalleAlbaran d : a.getDetalles()) {
                    d.setAlbaranId(a.getId());
                    detalleAlbaranDAO.guardar(d, conn);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ---------- Total ----------

    /** Total a pagar al socio: solo cuenta lo aceptado. */
    public BigDecimal totalAPagar(Albaran a) {
        return a.calcularTotal().setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Cierre de ciclo operacional: Liquida el total aceptado del albarán acreditándolo
     * directamente como depósito a una cuenta bancaria del socio.
     */
    public BigDecimal liquidarAlbaranEnCuenta(int albaranId, int cuentaId) throws NegocioException {
        try {
            // El catalogo se consulta antes de retener una conexion o bloqueos del pool.
            int estatusActivo = Catalogos.estatusId(Catalogos.ESTATUS_ACTIVO);
            try (Connection conn = ConexionOracle.getConexion()) {
                conn.setAutoCommit(false);
                try {
                    albaranDAO.bloquear(albaranId, conn);
                    Albaran a = albaranDAO.obtenerPorId(albaranId, conn);
                    if (a == null) {
                        throw new NegocioException("El albarán no existe.");
                    }
                    if (a.isLiquidado()) {
                        throw new NegocioException("Este albarán ya fue liquidado. No se volverá a abonar.");
                    }
                    a.setDetalles(detalleAlbaranDAO.obtenerPorAlbaran(albaranId, conn));
                    BigDecimal total = totalAPagar(a);
                    if (total.signum() <= 0) {
                        throw new NegocioException("El albarán no posee producto aceptado para liquidar (total Q0.00).");
                    }
                    // Siempre albaran -> cuenta -> relacion: evita invertir el orden de bloqueos.
                    cuentaDAO.bloquear(cuentaId, conn);
                    Cuenta cuenta = cuentaDAO.obtenerPorId(cuentaId, conn);
                    if (cuenta == null) {
                        throw new NegocioException("La cuenta no existe.");
                    }
                    if (cuenta.getEstatusId() != estatusActivo) {
                        throw new NegocioException("La cuenta no está activa.");
                    }
                    if (!cuentaPersonaDAO.perteneceYBloquear(cuentaId, a.getPersonaId(), conn)) {
                        throw new NegocioException("La cuenta no pertenece al socio de este albarán.");
                    }
                    Saldo actual = saldoDAO.obtenerActual(cuentaId, conn);
                    BigDecimal base = actual == null ? BigDecimal.ZERO : actual.getMonto();
                    if (!saldoDAO.registrar(new Saldo(0, cuentaId, base.add(total), null), conn)) {
                        throw new SQLException("No se pudo registrar el saldo de la liquidacion.");
                    }
                    if (!albaranDAO.marcarLiquidado(albaranId, conn)) {
                        throw new NegocioException("Este albarán ya fue liquidado. No se volverá a abonar.");
                    }
                    conn.commit();
                    return total;
                } catch (SQLException | RuntimeException | NegocioException e) {
                    conn.rollback();
                    throw e;
                }
            }
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo liquidar el albarán", e);
        }
    }

    // ---------- Consultas ----------

    /** Albarán con sus líneas. */
    public Albaran obtener(int albaranId) throws NegocioException {
        try {
            Albaran a = albaranDAO.obtenerPorIdConDetalles(albaranId);
            if (a == null) {
                throw new NegocioException("El albarán con id " + albaranId + " no existe.");
            }
            return a;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo consultar el albarán", e);
        }
    }

    /** Devuelve el albarán (con líneas) o null si el talonario no existe. */
    public Albaran buscarPorTalonario(String talonario) throws NegocioException {
        try {
            Albaran a = albaranDAO.obtenerPorNumeroTalonario(talonario == null ? "" : talonario.trim());
            if (a != null) {
                a.setDetalles(detalleAlbaranDAO.obtenerPorAlbaran(a.getId()));
            }
            return a;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo buscar el talonario", e);
        }
    }

    public List<Albaran> listarTodos() throws NegocioException {
        try {
            return albaranDAO.obtenerTodos();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo listar los albaranes", e);
        }
    }

    public List<Albaran> listarPorSocio(int personaId) throws NegocioException {
        try {
            return albaranDAO.obtenerPorPersona(personaId);
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo listar las entregas del socio", e);
        }
    }
}
