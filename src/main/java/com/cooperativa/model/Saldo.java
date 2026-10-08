package com.cooperativa.model;

import java.math.BigDecimal;
import java.util.Date;

/** Registro de saldos de una cuenta (cada fila es el saldo en una fecha). */
public class Saldo {
    private int id;
    private int cuentaId;
    private BigDecimal monto;
    private Date fecha;

    public Saldo() {}

    public Saldo(int id, int cuentaId, BigDecimal monto, Date fecha) {
        this.id = id;
        this.cuentaId = cuentaId;
        this.monto = monto;
        this.fecha = fecha;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCuentaId() { return cuentaId; }
    public void setCuentaId(int cuentaId) { this.cuentaId = cuentaId; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
}