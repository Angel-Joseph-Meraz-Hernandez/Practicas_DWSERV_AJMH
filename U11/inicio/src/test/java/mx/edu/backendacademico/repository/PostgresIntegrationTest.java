package mx.edu.backendacademico.repository;

import static org.junit.jupiter.api.Assertions.*;
import mx.edu.backendacademico.domain.*;
import mx.edu.backendacademico.exception.OperacionAcademicaException;
import mx.edu.backendacademico.repository.jpa.*;
import mx.edu.backendacademico.service.InscripcionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc(addFilters = false)
class PostgresIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Un puerto efímero impide depender de una base compartida de tu equipo.
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired AlumnoSpringDataRepository alumnos;
    @Autowired MateriaRepository materias;
    @Autowired InscripcionRepository inscripciones;
    @Autowired InscripcionService servicio;
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test void noSobrepasaElCupoNiRepiteUnaInscripcion() {
        var a = alumnos.saveAndFlush(new AlumnoEntity(new Alumno(null, "P1", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO)));
        var b = alumnos.saveAndFlush(new AlumnoEntity(new Alumno(null, "P2", "Emmy", "emmy@u.mx", EstatusAlumno.ACTIVO)));
        var materia = materias.saveAndFlush(new MateriaEntity("P101", "Pruebas", 4, 1));
        servicio.inscribir(a.getId(), materia.getId());
        var repetida = assertThrows(OperacionAcademicaException.class, () -> servicio.inscribir(a.getId(), materia.getId()));
        assertEquals("INSCRIPCION_DUPLICADA", repetida.getCodigo());
        var llena = assertThrows(OperacionAcademicaException.class, () -> servicio.inscribir(b.getId(), materia.getId()));
        assertEquals("SIN_CUPO", llena.getCodigo());
        // Las llamadas anteriores pasaron por el proxy real y terminaron su transacción.
        assertEquals(1, inscripciones.countByMateriaId(materia.getId()));
    }

    @Test
    void solicitudesSimultaneasConLaMismaClaveCreanUnSoloAlumno() throws Exception {
        String clave = "prueba-concurrente-" + System.nanoTime();
        String matricula = "IDEM" + System.nanoTime();
        String cuerpo = """
            {"matricula": "%s",
                "nombre": "Alumno Idempotente",
                "correo": "idem-%s@u.mx"}
            """.formatted(matricula, System.nanoTime());
        var inicio = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var solicitud1 = ejecutor.submit(() -> {inicio.await();
                return enviarSolicitud(clave, cuerpo);});
            var solicitud2 = ejecutor.submit(() -> {inicio.await();
                return enviarSolicitud(clave, cuerpo);});
            inicio.countDown();
            MvcResult respuesta1 = solicitud1.get();
            MvcResult respuesta2 = solicitud2.get();
            assertEquals(201, respuesta1.getResponse().getStatus());
            assertEquals(201, respuesta2.getResponse().getStatus());
            assertEquals(respuesta1.getResponse().getHeader("Location"), respuesta2.getResponse().getHeader("Location"));
            assertEquals(respuesta1.getResponse().getContentAsString(), respuesta2.getResponse().getContentAsString());
            Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM alumnos WHERE matricula = ?", Integer.class, matricula);
            assertEquals(1, cantidad);
        }
    }
    @Test
    void mismaClaveConContenidoDiferenteDevuelveConflicto() throws Exception {
        String clave = "prueba-contenido-" + System.nanoTime();
        String matricula1 = "IDEM" + System.nanoTime();
        String matricula2 = "IDEM" + (System.nanoTime() + 1);
        String cuerpo1 = """
            {"matricula": "%s",
                "nombre": "Primer Alumno",
                "correo": "primero@u.mx"}
            """.formatted(matricula1);
        String cuerpo2 = """
            {"matricula": "%s",
                "nombre": "Segundo Alumno",
                "correo": "segundo@u.mx"}
            """.formatted(matricula2);
        MvcResult primera = enviarSolicitud(clave, cuerpo1);
        MvcResult segunda = enviarSolicitud(clave, cuerpo2);
        assertEquals(201, primera.getResponse().getStatus());
        assertEquals(409, segunda.getResponse().getStatus());
        Integer cantidadPrimero = jdbc.queryForObject("SELECT COUNT(*) FROM alumnos WHERE matricula = ?", Integer.class, matricula1);
        Integer cantidadSegundo = jdbc.queryForObject("SELECT COUNT(*) FROM alumnos WHERE matricula = ?", Integer.class, matricula2);
        assertEquals(1, cantidadPrimero);
        assertEquals(0, cantidadSegundo);
    }

    private MvcResult enviarSolicitud(String clave, String cuerpo) throws Exception {
        return mockMvc.perform(
                        post("/api/v1/alumnos")
                                .header("Idempotency-Key", clave)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andReturn();
    }
}
