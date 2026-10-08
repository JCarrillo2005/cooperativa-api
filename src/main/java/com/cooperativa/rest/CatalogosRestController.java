package com.cooperativa.rest;

import com.cooperativa.controller.CatalogosController;
import com.cooperativa.controller.NegocioException;
import com.cooperativa.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@CrossOrigin(origins = "*") // Permite el consumo desde el frontend web (HTML/CSS/JS)
public class CatalogosRestController {

    private final CatalogosController catalogosController = new CatalogosController();

    /** 1. Listar todas las sedes */
    @GetMapping("/sedes")
    public ResponseEntity<?> listarSedes() {
        try {
            List<Sede> lista = catalogosController.listarSedes();
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 2. Listar estatus del sistema */
    @GetMapping("/estatus")
    public ResponseEntity<?> listarEstatus() {
        try {
            List<Estatus> lista = catalogosController.listarEstatus();
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 3. Listar tipos de persona (Ej. Socio, Empleado, etc.) */
    @GetMapping("/tipos-persona")
    public ResponseEntity<?> listarTiposPersona() {
        try {
            List<TipoPersona> lista = catalogosController.listarTiposPersona();
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 4. Listar tipos de cuenta bancaria */
    @GetMapping("/tipos-cuenta")
    public ResponseEntity<?> listarTiposCuenta() {
        try {
            List<TipoCuenta> lista = catalogosController.listarTiposCuenta();
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 5. Listar tipos de préstamo (Ej. Consumo, Efectivo, etc.) */
    @GetMapping("/tipos-prestamo")
    public ResponseEntity<?> listarTiposPrestamo() {
        try {
            List<TipoPrestamo> lista = catalogosController.listarTiposPrestamo();
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 6. Listar categorías de productos / cosechas */
    @GetMapping("/categorias-producto")
    public ResponseEntity<?> listarCategoriasProducto() {
        try {
            List<CategoriaProducto> lista = catalogosController.listarCategoriasProducto();
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}