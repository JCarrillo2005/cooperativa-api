package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.CategoriaProducto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriaProductoDAO {

    public boolean guardar(CategoriaProducto c) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            return guardar(c, conn);
        }
    }

    public boolean guardar(CategoriaProducto c, Connection conn) throws SQLException {
        c.setId(siguienteId(conn));
        String sql = "INSERT INTO categorias_producto (id, nombre) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getId());
            ps.setString(2, c.getNombre());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_categorias_producto.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public CategoriaProducto obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre FROM categorias_producto WHERE id = ?";
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

    public List<CategoriaProducto> obtenerTodos() throws SQLException {
        List<CategoriaProducto> lista = new ArrayList<CategoriaProducto>();
        String sql = "SELECT id, nombre FROM categorias_producto ORDER BY id";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public boolean actualizar(CategoriaProducto c) throws SQLException {
        String sql = "UPDATE categorias_producto SET nombre = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setInt(2, c.getId());
            return ps.executeUpdate() == 1;
        }
    }

    /** Falla (ORA-02292) si la categoría ya tiene productos. */
    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM categorias_producto WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private CategoriaProducto mapear(ResultSet rs) throws SQLException {
        CategoriaProducto c = new CategoriaProducto();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        return c;
    }
}