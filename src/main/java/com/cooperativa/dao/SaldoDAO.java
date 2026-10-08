package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.Saldo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SaldoDAO {

    public boolean registrar(Saldo s) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            return registrar(s, conn);
        }
    }

    /** Para usar dentro de una transacción. La fecha se toma de SYSDATE. */
    public boolean registrar(Saldo s, Connection conn) throws SQLException {
        s.setId(siguienteId(conn));
        String sql = "INSERT INTO saldos (id, cuenta_id, monto, fecha) VALUES (?, ?, ?, SYSDATE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, s.getId());
            ps.setInt(2, s.getCuentaId());
            ps.setBigDecimal(3, s.getMonto());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_saldos.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Saldo vigente de la cuenta: el registro más reciente. Null si no tiene ninguno. */
    public Saldo obtenerActual(int cuentaId) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            return obtenerActual(cuentaId, conn);
        }
    }

    /** Versión para transacciones (para leer el saldo antes de modificarlo). */
    public Saldo obtenerActual(int cuentaId, Connection conn) throws SQLException {
        String sql = "SELECT id, cuenta_id, monto, fecha FROM ("
                   + "  SELECT id, cuenta_id, monto, fecha FROM saldos "
                   + "  WHERE cuenta_id = ? ORDER BY fecha DESC, id DESC"
                   + ") WHERE ROWNUM = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cuentaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    /** Historial completo, del más reciente al más antiguo. */
    public List<Saldo> obtenerHistorial(int cuentaId) throws SQLException {
        List<Saldo> lista = new ArrayList<Saldo>();
        String sql = "SELECT id, cuenta_id, monto, fecha FROM saldos "
                   + "WHERE cuenta_id = ? ORDER BY fecha DESC, id DESC";
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

    private Saldo mapear(ResultSet rs) throws SQLException {
        Saldo s = new Saldo();
        s.setId(rs.getInt("id"));
        s.setCuentaId(rs.getInt("cuenta_id"));
        s.setMonto(rs.getBigDecimal("monto"));
        s.setFecha(rs.getTimestamp("fecha"));
        return s;
    }
}