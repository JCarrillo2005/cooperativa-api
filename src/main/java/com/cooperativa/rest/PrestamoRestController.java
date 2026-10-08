package com.cooperativa.rest;

import com.cooperativa.controller.NegocioException;
import com.cooperativa.controller.PrestamoController;
import com.cooperativa.model.Prestamo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
@CrossOrigin(origins = "*") // Permite el consumo desde el frontend web (HTML/CSS/JS)
public class PrestamoRestController {

    private final PrestamoController prestamoController = new PrestamoController();

    /** 1. Listar todos los préstamos */
    @GetMapping
    public ResponseEntity<?> listarTodos() {
        try {
            List<Prestamo> lista = prestamoController.listarTodos();
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 2. Obtener préstamo por ID (incluye detalles/productos si es de consumo) */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable int id) {
        try {
            Prestamo p = prestamoController.obtener(id);
            return ResponseEntity.ok(p);
        } catch (NegocioException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    /** 3. Listar préstamos asociados a una persona */
    @GetMapping("/persona/{personaId}")
    public ResponseEntity<?> listarPorPersona(@PathVariable int personaId) {
        try {
            List<Prestamo> lista = prestamoController.listarPorPersona(personaId);
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 4. Registrar un nuevo préstamo (transaccional con stock y detalles) */
    @PostMapping
    public ResponseEntity<?> registrarPrestamo(@RequestBody Prestamo prestamo) {
        try {
            Prestamo nuevo = prestamoController.registrarPrestamo(prestamo);
            return ResponseEntity.ok(nuevo);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 5. Registrar un abono o liquidación al préstamo */
    @PatchMapping("/{id}/abono")
    public ResponseEntity<?> registrarAbono(@PathVariable int id, @RequestParam BigDecimal montoAbono) {
        try {
            BigDecimal saldoRestante = prestamoController.registrarAbono(id, montoAbono);
            return ResponseEntity.ok("Abono registrado con éxito. Saldo pendiente: " + saldoRestante);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 6. Cambiar el estatus del préstamo */
    @PatchMapping("/{id}/estatus")
    public ResponseEntity<?> cambiarEstatus(@PathVariable int id, @RequestParam String estatus) {
        try {
            prestamoController.cambiarEstatus(id, estatus);
            return ResponseEntity.ok("Estatus del préstamo actualizado a: " + estatus);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}