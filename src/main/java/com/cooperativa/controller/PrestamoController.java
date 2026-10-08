package com.cooperativa.controller;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.dao.DetallePrestamoDAO;
import com.cooperativa.dao.PrestamoDAO;
import com.cooperativa.dao.ProductoDAO;
import com.cooperativa.dao.TipoPrestamoDAO;
import com.cooperativa.model.DetallePrestamo;
import com.cooperativa.model.Prestamo;
import com.cooperativa.model.Producto;
import com.cooperativa.model.TipoPrestamo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class PrestamoController {

    private final PrestamoDAO prestamoDAO = new PrestamoDAO();
    private final DetallePrestamoDAO detallePrestamoDAO = new DetallePrestamoDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final TipoPrestamoDAO tipoPrestamoDAO = new TipoPrestamoDAO();
    private final PersonaController personaController = new PersonaController();

    // ---------- Registrar ----------

    /**
     * Registra un préstamo. El estatus inicial es ACTIVO. Devuelve el mismo
     * objeto, ya con id y monto calculado.
     */
    public Prestamo registrarPrestamo(Prestamo p) throws NegocioException {
        if (p == null) {
            throw new NegocioException("No se recibieron los datos del préstamo.");
        }
        personaController.obtenerActiva(p.getPersonaId());

        try {
            TipoPrestamo tipo = tipoPrestamoDAO.obtenerPorId(p.getTipoPrestamoId());
            if (tipo == null) {
                throw new NegocioException("El tipo de préstamo no existe.");
            }
            boolean esConsumo = Catalogos.TIPO_PRESTAMO_CONSUMO.equalsIgnoreCase(tipo.getNombre());

            if (p.getTasaInteres() == null) {
                p.setTasaInteres(BigDecimal.ZERO);
            }
            if (p.getTasaInteres().signum() < 0) {
                throw new NegocioException("La tasa de interés no puede ser negativa.");
            }
            if (p.getFechaVencimiento() != null && p.getFechaVencimiento().before(new Date())) {
                throw new NegocioException("La fecha de vencimiento debe ser futura.");
            }
            p.setEstatusId(Catalogos.estatusId(Catalogos.ESTATUS_ACTIVO));

            if (esConsumo) {
                prepararConsumo(p);
            } else {
                prepararDinero(p, tipo);
            }

            guardarEnTransaccion(p);
            return p;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo registrar el préstamo", e);
        }
    }

    private void prepararConsumo(Prestamo p) throws NegocioException, SQLException {
        if (p.getDetalles() == null || p.getDetalles().isEmpty()) {
            throw new NegocioException("Un préstamo de consumo de productos debe tener al menos un producto.");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePrestamo d : p.getDetalles()) {
            if (d.getCantidad() <= 0) {
                throw new NegocioException("La cantidad de cada producto debe ser mayor que cero.");
            }
            Producto prod = productoDAO.obtenerPorId(d.getProductoId());
            if (prod == null) {
                throw new NegocioException("El producto con id " + d.getProductoId() + " no existe.");
            }
            d.setPrecioUnitario(prod.getPrecio());   // el precio sale del catálogo
            total = total.add(d.getSubtotal());
        }
        p.setMonto(total.setScale(2, RoundingMode.HALF_UP));
    }

    private void prepararDinero(Prestamo p, TipoPrestamo tipo) throws NegocioException {
        if (p.getDetalles() != null && !p.getDetalles().isEmpty()) {
            throw new NegocioException("Un préstamo de tipo " + tipo.getNombre() + " no lleva productos.");
        }
        if (p.getMonto() == null || p.getMonto().signum() <= 0) {
            throw new NegocioException("El monto del préstamo debe ser mayor que cero.");
        }
        p.setMonto(p.getMonto().setScale(2, RoundingMode.HALF_UP));
    }

    /** Préstamo + descuento de stock + detalles: todo o nada. */
    private void guardarEnTransaccion(Prestamo p) throws NegocioException, SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            conn.setAutoCommit(false);
            try {
                prestamoDAO.guardar(p, conn);
                for (DetallePrestamo d : p.getDetalles()) {
                    if (!productoDAO.descontarStock(d.getProductoId(), d.getCantidad(), conn)) {
                        Producto prod = productoDAO.obtenerPorId(d.getProductoId());
                        throw new NegocioException("Stock insuficiente de '" + prod
                            + "' (disponible: " + prod.getStock() + ").");
                    }
                    d.setPrestamoId(p.getId());
                    detallePrestamoDAO.guardar(d, conn);
                }
                conn.commit();
            } catch (SQLException | RuntimeException | NegocioException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ---------- Estatus ----------

    /** Por ejemplo: cambiarEstatus(5, "PAGADO"). El estatus debe existir en el catálogo. */
    public void cambiarEstatus(int prestamoId, String nombreEstatus) throws NegocioException {
        try {
            if (prestamoDAO.obtenerPorId(prestamoId) == null) {
                throw new NegocioException("El préstamo no existe.");
            }
            prestamoDAO.cambiarEstatus(prestamoId, Catalogos.estatusId(nombreEstatus));
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo cambiar el estatus del préstamo", e);
        }
    }

    /**
     * Cierre de ciclo operacional: Registra un abono parcial o liquidación total al préstamo.
     * Descuenta el monto abonado de la deuda pendiente. Si el saldo restante llega a 0,
     * cambia automáticamente el estatus del préstamo a PAGADO.
     * Devuelve el saldo restante pendiente.
     */
    public BigDecimal registrarAbono(int prestamoId, BigDecimal montoAbono) throws NegocioException {
        if (montoAbono == null || montoAbono.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("El monto del abono debe ser mayor que cero.");
        }
        Prestamo p = obtener(prestamoId);
        if (p.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("El préstamo no posee saldo pendiente por pagar.");
        }
        if (montoAbono.compareTo(p.getMonto()) > 0) {
            throw new NegocioException("El abono (Q" + montoAbono.setScale(2, RoundingMode.HALF_UP)
                + ") no puede superar el saldo pendiente actual (Q" + p.getMonto() + ").");
        }

        BigDecimal nuevoSaldo = p.getMonto().subtract(montoAbono).setScale(2, RoundingMode.HALF_UP);
        p.setMonto(nuevoSaldo);

        try {
            if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
                p.setEstatusId(Catalogos.estatusId("PAGADO"));
            }
            prestamoDAO.actualizar(p);
            return nuevoSaldo;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo registrar el abono al préstamo", e);
        }
    }

    // ---------- Consultas ----------

    /** Préstamo con sus productos (si es de consumo). */
    public Prestamo obtener(int prestamoId) throws NegocioException {
        try {
            Prestamo p = prestamoDAO.obtenerPorIdConDetalles(prestamoId);
            if (p == null) {
                throw new NegocioException("El préstamo con id " + prestamoId + " no existe.");
            }
            return p;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo consultar el préstamo", e);
        }
    }

    public List<Prestamo> listarTodos() throws NegocioException {
        try {
            return prestamoDAO.obtenerTodos();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo listar los préstamos", e);
        }
    }

    public List<Prestamo> listarPorPersona(int personaId) throws NegocioException {
        try {
            return prestamoDAO.obtenerPorPersona(personaId);
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo listar los préstamos de la persona", e);
        }
    }
}