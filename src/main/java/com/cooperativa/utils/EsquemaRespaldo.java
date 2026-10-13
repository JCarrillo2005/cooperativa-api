package com.cooperativa.utils;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Contrato explícito de las 15 tablas, conforme a las columnas de los DAO.
 * El orden de la lista es el orden de copia: primero los catálogos, al final los hijos,
 * de modo que ninguna clave foránea se inserte antes que su fila padre.
 */
final class EsquemaRespaldo {
    private EsquemaRespaldo() { }

    static final class Tabla {
        final String nombre;
        final String columnas;
        final int[] tipos;
        final String orden;
        final String secuencia;

        Tabla(String nombre, String columnas, int... tipos) {
            this.nombre = nombre;
            this.columnas = columnas;
            this.tipos = tipos;
            boolean puente = "cuentas_personas".equals(nombre);
            this.orden = puente ? "cuenta_id, persona_id" : "id";
            this.secuencia = puente ? null : "seq_" + nombre;
            if (columnas.split(",").length != tipos.length) {
                throw new IllegalArgumentException("Tipos incompletos en " + nombre);
            }
        }

        String seleccionar() {
            return "SELECT " + columnas + " FROM " + nombre + " ORDER BY " + orden;
        }

        String insertar() {
            StringBuilder valores = new StringBuilder();
            for (int indice = 0; indice < tipos.length; indice++) {
                if (indice > 0) { valores.append(", "); }
                valores.append("?");
            }
            return "INSERT INTO " + nombre + " (" + columnas + ") VALUES (" + valores + ")";
        }
    }

    static final List<Tabla> TABLAS = Collections.unmodifiableList(Arrays.asList(
        new Tabla("sedes", "id, nombre, direccion, departamento",
            Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR),
        new Tabla("estatus", "id, nombre, descripcion",
            Types.INTEGER, Types.VARCHAR, Types.VARCHAR),
        new Tabla("tipos_persona", "id, nombre", Types.INTEGER, Types.VARCHAR),
        new Tabla("tipos_cuenta", "id, nombre", Types.INTEGER, Types.VARCHAR),
        new Tabla("tipos_prestamo", "id, nombre", Types.INTEGER, Types.VARCHAR),
        new Tabla("categorias_producto", "id, nombre", Types.INTEGER, Types.VARCHAR),
        new Tabla("personas", "id, nombre, apellido, dni, direccion, telefono, tipo_persona_id, estatus_id, sede_id, fecha_registro",
            Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
            Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.TIMESTAMP),
        new Tabla("productos", "id, nombre, descripcion, categoria_id, precio, stock",
            Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.NUMERIC, Types.INTEGER),
        new Tabla("cuentas", "id, numero, tipo_cuenta_id, estatus_id, sede_id, fecha_apertura",
            Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.TIMESTAMP),
        new Tabla("cuentas_personas", "cuenta_id, persona_id, titular",
            Types.INTEGER, Types.INTEGER, Types.SMALLINT),
        new Tabla("saldos", "id, cuenta_id, monto, fecha",
            Types.INTEGER, Types.INTEGER, Types.NUMERIC, Types.TIMESTAMP),
        new Tabla("prestamos", "id, persona_id, tipo_prestamo_id, estatus_id, monto, tasa_interes, fecha, fecha_vencimiento",
            Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.NUMERIC,
            Types.NUMERIC, Types.TIMESTAMP, Types.TIMESTAMP),
        new Tabla("detalles_prestamo", "id, prestamo_id, producto_id, cantidad, precio_unitario",
            Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.NUMERIC),
        // liquidado es propio del API (migracion 08); no existe en el proyecto de escritorio.
        new Tabla("albaranes", "id, numero_talonario, persona_id, sede_id, fecha, liquidado",
            Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.TIMESTAMP, Types.SMALLINT),
        new Tabla("detalles_albaran", "id, albaran_id, producto_id, cantidad_aceptada, cantidad_rechazada, precio_unitario",
            Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.NUMERIC, Types.NUMERIC, Types.NUMERIC)
    ));
}
