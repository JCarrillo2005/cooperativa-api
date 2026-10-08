package com.cooperativa.model;

public class Estatus {
    private int id;
    private String nombre;        // ACTIVO, INACTIVO, PAGADO, VENCIDO, etc.
    private String descripcion;

    public Estatus() {}

    public Estatus(int id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() { return nombre; }
}