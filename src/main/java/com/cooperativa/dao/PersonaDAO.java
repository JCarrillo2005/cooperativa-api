package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.Persona;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PersonaDAO {

    private static final String COLUMNAS =
        "id, nombre, apellido, dni, direccion, telefono, "
        + "tipo_persona_id, estatus_id, sede_id, fecha_registro";

    // ---------- CREATE ----------

    /** Guarda una persona gestionando su propia transacción y commit. */
    public boolean guardar(Persona p) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false); // Iniciar transacción explícita
                boolean resultado = guardar(p, conn);
                conn.commit();            // Confirmar cambios en Oracle
                return resultado;
            } catch (SQLException e) {
                conn.rollback();          // Revertir si hay error
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    /** Versión para usar dentro de una transacción externa: no abre ni hace commit/rollback local. */
    public boolean guardar(Persona p, Connection conn) throws SQLException {
        p.setId(siguienteId(conn));

        String sql = "INSERT INTO personas (" + COLUMNAS + ") "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getId());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getApellido());
            ps.setString(4, p.getDni());
            ps.setString(5, p.getDireccion());
            ps.setString(6, p.getTelefono());
            ps.setInt(7, p.getTipoPersonaId());
            ps.setInt(8, p.getEstatusId());
            ps.setInt(9, p.getSedeId());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        String sql = "SELECT seq_personas.NEXTVAL FROM dual";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("No se pudo obtener el siguiente ID de la secuencia.");
        }
    }

    // ---------- READ ----------

    public Persona obtenerPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM personas WHERE id = ?";
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

    public Persona obtenerPorDni(String dni) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM personas WHERE dni = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    public List<Persona> obtenerTodos() throws SQLException {
        List<Persona> lista = new ArrayList<Persona>();
        String sql = "SELECT " + COLUMNAS + " FROM personas ORDER BY apellido, nombre";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Persona> obtenerPorTipo(int tipoPersonaId) throws SQLException {
        List<Persona> lista = new ArrayList<Persona>();
        String sql = "SELECT " + COLUMNAS + " FROM personas "
                   + "WHERE tipo_persona_id = ? ORDER BY apellido, nombre";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tipoPersonaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    // ---------- UPDATE ----------

    public boolean actualizar(Persona p) throws SQLException {
        String sql = "UPDATE personas SET nombre = ?, apellido = ?, dni = ?, "
                   + "direccion = ?, telefono = ?, tipo_persona_id = ?, "
                   + "estatus_id = ?, sede_id = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean actualizado;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, p.getNombre());
                    ps.setString(2, p.getApellido());
                    ps.setString(3, p.getDni());
                    ps.setString(4, p.getDireccion());
                    ps.setString(5, p.getTelefono());
                    ps.setInt(6, p.getTipoPersonaId());
                    ps.setInt(7, p.getEstatusId());
                    ps.setInt(8, p.getSedeId());
                    ps.setInt(9, p.getId());
                    actualizado = ps.executeUpdate() == 1;
                }
                conn.commit();
                return actualizado;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    public boolean cambiarEstatus(int personaId, int estatusId) throws SQLException {
        String sql = "UPDATE personas SET estatus_id = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean cambiado;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, estatusId);
                    ps.setInt(2, personaId);
                    cambiado = ps.executeUpdate() == 1;
                }
                conn.commit();
                return cambiado;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    // ---------- DELETE ----------

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM personas WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean eliminado;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    eliminado = ps.executeUpdate() == 1;
                }
                conn.commit();
                return eliminado;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    // ---------- Mapeo ResultSet -> Persona ----------

    private Persona mapear(ResultSet rs) throws SQLException {
        Persona p = new Persona();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setApellido(rs.getString("apellido"));
        p.setDni(rs.getString("dni"));
        p.setDireccion(rs.getString("direccion"));
        p.setTelefono(rs.getString("telefono"));
        p.setTipoPersonaId(rs.getInt("tipo_persona_id"));
        p.setEstatusId(rs.getInt("estatus_id"));
        p.setSedeId(rs.getInt("sede_id"));
        p.setFechaRegistro(rs.getTimestamp("fecha_registro"));
        return p;
    }
}