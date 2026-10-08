package com.cooperativa.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Prestamo {
    private int id;
    private int personaId;
    private int tipoPrestamoId;
    private int estatusId;
    private BigDecimal monto;
    private BigDecimal tasaInteres;
    private Date fecha;
    private Date fechaVencimiento;
    private List<DetallePrestamo> detalles = new ArrayList<DetallePrestamo>();

    public Prestamo() {}

    public Prestamo(int id, int personaId, int tipoPrestamoId, int estatusId,
                    BigDecimal monto, BigDecimal tasaInteres, Date fecha, Date fechaVencimiento) {
        this.id = id;
        this.personaId = personaId;
        this.tipoPrestamoId = tipoPrestamoId;
        this.estatusId = estatusId;
        this.monto = monto;
        this.tasaInteres = tasaInteres;
        this.fecha = fecha;
        this.fechaVencimiento = fechaVencimiento;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPersonaId() { return personaId; }
    public void setPersonaId(int personaId) { this.personaId = personaId; }

    public int getTipoPrestamoId() { return tipoPrestamoId; }
    public void setTipoPrestamoId(int tipoPrestamoId) { this.tipoPrestamoId = tipoPrestamoId; }

    public int getEstatusId() { return estatusId; }
    public void setEstatusId(int estatusId) { this.estatusId = estatusId; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public BigDecimal getTasaInteres() { return tasaInteres; }
    public void setTasaInteres(BigDecimal tasaInteres) { this.tasaInteres = tasaInteres; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    public Date getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(Date fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    /** Solo se usa cuando el préstamo es de consumo de productos. */
    public List<DetallePrestamo> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePrestamo> detalles) { this.detalles = detalles; }

    public void agregarDetalle(DetallePrestamo d) { detalles.add(d); }
}