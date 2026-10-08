package com.cooperativa.rest;

import com.cooperativa.controller.CuentaController;
import com.cooperativa.controller.NegocioException;
import com.cooperativa.model.Cuenta;
import com.cooperativa.model.CuentaPersona;
import com.cooperativa.model.Saldo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@CrossOrigin(origins = "*") // Permite el consumo desde el frontend web
public class CuentaRestController {

    private final CuentaController cuentaController = new CuentaController();

    /** DTO para agrupar los datos necesarios al abrir una cuenta */
    public static class AperturaCuentaRequest {
        private Cuenta cuenta;
        private List<CuentaPersona> personas;
        private BigDecimal saldoInicial;

        public Cuenta getCuenta() { return cuenta; }
        public void setCuenta(Cuenta cuenta) { this.cuenta = cuenta; }
        public List<CuentaPersona> getPersonas() { return personas; }
        public void setPersonas(List<CuentaPersona> personas) { this.personas = personas; }
        public BigDecimal getSaldoInicial() { return saldoInicial; }
        public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }
    }

    /** 1. Abrir una nueva cuenta (Transaccional) */
    @PostMapping
    public ResponseEntity<?> abrirCuenta(@RequestBody AperturaCuentaRequest request) {
        try {
            Cuenta nuevaCuenta = cuentaController.abrirCuenta(
                request.getCuenta(), 
                request.getPersonas(), 
                request.getSaldoInicial()
            );
            return ResponseEntity.ok(nuevaCuenta);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 2. Consultar saldo actual de una cuenta */
    @GetMapping("/{id}/saldo")
    public ResponseEntity<?> consultarSaldo(@PathVariable int id) {
        try {
            BigDecimal saldo = cuentaController.consultarSaldo(id);
            return ResponseEntity.ok(saldo);
        } catch (NegocioException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    /** 3. Consultar historial de saldos de una cuenta */
    @GetMapping("/{id}/historial-saldos")
    public ResponseEntity<?> historialSaldos(@PathVariable int id) {
        try {
            List<Saldo> historial = cuentaController.historialSaldos(id);
            return ResponseEntity.ok(historial);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 4. Listar cuentas asociadas a una persona */
    @GetMapping("/persona/{personaId}")
    public ResponseEntity<?> cuentasDePersona(@PathVariable int personaId) {
        try {
            List<Cuenta> cuentas = cuentaController.cuentasDePersona(personaId);
            return ResponseEntity.ok(cuentas);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 5. Listar personas asociadas a una cuenta */
    @GetMapping("/{id}/personas")
    public ResponseEntity<?> personasDeCuenta(@PathVariable int id) {
        try {
            List<CuentaPersona> personas = cuentaController.personasDeCuenta(id);
            return ResponseEntity.ok(personas);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 6. Realizar un depósito */
    @PatchMapping("/{id}/depositar")
    public ResponseEntity<?> depositar(@PathVariable int id, @RequestParam BigDecimal monto) {
        try {
            BigDecimal nuevoSaldo = cuentaController.depositar(id, monto);
            return ResponseEntity.ok("Depósito exitoso. Nuevo saldo: " + nuevoSaldo);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 7. Realizar un retiro */
    @PatchMapping("/{id}/retirar")
    public ResponseEntity<?> retirar(@PathVariable int id, @RequestParam BigDecimal monto) {
        try {
            BigDecimal nuevoSaldo = cuentaController.retirar(id, monto);
            return ResponseEntity.ok("Retiro exitoso. Nuevo saldo: " + nuevoSaldo);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 8. Agregar una persona a una cuenta existente */
    @PostMapping("/{id}/personas")
    public ResponseEntity<?> agregarPersona(@PathVariable int id, @RequestParam int personaId, @RequestParam boolean titular) {
        try {
            cuentaController.agregarPersona(id, personaId, titular);
            return ResponseEntity.ok("Persona agregada exitosamente a la cuenta.");
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 9. Quitar una persona de una cuenta */
    @DeleteMapping("/{cuentaId}/personas/{personaId}")
    public ResponseEntity<?> quitarPersona(@PathVariable int cuentaId, @PathVariable int personaId) {
        try {
            cuentaController.quitarPersona(cuentaId, personaId);
            return ResponseEntity.ok("Persona removida de la cuenta exitosamente.");
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}