package com.cooperativa.model;

public class CategoriaProducto {
    private int id;
    private String nombre;        // INSUMO_SOCIOS (semillas, abonos...), PRODUCTO_EXPORTACION (hortalizas...)

    public CategoriaProducto() {}

    public CategoriaProducto(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override
    public String toString() { return nombre; }
}