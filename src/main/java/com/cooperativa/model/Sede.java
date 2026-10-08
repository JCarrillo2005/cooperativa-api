package com.cooperativa.model;

public class Sede {
    private int id;
    private String nombre;
    private String direccion;
    private String departamento;

    public Sede() {}

    public Sede(int id, String nombre, String direccion, String departamento) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.departamento = departamento;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    @Override
    public String toString() { return nombre; }
}