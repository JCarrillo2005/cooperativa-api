package com.cooperativa.model;

/** Tabla puente: una cuenta puede pertenecer a varias personas. */
public class CuentaPersona {
    private int cuentaId;
    private int personaId;
    private boolean titular;

    public CuentaPersona() {}

    public CuentaPersona(int cuentaId, int personaId, boolean titular) {
        this.cuentaId = cuentaId;
        this.personaId = personaId;
        this.titular = titular;
    }

    public int getCuentaId() { return cuentaId; }
    public void setCuentaId(int cuentaId) { this.cuentaId = cuentaId; }

    public int getPersonaId() { return personaId; }
    public void setPersonaId(int personaId) { this.personaId = personaId; }

    public boolean isTitular() { return titular; }
    public void setTitular(boolean titular) { this.titular = titular; }
}