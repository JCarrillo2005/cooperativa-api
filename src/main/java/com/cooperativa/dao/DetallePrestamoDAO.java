package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.DetallePrestamo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DetallePrestamoDAO {

    public boolean guardar(DetallePrestamo d) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            return guardar(d, conn);
        }
    }

    /** Para usar dentro de la transacción del préstamo. Requiere d.getPrestamoId() ya asignado. */
    public boolean guardar(DetallePrestamo d, Connection conn) throws SQLException {
        d.setId(siguienteId(conn));
        String sql = "INSERT INTO detalles_prestamo (id, prestamo_id, producto_id, cantidad, precio_unitario) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getId());
            ps.setInt(2, d.getPrestamoId());
            ps.setInt(3, d.getProductoId());
            ps.setInt(4, d.getCantidad());
            ps.setBigDecimal(5, d.getPrecioUnitario());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_detalles_prestamo.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<DetallePrestamo> obtenerPorPrestamo(int prestamoId) throws SQLException {
        List<DetallePrestamo> lista = new ArrayList<DetallePrestamo>();
        String sql = "SELECT id, prestamo_id, producto_id, cantidad, precio_unitario "
                   + "FROM detalles_prestamo WHERE prestamo_id = ? ORDER BY id";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private DetallePrestamo mapear(ResultSet rs) throws SQLException {
        DetallePrestamo d = new DetallePrestamo();
        d.setId(rs.getInt("id"));
        d.setPrestamoId(rs.getInt("prestamo_id"));
        d.setProductoId(rs.getInt("producto_id"));
        d.setCantidad(rs.getInt("cantidad"));
        d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        return d;
    }
}