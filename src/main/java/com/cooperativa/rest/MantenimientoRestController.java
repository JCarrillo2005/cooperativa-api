package com.cooperativa.rest;

import com.cooperativa.service.RespaldoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operaciones de mantenimiento administrativo (Fase IV).
 *
 * El respaldo en caliente copia Oracle a PostgreSQL sin detener la cooperativa. La
 * recuperación (PostgreSQL -> Oracle) NO se expone aquí a propósito: exige la API detenida
 * y un esquema Oracle nuevo y vacío; se ejecuta por línea de comandos con
 * {@code BackupEnCaliente recuperar}.
 */
@RestController
@RequestMapping("/api/mantenimiento/respaldo")
@CrossOrigin(origins = "*") // Permite el consumo desde el frontend web (HTML/CSS/JS)
public class MantenimientoRestController {

    private final RespaldoService respaldoService = new RespaldoService();

    /** Estado actual: si hay un respaldo en curso, el último resultado y los conteos por tabla. */
    @GetMapping
    public ResponseEntity<RespaldoService.Estado> estado() {
        return ResponseEntity.ok(respaldoService.estado());
    }

    /** Dispara el respaldo en segundo plano. Responde 202 si arrancó y 409 si ya había uno en curso. */
    @PostMapping
    public ResponseEntity<RespaldoService.Estado> respaldar() {
        boolean iniciado = respaldoService.iniciar();
        if (!iniciado) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(respaldoService.estado());
        }
        return ResponseEntity.accepted().body(respaldoService.estado());
    }
}
