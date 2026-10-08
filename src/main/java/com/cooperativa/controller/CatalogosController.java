package com.cooperativa.controller;

import com.cooperativa.dao.CategoriaProductoDAO;
import com.cooperativa.dao.EstatusDAO;
import com.cooperativa.dao.SedeDAO;
import com.cooperativa.dao.TipoCuentaDAO;
import com.cooperativa.dao.TipoPersonaDAO;
import com.cooperativa.dao.TipoPrestamoDAO;
import com.cooperativa.model.CategoriaProducto;
import com.cooperativa.model.Estatus;
import com.cooperativa.model.Sede;
import com.cooperativa.model.TipoCuenta;
import com.cooperativa.model.TipoPersona;
import com.cooperativa.model.TipoPrestamo;
import java.sql.SQLException;
import java.util.List;

/**
 * Servicio de catálogos para alimentar los componentes de la interfaz de usuario
 * (combos desplegables, selectores, filtros).
 */
public class CatalogosController {

    private final SedeDAO sedeDAO = new SedeDAO();
    private final EstatusDAO estatusDAO = new EstatusDAO();
    private final TipoPersonaDAO tipoPersonaDAO = new TipoPersonaDAO();
    private final TipoCuentaDAO tipoCuentaDAO = new TipoCuentaDAO();
    private final TipoPrestamoDAO tipoPrestamoDAO = new TipoPrestamoDAO();
    private final CategoriaProductoDAO categoriaProductoDAO = new CategoriaProductoDAO();

    public List<Sede> listarSedes() throws NegocioException {
        try {
            return sedeDAO.obtenerTodos();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo cargar el catálogo de sedes", e);
        }
    }

    public List<Estatus> listarEstatus() throws NegocioException {
        try {
            return estatusDAO.obtenerTodos();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo cargar el catálogo de estatus", e);
        }
    }

    public List<TipoPersona> listarTiposPersona() throws NegocioException {
        try {
            return tipoPersonaDAO.obtenerTodos();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo cargar el catálogo de tipos de persona", e);
        }
    }

    public List<TipoCuenta> listarTiposCuenta() throws NegocioException {
        try {
            return tipoCuentaDAO.obtenerTodos();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo cargar el catálogo de tipos de cuenta", e);
        }
    }

    public List<TipoPrestamo> listarTiposPrestamo() throws NegocioException {
        try {
            return tipoPrestamoDAO.obtenerTodos();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo cargar el catálogo de tipos de préstamo", e);
        }
    }

    public List<CategoriaProducto> listarCategoriasProducto() throws NegocioException {
        try {
            return categoriaProductoDAO.obtenerTodos();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo cargar el catálogo de categorías de producto", e);
        }
    }
}
