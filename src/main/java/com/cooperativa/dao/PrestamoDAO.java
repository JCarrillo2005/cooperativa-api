package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.Prestamo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PrestamoDAO {

    private static final String COLUMNAS =
        "id, persona_id, tipo_prestamo_id, estatus_id, monto, tasa_interes, fecha, fecha_vencimiento";

    // ---------- CREATE ----------

    public boolean guardar(Prestamo p) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean resultado = guardar(p, conn);
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

    /**
     * Guarda solo el encabezado del préstamo (los detalles los guarda DetallePrestamoDAO).
     */
    public boolean guardar(Prestamo p, Connection conn) throws SQLException {
        p.setId(siguienteId(conn));
        String sql = "INSERT INTO prestamos (" + COLUMNAS + ") "
                   + "VALUES (?, ?, ?, ?, ?, ?, SYSDATE, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getId());
            ps.setInt(2, p.getPersonaId());
            ps.setInt(3, p.getTipoPrestamoId());
            ps.setInt(4, p.getEstatusId());
            ps.setBigDecimal(5, p.getMonto());
            ps.setBigDecimal(6, p.getTasaInteres());
            if (p.getFechaVencimiento() == null) {
                ps.setNull(7, Types.DATE);
            } else {
                ps.setDate(7, new java.sql.Date(p.getFechaVencimiento().getTime()));
            }
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_prestamos.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("No se pudo obtener el siguiente ID de la secuencia seq_prestamos.");
        }
    }

    // ---------- READ ----------

    public Prestamo obtenerPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM prestamos WHERE id = ?";
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

    public Prestamo obtenerPorIdConDetalles(int id) throws SQLException {
        Prestamo p = obtenerPorId(id);
        if (p != null) {
            p.setDetalles(new DetallePrestamoDAO().obtenerPorPrestamo(id));
        }
        return p;
    }

    public List<Prestamo> obtenerTodos() throws SQLException {
        List<Prestamo> lista = new ArrayList<Prestamo>();
        String sql = "SELECT " + COLUMNAS + " FROM prestamos ORDER BY fecha DESC, id DESC";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Prestamo> obtenerPorPersona(int personaId) throws SQLException {
        List<Prestamo> lista = new ArrayList<Prestamo>();
        String sql = "SELECT " + COLUMNAS + " FROM prestamos "
                   + "WHERE persona_id = ? ORDER BY fecha DESC, id DESC";
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

    public boolean actualizar(Prestamo p) throws SQLException {
        String sql = "UPDATE prestamos SET monto = ?, tasa_interes = ?, "
                   + "fecha_vencimiento = ?, estatus_id = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean actualizado;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setBigDecimal(1, p.getMonto());
                    ps.setBigDecimal(2, p.getTasaInteres());
                    if (p.getFechaVencimiento() == null) {
                        ps.setNull(3, Types.DATE);
                    } else {
                        ps.setDate(3, new java.sql.Date(p.getFechaVencimiento().getTime()));
                    }
                    ps.setInt(4, p.getEstatusId());
                    ps.setInt(5, p.getId());
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

    public boolean cambiarEstatus(int prestamoId, int estatusId) throws SQLException {
        String sql = "UPDATE prestamos SET estatus_id = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                boolean cambiado;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, estatusId);
                    ps.setInt(2, prestamoId);
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

    // ---------- Mapeo ----------

    private Prestamo mapear(ResultSet rs) throws SQLException {
        Prestamo p = new Prestamo();
        p.setId(rs.getInt("id"));
        p.setPersonaId(rs.getInt("persona_id"));
        p.setTipoPrestamoId(rs.getInt("tipo_prestamo_id"));
        p.setEstatusId(rs.getInt("estatus_id"));
        p.setMonto(rs.getBigDecimal("monto"));
        p.setTasaInteres(rs.getBigDecimal("tasa_interes"));
        p.setFecha(rs.getTimestamp("fecha"));
        p.setFechaVencimiento(rs.getTimestamp("fecha_vencimiento"));
        return p;
    }
}