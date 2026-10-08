package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.Albaran;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlbaranDAO {

    private static final String COLUMNAS = "id, numero_talonario, persona_id, sede_id, fecha";

    // ---------- CREATE ----------

    public boolean guardar(Albaran a) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            return guardar(a, conn);
        }
    }

    /**
     * Guarda solo el encabezado (los detalles los guarda DetalleAlbaranDAO).
     * La fecha se toma de SYSDATE.
     */
    public boolean guardar(Albaran a, Connection conn) throws SQLException {
        a.setId(siguienteId(conn));
        String sql = "INSERT INTO albaranes (" + COLUMNAS + ") VALUES (?, ?, ?, ?, SYSDATE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, a.getId());
            ps.setString(2, a.getNumeroTalonario());
            ps.setInt(3, a.getPersonaId());
            ps.setInt(4, a.getSedeId());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_albaranes.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ---------- READ ----------

    /** Solo el encabezado (sin la lista de detalles). */
    public Albaran obtenerPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM albaranes WHERE id = ?";
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

    /** Encabezado + líneas (producto, cantidad aceptada y rechazada, precio). */
    public Albaran obtenerPorIdConDetalles(int id) throws SQLException {
        Albaran a = obtenerPorId(id);
        if (a != null) {
            a.setDetalles(new DetalleAlbaranDAO().obtenerPorAlbaran(id));
        }
        return a;
    }

    public Albaran obtenerPorNumeroTalonario(String numeroTalonario) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM albaranes WHERE numero_talonario = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numeroTalonario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    public List<Albaran> obtenerTodos() throws SQLException {
        List<Albaran> lista = new ArrayList<Albaran>();
        String sql = "SELECT " + COLUMNAS + " FROM albaranes ORDER BY fecha DESC, id DESC";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Todas las entregas de un socio. */
    public List<Albaran> obtenerPorPersona(int personaId) throws SQLException {
        List<Albaran> lista = new ArrayList<Albaran>();
        String sql = "SELECT " + COLUMNAS + " FROM albaranes "
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

    // ---------- Mapeo ----------

    private Albaran mapear(ResultSet rs) throws SQLException {
        Albaran a = new Albaran();
        a.setId(rs.getInt("id"));
        a.setNumeroTalonario(rs.getString("numero_talonario"));
        a.setPersonaId(rs.getInt("persona_id"));
        a.setSedeId(rs.getInt("sede_id"));
        a.setFecha(rs.getTimestamp("fecha"));
        return a;
    }
}