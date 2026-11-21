package mx.edu.backendacademico.controller;

import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.service.AlumnoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/alumnos")
public class ExpedienteController {
    private final AlumnoService service;
    public ExpedienteController(AlumnoService service) {
        this.service = service;
    }
    @GetMapping("/{id}/expediente")
    public AlumnoResponse consultar(@PathVariable Long id, Authentication authentication) {
        var alumno = service.buscar(id);
        boolean administrador = authentication.getAuthorities().stream()
                .anyMatch(autoridad -> autoridad.getAuthority().equals("ROLE_ADMIN"));
        boolean propietario = alumno.correo()
                .equalsIgnoreCase(authentication.getName());
        if (!administrador && !propietario) {
            throw new AccessDeniedException("No tienes permiso para consultar este expediente");
        }
        return AlumnoMapper.toResponse(alumno);
    }
}