package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.Estatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EstatusDAO {

    public boolean guardar(Estatus e) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            return guardar(e, conn);
        }
    }

    public boolean guardar(Estatus e, Connection conn) throws SQLException {
        e.setId(siguienteId(conn));
        String sql = "INSERT INTO estatus (id, nombre, descripcion) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, e.getId());
            ps.setString(2, e.getNombre());
            ps.setString(3, e.getDescripcion());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_estatus.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public Estatus obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre, descripcion FROM estatus WHERE id = ?";
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

    /** Útil en los controllers: obtenerPorNombre("ACTIVO"). */
    public Estatus obtenerPorNombre(String nombre) throws SQLException {
        String sql = "SELECT id, nombre, descripcion FROM estatus WHERE UPPER(nombre) = UPPER(?)";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    public List<Estatus> obtenerTodos() throws SQLException {
        List<Estatus> lista = new ArrayList<Estatus>();
        String sql = "SELECT id, nombre, descripcion FROM estatus ORDER BY id";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public boolean actualizar(Estatus e) throws SQLException {
        String sql = "UPDATE estatus SET nombre = ?, descripcion = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getNombre());
            ps.setString(2, e.getDescripcion());
            ps.setInt(3, e.getId());
            return ps.executeUpdate() == 1;
        }
    }

    /** Falla (ORA-02292) si el estatus ya está en uso. */
    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM estatus WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private Estatus mapear(ResultSet rs) throws SQLException {
        Estatus e = new Estatus();
        e.setId(rs.getInt("id"));
        e.setNombre(rs.getString("nombre"));
        e.setDescripcion(rs.getString("descripcion"));
        return e;
    }
}