package com.cooperativa.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Albaran {
    private int id;
    private String numeroTalonario;
    private int personaId;        // socio que entrega
    private int sedeId;
    private Date fecha;
    private List<DetalleAlbaran> detalles = new ArrayList<DetalleAlbaran>();

    public Albaran() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumeroTalonario() { return numeroTalonario; }
    public void setNumeroTalonario(String numeroTalonario) { this.numeroTalonario = numeroTalonario; }

    public int getPersonaId() { return personaId; }
    public void setPersonaId(int personaId) { this.personaId = personaId; }

    public int getSedeId() { return sedeId; }
    public void setSedeId(int sedeId) { this.sedeId = sedeId; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    public List<DetalleAlbaran> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleAlbaran> detalles) { this.detalles = detalles; }

    public void agregarDetalle(DetalleAlbaran d) { detalles.add(d); }

    /** Total a pagar al socio: solo lo aceptado. */
    public BigDecimal calcularTotal() {
        BigDecimal suma = BigDecimal.ZERO;
        for (DetalleAlbaran d : detalles) {
            suma = suma.add(d.getSubtotal());
        }
        return suma;
    }
}