package com.cooperativa.model;

import java.util.Date;

public class Persona {
    private int id;
    private String nombre;
    private String apellido;
    private String dni;
    private String direccion;
    private String telefono;
    private int tipoPersonaId;
    private int estatusId;
    private int sedeId;
    private Date fechaRegistro;

    public Persona() {}

    public Persona(int id, String nombre, String apellido, String dni,
                   int tipoPersonaId, int estatusId, int sedeId) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.tipoPersonaId = tipoPersonaId;
        this.estatusId = estatusId;
        this.sedeId = sedeId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public int getTipoPersonaId() { return tipoPersonaId; }
    public void setTipoPersonaId(int tipoPersonaId) { this.tipoPersonaId = tipoPersonaId; }

    public int getEstatusId() { return estatusId; }
    public void setEstatusId(int estatusId) { this.estatusId = estatusId; }

    public int getSedeId() { return sedeId; }
    public void setSedeId(int sedeId) { this.sedeId = sedeId; }

    public Date getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Date fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    @Override
    public String toString() { return nombre + " " + apellido; }
}