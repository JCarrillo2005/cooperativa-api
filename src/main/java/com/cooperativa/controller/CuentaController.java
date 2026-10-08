package com.cooperativa.controller;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.dao.CuentaDAO;
import com.cooperativa.dao.CuentaPersonaDAO;
import com.cooperativa.dao.SaldoDAO;
import com.cooperativa.dao.SedeDAO;
import com.cooperativa.dao.TipoCuentaDAO;
import com.cooperativa.model.Cuenta;
import com.cooperativa.model.CuentaPersona;
import com.cooperativa.model.Saldo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CuentaController {

    private final CuentaDAO cuentaDAO = new CuentaDAO();
    private final CuentaPersonaDAO cuentaPersonaDAO = new CuentaPersonaDAO();
    private final SaldoDAO saldoDAO = new SaldoDAO();
    private final TipoCuentaDAO tipoCuentaDAO = new TipoCuentaDAO();
    private final SedeDAO sedeDAO = new SedeDAO();
    private final PersonaController personaController = new PersonaController();

    // ---------- Abrir cuenta ----------

    /**
     * Abre una cuenta con sus personas y su saldo inicial, todo en una transacción.
     * Reglas: al menos una persona, al menos un titular, todas activas y sin repetir.
     * Si la cuenta no trae número ni estatus, se generan.
     */
    public Cuenta abrirCuenta(Cuenta cuenta, List<CuentaPersona> personas, BigDecimal saldoInicial)
            throws NegocioException {
        if (cuenta == null) {
            throw new NegocioException("No se recibieron los datos de la cuenta.");
        }
        if (personas == null || personas.isEmpty()) {
            throw new NegocioException("La cuenta debe tener al menos una persona.");
        }

        boolean hayTitular = false;
        Set<Integer> vistas = new HashSet<Integer>();
        for (CuentaPersona cp : personas) {
            if (!vistas.add(cp.getPersonaId())) {
                throw new NegocioException("Una persona no puede aparecer dos veces en la misma cuenta.");
            }
            personaController.obtenerActiva(cp.getPersonaId());
            if (cp.isTitular()) {
                hayTitular = true;
            }
        }
        if (!hayTitular) {
            throw new NegocioException("La cuenta debe tener al menos un titular.");
        }

        if (saldoInicial == null) {
            saldoInicial = BigDecimal.ZERO;
        }
        if (saldoInicial.signum() < 0) {
            throw new NegocioException("El saldo inicial no puede ser negativo.");
        }
        saldoInicial = saldoInicial.setScale(2, RoundingMode.HALF_UP);

        try {
            if (tipoCuentaDAO.obtenerPorId(cuenta.getTipoCuentaId()) == null) {
                throw new NegocioException("El tipo de cuenta no existe.");
            }
            if (sedeDAO.obtenerPorId(cuenta.getSedeId()) == null) {
                throw new NegocioException("La sede no existe.");
            }
            if (cuenta.getEstatusId() == 0) {
                cuenta.setEstatusId(Catalogos.estatusId(Catalogos.ESTATUS_ACTIVO));
            }

            if (cuenta.getNumero() == null || cuenta.getNumero().trim().isEmpty()) {
                cuenta.setNumero(generarNumero());
            } else {
                cuenta.setNumero(cuenta.getNumero().trim());
                if (cuentaDAO.obtenerPorNumero(cuenta.getNumero()) != null) {
                    throw new NegocioException("Ya existe una cuenta con el número " + cuenta.getNumero() + ".");
                }
            }

            try (Connection conn = ConexionOracle.getConexion()) {
                conn.setAutoCommit(false);
                try {
                    cuentaDAO.guardar(cuenta, conn);
                    for (CuentaPersona cp : personas) {
                        cp.setCuentaId(cuenta.getId());
                        cuentaPersonaDAO.agregar(cp, conn);
                    }
                    saldoDAO.registrar(new Saldo(0, cuenta.getId(), saldoInicial, null), conn);
                    conn.commit();
                } catch (SQLException | RuntimeException e) {
                    conn.rollback();
                    throw e;
                }
            }
            return cuenta;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo abrir la cuenta", e);
        }
    }

    private String generarNumero() throws SQLException {
        String numero;
        do {
            numero = "CTA-" + (System.currentTimeMillis() % 100000000L);
        } while (cuentaDAO.obtenerPorNumero(numero) != null);
        return numero;
    }

    // ---------- Movimientos ----------

    /** Devuelve el saldo nuevo. */
    public BigDecimal depositar(int cuentaId, BigDecimal monto) throws NegocioException {
        return movimiento(cuentaId, monto, false);
    }

    /** Devuelve el saldo nuevo. Falla si no hay saldo suficiente. */
    public BigDecimal retirar(int cuentaId, BigDecimal monto) throws NegocioException {
        return movimiento(cuentaId, monto, true);
    }

    private BigDecimal movimiento(int cuentaId, BigDecimal monto, boolean esRetiro) throws NegocioException {
        if (monto == null || monto.signum() <= 0) {
            throw new NegocioException("El monto debe ser mayor que cero.");
        }
        monto = monto.setScale(2, RoundingMode.HALF_UP);
        String operacion = esRetiro ? "el retiro" : "el depósito";

        try {
            Cuenta cuenta = cuentaDAO.obtenerPorId(cuentaId);
            if (cuenta == null) {
                throw new NegocioException("La cuenta no existe.");
            }
            if (cuenta.getEstatusId() != Catalogos.estatusId(Catalogos.ESTATUS_ACTIVO)) {
                throw new NegocioException("La cuenta " + cuenta + " no está activa.");
            }

            try (Connection conn = ConexionOracle.getConexion()) {
                conn.setAutoCommit(false);
                try {
                    cuentaDAO.bloquear(cuentaId, conn);
                    Saldo actual = saldoDAO.obtenerActual(cuentaId, conn);
                    BigDecimal base = (actual == null) ? BigDecimal.ZERO : actual.getMonto();
                    BigDecimal nuevo = esRetiro ? base.subtract(monto) : base.add(monto);

                    if (nuevo.signum() < 0) {
                        throw new NegocioException("Saldo insuficiente. Saldo actual: " + base + ".");
                    }
                    saldoDAO.registrar(new Saldo(0, cuentaId, nuevo, null), conn);
                    conn.commit();
                    return nuevo;
                } catch (SQLException | RuntimeException | NegocioException e) {
                    conn.rollback();
                    throw e;
                }
            }
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo realizar " + operacion, e);
        }
    }

    // ---------- Consultas ----------

    public BigDecimal consultarSaldo(int cuentaId) throws NegocioException {
        try {
            if (cuentaDAO.obtenerPorId(cuentaId) == null) {
                throw new NegocioException("La cuenta no existe.");
            }
            Saldo actual = saldoDAO.obtenerActual(cuentaId);
            return (actual == null) ? BigDecimal.ZERO : actual.getMonto();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo consultar el saldo", e);
        }
    }

    public List<Saldo> historialSaldos(int cuentaId) throws NegocioException {
        try {
            return saldoDAO.obtenerHistorial(cuentaId);
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo consultar el historial de saldos", e);
        }
    }

    public List<Cuenta> cuentasDePersona(int personaId) throws NegocioException {
        try {
            return cuentaDAO.obtenerPorPersona(personaId);
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo consultar las cuentas de la persona", e);
        }
    }

    public List<CuentaPersona> personasDeCuenta(int cuentaId) throws NegocioException {
        try {
            return cuentaPersonaDAO.obtenerPorCuenta(cuentaId);
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo consultar las personas de la cuenta", e);
        }
    }

    // ---------- Personas de la cuenta ----------

    public void agregarPersona(int cuentaId, int personaId, boolean titular) throws NegocioException {
        personaController.obtenerActiva(personaId);
        try {
            if (cuentaDAO.obtenerPorId(cuentaId) == null) {
                throw new NegocioException("La cuenta no existe.");
            }
            for (CuentaPersona cp : cuentaPersonaDAO.obtenerPorCuenta(cuentaId)) {
                if (cp.getPersonaId() == personaId) {
                    throw new NegocioException("La persona ya pertenece a esta cuenta.");
                }
            }
            cuentaPersonaDAO.agregar(new CuentaPersona(cuentaId, personaId, titular));
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo agregar la persona a la cuenta", e);
        }
    }

    /** No permite dejar la cuenta sin titular. */
    public void quitarPersona(int cuentaId, int personaId) throws NegocioException {
        try {
            CuentaPersona objetivo = null;
            boolean quedaTitular = false;
            for (CuentaPersona cp : cuentaPersonaDAO.obtenerPorCuenta(cuentaId)) {
                if (cp.getPersonaId() == personaId) {
                    objetivo = cp;
                } else if (cp.isTitular()) {
                    quedaTitular = true;
                }
            }
            if (objetivo == null) {
                throw new NegocioException("La persona no pertenece a esta cuenta.");
            }
            if (!quedaTitular) {
                throw new NegocioException("No se puede quitar: la cuenta debe conservar al menos un titular.");
            }
            cuentaPersonaDAO.quitar(cuentaId, personaId);
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo quitar la persona de la cuenta", e);
        }
    }
}