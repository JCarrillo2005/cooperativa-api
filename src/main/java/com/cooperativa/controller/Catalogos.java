package com.cooperativa.controller;

import com.cooperativa.dao.EstatusDAO;
import com.cooperativa.dao.TipoPersonaDAO;
import com.cooperativa.dao.TipoPrestamoDAO;
import com.cooperativa.model.Estatus;
import com.cooperativa.model.TipoPersona;
import com.cooperativa.model.TipoPrestamo;
import java.sql.SQLException;

/** Utilidad para obtener ids de catálogos a partir de su nombre. */
public class Catalogos {

    public static final String ESTATUS_ACTIVO = "ACTIVO";
    public static final String ESTATUS_INACTIVO = "INACTIVO";
    public static final String TIPO_PERSONA_SOCIO = "SOCIO";
    public static final String TIPO_PRESTAMO_CONSUMO = "CONSUMO_PRODUCTOS";

    private Catalogos() {
        // Clase utilitaria: no se instancia
    }

    public static int estatusId(String nombre) throws NegocioException {
        try {
            Estatus e = new EstatusDAO().obtenerPorNombre(nombre);
            if (e == null) {
                throw new NegocioException("No existe el estatus '" + nombre + "' en el catálogo.");
            }
            return e.getId();
        } catch (SQLException ex) {
            throw NegocioException.deSQL("No se pudo consultar el catálogo de estatus", ex);
        }
    }

    public static int tipoPersonaId(String nombre) throws NegocioException {
        try {
            for (TipoPersona t : new TipoPersonaDAO().obtenerTodos()) {
                if (t.getNombre().equalsIgnoreCase(nombre)) {
                    return t.getId();
                }
            }
            throw new NegocioException("No existe el tipo de persona '" + nombre + "' en el catálogo.");
        } catch (SQLException ex) {
            throw NegocioException.deSQL("No se pudo consultar el catálogo de tipos de persona", ex);
        }
    }

    public static int tipoPrestamoId(String nombre) throws NegocioException {
        try {
            for (TipoPrestamo t : new TipoPrestamoDAO().obtenerTodos()) {
                if (t.getNombre().equalsIgnoreCase(nombre)) {
                    return t.getId();
                }
            }
            throw new NegocioException("No existe el tipo de préstamo '" + nombre + "' en el catálogo.");
        } catch (SQLException ex) {
            throw NegocioException.deSQL("No se pudo consultar el catálogo de tipos de préstamo", ex);
        }
    }
}