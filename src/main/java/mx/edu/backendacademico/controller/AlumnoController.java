
package mx.edu.backendacademico.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;

import mx.edu.backendacademico.dto.ActualizarAlumnoRequest;
import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.dto.CrearAlumnoRequest;
import mx.edu.backendacademico.service.AlumnoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alumnos")
public class AlumnoController {

    private final AlumnoService service;

    public AlumnoController(AlumnoService service) {
        this.service = service;
    }

    @GetMapping
    public List<AlumnoResponse> listar() {
        return service.listar()
                .stream()
                .map(AlumnoMapper::toResponse)
                .toList();
    }

    // El principal proporciona la identidad autenticada.
    @GetMapping("/mi-expediente")
    public AlumnoResponse miExpediente(Principal principal) {
        return AlumnoMapper.toResponse(
                service.buscarMiExpediente(principal.getName()));
    }

    // Los alumnos solo pueden consultar expedientes propios.
    // El administrador puede consultar cualquier expediente.
    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> buscar(
            @PathVariable Long id,
            Authentication authentication) {

        boolean esAdministrador = authentication.getAuthorities()
                .stream()
                .anyMatch(autoridad ->
                        autoridad.getAuthority().equals("ROLE_ADMIN"));

        boolean esPropietario = service.perteneceAUsuario(
                id, authentication.getName());

        if (!esAdministrador && !esPropietario) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                AlumnoMapper.toResponse(service.buscar(id)));
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> crear(
            @RequestBody @Valid CrearAlumnoRequest request) {

        var creado = service.crear(
                request.matricula(),
                request.nombre(),
                request.correo());

        return ResponseEntity
                .created(URI.create("/api/v1/alumnos/" + creado.id()))
                .body(AlumnoMapper.toResponse(creado));
    }

    @PutMapping("/{id}")
    public AlumnoResponse actualizar(
            @PathVariable Long id,
            @RequestBody @Valid ActualizarAlumnoRequest request) {

        return AlumnoMapper.toResponse(
                service.actualizar(id, request.nombre(), request.correo()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
