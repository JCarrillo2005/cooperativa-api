package com.cooperativa.rest;

import com.cooperativa.controller.AlbaranController;
import com.cooperativa.controller.NegocioException;
import com.cooperativa.model.Albaran;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/albaranes")
@CrossOrigin(origins = "*") // Permite el consumo desde el frontend web (HTML/CSS/JS)
public class AlbaranRestController {

    private final AlbaranController albaranController = new AlbaranController();

    /** 1. Listar todos los albaranes */
    @GetMapping
    public ResponseEntity<?> listarTodos() {
        try {
            List<Albaran> lista = albaranController.listarTodos();
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 2. Obtener albarán por ID (incluye detalles y líneas de productos) */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable int id) {
        try {
            Albaran a = albaranController.obtener(id);
            return ResponseEntity.ok(a);
        } catch (NegocioException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    /** 3. Buscar albarán por número de talonario */
    @GetMapping("/talonario/{talonario}")
    public ResponseEntity<?> buscarPorTalonario(@PathVariable String talonario) {
        try {
            Albaran a = albaranController.buscarPorTalonario(talonario);
            if (a == null) {
                return ResponseEntity.status(404).body("No se encontró ningún albarán con el talonario: " + talonario);
            }
            return ResponseEntity.ok(a);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 4. Listar albaranes asociados a un socio específico */
    @GetMapping("/socio/{personaId}")
    public ResponseEntity<?> listarPorSocio(@PathVariable int personaId) {
        try {
            List<Albaran> lista = albaranController.listarPorSocio(personaId);
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 5. Registrar una nueva entrega de cosecha (Albarán con detalles) */
    @PostMapping
    public ResponseEntity<?> registrarEntrega(@RequestBody Albaran albaran) {
        try {
            Albaran nuevo = albaranController.registrarEntrega(albaran);
            return ResponseEntity.ok(nuevo);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 6. Liquidar albarán acreditando el pago como depósito a una cuenta del socio */
    @PatchMapping("/{id}/liquidar")
    public ResponseEntity<?> liquidarAlbaranEnCuenta(@PathVariable int id, @RequestParam int cuentaId) {
        try {
            BigDecimal totalLiquidado = albaranController.liquidarAlbaranEnCuenta(id, cuentaId);
            return ResponseEntity.ok("Albarán liquidado con éxito. Se abonaron Q" + totalLiquidado + " a la cuenta.");
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}