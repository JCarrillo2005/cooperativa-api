package com.cooperativa.controller;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.dao.CategoriaProductoDAO;
import com.cooperativa.dao.ProductoDAO;
import com.cooperativa.model.Producto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoController {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaProductoDAO categoriaDAO = new CategoriaProductoDAO();

    // ---------- Registrar y actualizar ----------

    public Producto registrar(Producto p) throws NegocioException {
        if (p == null) {
            throw new NegocioException("No se recibieron los datos del producto.");
        }
        try {
            validar(p);
            productoDAO.guardar(p);
            return p;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo registrar el producto", e);
        }
    }

    public Producto actualizar(Producto p) throws NegocioException {
        if (p == null) {
            throw new NegocioException("No se recibieron los datos del producto.");
        }
        try {
            if (productoDAO.obtenerPorId(p.getId()) == null) {
                throw new NegocioException("El producto no existe.");
            }
            validar(p);
            productoDAO.actualizar(p);
            return p;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo actualizar el producto", e);
        }
    }

    /** Cambia solo el precio. Los préstamos ya registrados conservan el precio con el que se hicieron. */
    public void cambiarPrecio(int productoId, BigDecimal nuevoPrecio) throws NegocioException {
        Producto p = obtenerPorId(productoId);
        p.setPrecio(nuevoPrecio);
        actualizar(p);
    }

    /** Entrada de mercancía: suma unidades al inventario. */
    public void agregarStock(int productoId, int cantidad) throws NegocioException {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad a agregar debe ser mayor que cero.");
        }
        try {
            if (productoDAO.obtenerPorId(productoId) == null) {
                throw new NegocioException("El producto no existe.");
            }
            try (Connection conn = ConexionOracle.getConexion()) {
                productoDAO.aumentarStock(productoId, cantidad, conn);   // autocommit
            }
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo agregar stock", e);
        }
    }

    // ---------- Consultas ----------

    /** Devuelve el producto o lanza NegocioException si no existe. */
    public Producto obtenerPorId(int id) throws NegocioException {
        try {
            Producto p = productoDAO.obtenerPorId(id);
            if (p == null) {
                throw new NegocioException("El producto con id " + id + " no existe.");
            }
            return p;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo consultar el producto", e);
        }
    }

    public List<Producto> listarTodos() throws NegocioException {
        try {
            return productoDAO.obtenerTodos();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo listar los productos", e);
        }
    }

    public List<Producto> listarPorCategoria(int categoriaId) throws NegocioException {
        try {
            return productoDAO.obtenerPorCategoria(categoriaId);
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo listar los productos de la categoría", e);
        }
    }

    /** Productos con stock igual o menor al umbral (para avisos de reabastecimiento). */
    public List<Producto> listarStockBajo(int umbral) throws NegocioException {
        List<Producto> bajos = new ArrayList<Producto>();
        for (Producto p : listarTodos()) {
            if (p.getStock() <= umbral) {
                bajos.add(p);
            }
        }
        return bajos;
    }

    // ---------- Eliminar ----------

    /** Falla con un mensaje claro si el producto ya aparece en préstamos o albaranes. */
    public void eliminar(int productoId) throws NegocioException {
        try {
            if (productoDAO.obtenerPorId(productoId) == null) {
                throw new NegocioException("El producto no existe.");
            }
            productoDAO.eliminar(productoId);
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo eliminar el producto", e);
        }
    }

    // ---------- Validaciones ----------

    private void validar(Producto p) throws NegocioException, SQLException {
        if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
            throw new NegocioException("El nombre del producto es obligatorio.");
        }
        p.setNombre(p.getNombre().trim());
        if (p.getNombre().length() > 80) {
            throw new NegocioException("El nombre no puede pasar de 80 caracteres.");
        }
        if (p.getDescripcion() != null && p.getDescripcion().length() > 200) {
            throw new NegocioException("La descripción no puede pasar de 200 caracteres.");
        }
        if (categoriaDAO.obtenerPorId(p.getCategoriaId()) == null) {
            throw new NegocioException("La categoría del producto no existe.");
        }
        if (p.getPrecio() == null || p.getPrecio().signum() <= 0) {
            throw new NegocioException("El precio debe ser mayor que cero.");
        }
        p.setPrecio(p.getPrecio().setScale(2, RoundingMode.HALF_UP));
        if (p.getStock() < 0) {
            throw new NegocioException("El stock no puede ser negativo.");
        }
    }
}