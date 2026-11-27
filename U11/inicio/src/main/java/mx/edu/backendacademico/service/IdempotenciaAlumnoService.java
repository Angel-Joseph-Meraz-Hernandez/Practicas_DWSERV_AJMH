package mx.edu.backendacademico.service;

import java.net.URI;
import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.dto.CrearAlumnoRequest;
import mx.edu.backendacademico.repository.IdempotenciaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class IdempotenciaAlumnoService {
    private final IdempotenciaRepository idempotenciaRepository;
    private final AlumnoService alumnoService;
    private final ObjectMapper objectMapper;
    public IdempotenciaAlumnoService(IdempotenciaRepository idempotenciaRepository, AlumnoService alumnoService,
            ObjectMapper objectMapper) {
        this.idempotenciaRepository = idempotenciaRepository;
        this.alumnoService = alumnoService;
        this.objectMapper = objectMapper;
    }
    public ResponseEntity<AlumnoResponse> crear(String clave, CrearAlumnoRequest request) {
        String huella = HuellaSolicitud.calcular(request.matricula(), request.nombre(), request.correo());
        boolean reservada = idempotenciaRepository.reservar(clave, huella);
        if (!reservada) {
            var anterior = idempotenciaRepository.buscar(clave).orElseThrow(() -> new IllegalStateException(
                            "No se encontró el registro de idempotencia"));
            if (!anterior.huella().equals(huella)) {return ResponseEntity.status(409).build();}
            try {
                return ResponseEntity.status(anterior.statusCode()).location(URI.create(anterior.location()))
                        .body(objectMapper.readValue(anterior.responseBody(), AlumnoResponse.class));
            } catch (JacksonException e) {
                throw new IllegalStateException("No se pudo recuperar la respuesta original", e);
            }
        }
        var creado = alumnoService.crear(request.matricula(), request.nombre(), request.correo());
        AlumnoResponse respuesta = AlumnoMapper.toResponse(creado);
        URI location = URI.create("/api/v1/alumnos/" + creado.id());
        ResponseEntity<AlumnoResponse> response = ResponseEntity.created(location).body(respuesta);
        try {String responseBody = objectMapper.writeValueAsString(respuesta);
            idempotenciaRepository.completar(clave, creado.id(), response.getStatusCode().value(),
                    location.toString(), responseBody);
        } catch (JacksonException e) {
            throw new IllegalStateException("No se pudo guardar la respuesta original", e);
        }
        return response;
    }
}