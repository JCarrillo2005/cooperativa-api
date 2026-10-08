package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.Sede;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SedeDAO {

    public boolean guardar(Sede s) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            return guardar(s, conn);
        }
    }

    public boolean guardar(Sede s, Connection conn) throws SQLException {
        s.setId(siguienteId(conn));
        String sql = "INSERT INTO sedes (id, nombre, direccion, departamento) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, s.getId());
            ps.setString(2, s.getNombre());
            ps.setString(3, s.getDireccion());
            ps.setString(4, s.getDepartamento());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_sedes.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public Sede obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre, direccion, departamento FROM sedes WHERE id = ?";
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

    public List<Sede> obtenerTodos() throws SQLException {
        List<Sede> lista = new ArrayList<Sede>();
        String sql = "SELECT id, nombre, direccion, departamento FROM sedes ORDER BY nombre";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public boolean actualizar(Sede s) throws SQLException {
        String sql = "UPDATE sedes SET nombre = ?, direccion = ?, departamento = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getNombre());
            ps.setString(2, s.getDireccion());
            ps.setString(3, s.getDepartamento());
            ps.setInt(4, s.getId());
            return ps.executeUpdate() == 1;
        }
    }

    /** Falla (ORA-02292) si la sede ya está en uso por personas, cuentas o albaranes. */
    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM sedes WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private Sede mapear(ResultSet rs) throws SQLException {
        Sede s = new Sede();
        s.setId(rs.getInt("id"));
        s.setNombre(rs.getString("nombre"));
        s.setDireccion(rs.getString("direccion"));
        s.setDepartamento(rs.getString("departamento"));
        return s;
    }
}