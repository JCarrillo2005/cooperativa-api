package com.cooperativa.rest;

import com.cooperativa.controller.NegocioException;
import com.cooperativa.controller.PersonaController;
import com.cooperativa.model.Persona;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
@CrossOrigin(origins = "*") // Permite llamadas desde cualquier frontend web (HTML/CSS/JS)
public class PersonaRestController {

    private final PersonaController personaController = new PersonaController();

    /** 1. Listar todas las personas */
    @GetMapping
    public ResponseEntity<?> listarTodas() {
        try {
            List<Persona> lista = personaController.listarTodas();
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 2. Listar únicamente socios */
    @GetMapping("/socios")
    public ResponseEntity<?> listarSocios() {
        try {
            List<Persona> socios = personaController.listarSocios();
            return ResponseEntity.ok(socios);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 3. Obtener persona por ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable int id) {
        try {
            Persona p = personaController.obtenerPorId(id);
            return ResponseEntity.ok(p);
        } catch (NegocioException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    /** 4. Buscar persona por DNI */
    @GetMapping("/dni/{dni}")
    public ResponseEntity<?> buscarPorDni(@PathVariable String dni) {
        try {
            Persona p = personaController.buscarPorDni(dni);
            if (p == null) {
                return ResponseEntity.status(404).body("No se encontró ninguna persona con el DNI: " + dni);
            }
            return ResponseEntity.ok(p);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 5. Registrar una nueva persona */
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Persona persona) {
        try {
            Persona nueva = personaController.registrar(persona);
            return ResponseEntity.ok(nueva);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 6. Actualizar una persona existente */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @RequestBody Persona persona) {
        try {
            persona.setId(id);
            Persona actualizada = personaController.actualizar(persona);
            return ResponseEntity.ok(actualizada);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 7. Cambiar estatus (Activar / Desactivar) */
    @PatchMapping("/{id}/estatus")
    public ResponseEntity<?> cambiarEstatus(@PathVariable int id, @RequestParam String estatus) {
        try {
            personaController.cambiarEstatus(id, estatus);
            return ResponseEntity.ok("Estatus actualizado correctamente a: " + estatus);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 8. Eliminar o dar de baja */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        try {
            personaController.eliminar(id);
            return ResponseEntity.ok("Persona eliminada con éxito.");
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}