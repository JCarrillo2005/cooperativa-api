package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.CuentaPersona;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CuentaPersonaDAO {

    public boolean agregar(CuentaPersona cp) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean resultado = agregar(cp, conn);
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

    /** Para usar dentro de una transacción externa. */
    public boolean agregar(CuentaPersona cp, Connection conn) throws SQLException {
        String sql = "INSERT INTO cuentas_personas (cuenta_id, persona_id, titular) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cp.getCuentaId());
            ps.setInt(2, cp.getPersonaId());
            ps.setInt(3, cp.isTitular() ? 1 : 0);
            return ps.executeUpdate() == 1;
        }
    }

    public List<CuentaPersona> obtenerPorCuenta(int cuentaId) throws SQLException {
        List<CuentaPersona> lista = new ArrayList<CuentaPersona>();
        String sql = "SELECT cuenta_id, persona_id, titular FROM cuentas_personas "
                   + "WHERE cuenta_id = ? ORDER BY titular DESC, persona_id";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cuentaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public List<CuentaPersona> obtenerPorPersona(int personaId) throws SQLException {
        List<CuentaPersona> lista = new ArrayList<CuentaPersona>();
        String sql = "SELECT cuenta_id, persona_id, titular FROM cuentas_personas "
                   + "WHERE persona_id = ? ORDER BY cuenta_id";
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

    public boolean cambiarTitular(int cuentaId, int personaId, boolean titular) throws SQLException {
        String sql = "UPDATE cuentas_personas SET titular = ? WHERE cuenta_id = ? AND persona_id = ?";
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean cambiado;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, titular ? 1 : 0);
                    ps.setInt(2, cuentaId);
                    ps.setInt(3, personaId);
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

    public boolean quitar(int cuentaId, int personaId) throws SQLException {
        String sql = "DELETE FROM cuentas_personas WHERE cuenta_id = ? AND persona_id = ?";
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean eliminado;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, cuentaId);
                    ps.setInt(2, personaId);
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

    private CuentaPersona mapear(ResultSet rs) throws SQLException {
        CuentaPersona cp = new CuentaPersona();
        cp.setCuentaId(rs.getInt("cuenta_id"));
        cp.setPersonaId(rs.getInt("persona_id"));
        cp.setTitular(rs.getInt("titular") == 1);
        return cp;
    }
}