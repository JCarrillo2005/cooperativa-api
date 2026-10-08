package com.cooperativa.rest;

import com.cooperativa.controller.NegocioException;
import com.cooperativa.controller.ProductoController;
import com.cooperativa.model.Producto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*") // Permite el consumo desde el frontend web
public class ProductoRestController {

    private final ProductoController productoController = new ProductoController();

    /** 1. Listar todos los productos */
    @GetMapping
    public ResponseEntity<?> listarTodos() {
        try {
            List<Producto> lista = productoController.listarTodos();
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 2. Obtener producto por ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable int id) {
        try {
            Producto p = productoController.obtenerPorId(id);
            return ResponseEntity.ok(p);
        } catch (NegocioException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    /** 3. Listar productos por categoría */
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<?> listarPorCategoria(@PathVariable int categoriaId) {
        try {
            List<Producto> lista = productoController.listarPorCategoria(categoriaId);
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 4. Listar productos con stock bajo (ej. ?umbral=5) */
    @GetMapping("/stock-bajo")
    public ResponseEntity<?> listarStockBajo(@RequestParam(defaultValue = "5") int umbral) {
        try {
            List<Producto> lista = productoController.listarStockBajo(umbral);
            return ResponseEntity.ok(lista);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 5. Registrar un nuevo producto */
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Producto producto) {
        try {
            Producto nuevo = productoController.registrar(producto);
            return ResponseEntity.ok(nuevo);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 6. Actualizar un producto existente */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id, @RequestBody Producto producto) {
        try {
            producto.setId(id);
            Producto actualizado = productoController.actualizar(producto);
            return ResponseEntity.ok(actualizado);
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 7. Cambiar precio de un producto */
    @PatchMapping("/{id}/precio")
    public ResponseEntity<?> cambiarPrecio(@PathVariable int id, @RequestParam BigDecimal nuevoPrecio) {
        try {
            productoController.cambiarPrecio(id, nuevoPrecio);
            return ResponseEntity.ok("Precio actualizado correctamente.");
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 8. Agregar stock (entrada de mercancía) */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> agregarStock(@PathVariable int id, @RequestParam int cantidad) {
        try {
            productoController.agregarStock(id, cantidad);
            return ResponseEntity.ok("Stock incrementado correctamente.");
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 9. Eliminar un producto */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        try {
            productoController.eliminar(id);
            return ResponseEntity.ok("Producto eliminado con éxito.");
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}