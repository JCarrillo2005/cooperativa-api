package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.Cuenta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CuentaDAO {

    private static final String COLUMNAS =
        "id, numero, tipo_cuenta_id, estatus_id, sede_id, fecha_apertura";

    // ---------- CREATE ----------

    public boolean guardar(Cuenta c) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean resultado = guardar(c, conn);
                conn.commit();
                return resultado;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    /** Para usar dentro de una transacción externa (cuenta + titulares + saldo inicial). */
    public boolean guardar(Cuenta c, Connection conn) throws SQLException {
        c.setId(siguienteId(conn));
        String sql = "INSERT INTO cuentas (" + COLUMNAS + ") VALUES (?, ?, ?, ?, ?, SYSDATE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getId());
            ps.setString(2, c.getNumero());
            ps.setInt(3, c.getTipoCuentaId());
            ps.setInt(4, c.getEstatusId());
            ps.setInt(5, c.getSedeId());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_cuentas.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("No se pudo obtener el siguiente ID de la secuencia seq_cuentas.");
        }
    }

    // ---------- READ ----------

    public Cuenta obtenerPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM cuentas WHERE id = ?";
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

    public Cuenta obtenerPorNumero(String numero) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM cuentas WHERE numero = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    public List<Cuenta> obtenerTodos() throws SQLException {
        List<Cuenta> lista = new ArrayList<Cuenta>();
        String sql = "SELECT " + COLUMNAS + " FROM cuentas ORDER BY id";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Cuenta> obtenerPorPersona(int personaId) throws SQLException {
        List<Cuenta> lista = new ArrayList<Cuenta>();
        String sql = "SELECT c.id, c.numero, c.tipo_cuenta_id, c.estatus_id, c.sede_id, c.fecha_apertura "
                   + "FROM cuentas c JOIN cuentas_personas cp ON cp.cuenta_id = c.id "
                   + "WHERE cp.persona_id = ? ORDER BY c.id";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    // ---------- UPDATE ----------

    public boolean actualizar(Cuenta c) throws SQLException {
        String sql = "UPDATE cuentas SET numero = ?, tipo_cuenta_id = ?, estatus_id = ?, "
                   + "sede_id = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean actualizado;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, c.getNumero());
                    ps.setInt(2, c.getTipoCuentaId());
                    ps.setInt(3, c.getEstatusId());
                    ps.setInt(4, c.getSedeId());
                    ps.setInt(5, c.getId());
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

    public boolean cambiarEstatus(int cuentaId, int estatusId) throws SQLException {
        String sql = "UPDATE cuentas SET estatus_id = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean cambiado;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, estatusId);
                    ps.setInt(2, cuentaId);
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
        String sql = "DELETE FROM cuentas WHERE id = ?";
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

    // ---------- Mapeo ----------

    private Cuenta mapear(ResultSet rs) throws SQLException {
        Cuenta c = new Cuenta();
        c.setId(rs.getInt("id"));
        c.setNumero(rs.getString("numero"));
        c.setTipoCuentaId(rs.getInt("tipo_cuenta_id"));
        c.setEstatusId(rs.getInt("estatus_id"));
        c.setSedeId(rs.getInt("sede_id"));
        c.setFechaApertura(rs.getTimestamp("fecha_apertura"));
        return c;
    }
    
    public void bloquear(int cuentaId, Connection conn) throws SQLException {
        String sql = "SELECT id FROM cuentas WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cuentaId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
            }
        }
    }
}