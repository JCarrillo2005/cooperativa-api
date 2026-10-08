package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.TipoPrestamo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TipoPrestamoDAO {

    public boolean guardar(TipoPrestamo t) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            return guardar(t, conn);
        }
    }

    public boolean guardar(TipoPrestamo t, Connection conn) throws SQLException {
        t.setId(siguienteId(conn));
        String sql = "INSERT INTO tipos_prestamo (id, nombre) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, t.getId());
            ps.setString(2, t.getNombre());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_tipos_prestamo.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public TipoPrestamo obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre FROM tipos_prestamo WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    public List<TipoPrestamo> obtenerTodos() throws SQLException {
        List<TipoPrestamo> lista = new ArrayList<TipoPrestamo>();
        String sql = "SELECT id, nombre FROM tipos_prestamo ORDER BY id";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public boolean actualizar(TipoPrestamo t) throws SQLException {
        String sql = "UPDATE tipos_prestamo SET nombre = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getNombre());
            ps.setInt(2, t.getId());
            return ps.executeUpdate() == 1;
        }
    }

    /** Falla (ORA-02292) si el tipo ya está en uso. */
    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM tipos_prestamo WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private TipoPrestamo mapear(ResultSet rs) throws SQLException {
        TipoPrestamo t = new TipoPrestamo();
        t.setId(rs.getInt("id"));
        t.setNombre(rs.getString("nombre"));
        return t;
    }
}