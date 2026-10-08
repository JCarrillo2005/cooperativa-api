package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.DetalleAlbaran;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DetalleAlbaranDAO {

    public boolean guardar(DetalleAlbaran d) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            return guardar(d, conn);
        }
    }

    /** Para usar dentro de la transacción del albarán. Requiere d.getAlbaranId() ya asignado. */
    public boolean guardar(DetalleAlbaran d, Connection conn) throws SQLException {
        d.setId(siguienteId(conn));
        String sql = "INSERT INTO detalles_albaran "
                   + "(id, albaran_id, producto_id, cantidad_aceptada, cantidad_rechazada, precio_unitario) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getId());
            ps.setInt(2, d.getAlbaranId());
            ps.setInt(3, d.getProductoId());
            ps.setBigDecimal(4, d.getCantidadAceptada());
            ps.setBigDecimal(5, d.getCantidadRechazada());
            ps.setBigDecimal(6, d.getPrecioUnitario());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_detalles_albaran.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<DetalleAlbaran> obtenerPorAlbaran(int albaranId) throws SQLException {
        List<DetalleAlbaran> lista = new ArrayList<DetalleAlbaran>();
        String sql = "SELECT id, albaran_id, producto_id, cantidad_aceptada, cantidad_rechazada, precio_unitario "
                   + "FROM detalles_albaran WHERE albaran_id = ? ORDER BY id";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, albaranId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private DetalleAlbaran mapear(ResultSet rs) throws SQLException {
        DetalleAlbaran d = new DetalleAlbaran();
        d.setId(rs.getInt("id"));
        d.setAlbaranId(rs.getInt("albaran_id"));
        d.setProductoId(rs.getInt("producto_id"));
        d.setCantidadAceptada(rs.getBigDecimal("cantidad_aceptada"));
        d.setCantidadRechazada(rs.getBigDecimal("cantidad_rechazada"));
        d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        return d;
    }
}