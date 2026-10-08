package com.cooperativa.model;

import java.util.Date;

public class Cuenta {
    private int id;
    private String numero;
    private int tipoCuentaId;
    private int estatusId;
    private int sedeId;
    private Date fechaApertura;

    public Cuenta() {}

    public Cuenta(int id, String numero, int tipoCuentaId, int estatusId,
                  int sedeId, Date fechaApertura) {
        this.id = id;
        this.numero = numero;
        this.tipoCuentaId = tipoCuentaId;
        this.estatusId = estatusId;
        this.sedeId = sedeId;
        this.fechaApertura = fechaApertura;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public int getTipoCuentaId() { return tipoCuentaId; }
    public void setTipoCuentaId(int tipoCuentaId) { this.tipoCuentaId = tipoCuentaId; }

    public int getEstatusId() { return estatusId; }
    public void setEstatusId(int estatusId) { this.estatusId = estatusId; }

    public int getSedeId() { return sedeId; }
    public void setSedeId(int sedeId) { this.sedeId = sedeId; }

    public Date getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(Date fechaApertura) { this.fechaApertura = fechaApertura; }

    @Override
    public String toString() { return numero; }
}