package com.cooperativa.dao;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.model.Producto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    private static final String COLUMNAS = "id, nombre, descripcion, categoria_id, precio, stock";

    // ---------- CREATE ----------

    public boolean guardar(Producto p) throws SQLException {
        try (Connection conn = ConexionOracle.getConexion()) {
            return guardar(p, conn);
        }
    }

    public boolean guardar(Producto p, Connection conn) throws SQLException {
        p.setId(siguienteId(conn));
        String sql = "INSERT INTO productos (" + COLUMNAS + ") VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getId());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getDescripcion());
            ps.setInt(4, p.getCategoriaId());
            ps.setBigDecimal(5, p.getPrecio());
            ps.setInt(6, p.getStock());
            return ps.executeUpdate() == 1;
        }
    }

    private int siguienteId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT seq_productos.NEXTVAL FROM dual");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ---------- READ ----------

    public Producto obtenerPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM productos WHERE id = ?";
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

    public List<Producto> obtenerTodos() throws SQLException {
        List<Producto> lista = new ArrayList<Producto>();
        String sql = "SELECT " + COLUMNAS + " FROM productos ORDER BY nombre";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Producto> obtenerPorCategoria(int categoriaId) throws SQLException {
        List<Producto> lista = new ArrayList<Producto>();
        String sql = "SELECT " + COLUMNAS + " FROM productos WHERE categoria_id = ? ORDER BY nombre";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoriaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    // ---------- UPDATE ----------

    public boolean actualizar(Producto p) throws SQLException {
        String sql = "UPDATE productos SET nombre = ?, descripcion = ?, categoria_id = ?, "
                   + "precio = ?, stock = ? WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setInt(3, p.getCategoriaId());
            ps.setBigDecimal(4, p.getPrecio());
            ps.setInt(5, p.getStock());
            ps.setInt(6, p.getId());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Resta stock dentro de una transacción. Devuelve false si no hay
     * suficiente inventario (en ese caso no modifica nada).
     */
    public boolean descontarStock(int productoId, int cantidad, Connection conn) throws SQLException {
        String sql = "UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, productoId);
            ps.setInt(3, cantidad);
            return ps.executeUpdate() == 1;
        }
    }

    /** Suma stock dentro de una transacción. */
    public boolean aumentarStock(int productoId, int cantidad, Connection conn) throws SQLException {
        String sql = "UPDATE productos SET stock = stock + ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, productoId);
            return ps.executeUpdate() == 1;
        }
    }

    // ---------- DELETE ----------

    /** Falla (ORA-02292) si el producto ya aparece en préstamos o albaranes. */
    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM productos WHERE id = ?";
        try (Connection conn = ConexionOracle.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    // ---------- Mapeo ----------

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setCategoriaId(rs.getInt("categoria_id"));
        p.setPrecio(rs.getBigDecimal("precio"));
        p.setStock(rs.getInt("stock"));
        return p;
    }
}