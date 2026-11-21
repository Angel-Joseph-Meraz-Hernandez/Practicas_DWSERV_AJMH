package mx.edu.backendacademico.controller;

import mx.edu.backendacademico.config.SecurityConfig;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.service.AlumnoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
@WebMvcTest(ExpedienteController.class)
@Import(SecurityConfig.class)
class ExpedienteControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private AlumnoService service;
    private Alumno alumnoAda() {
        return new Alumno(4L, "A001", "Ada Byron", "ada.byron@universidad.mx",
                EstatusAlumno.ACTIVO);
    }
    @Test
    void propietarioPuedeConsultarSuExpediente() throws Exception {
        when(service.buscar(4L)).thenReturn(alumnoAda());
        mvc.perform(get("/api/v1/alumnos/4/expediente")
                        .with(user("ada.byron@universidad.mx").roles("ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ada Byron"));
    }
    @Test
    void usuarioAjenoNoPuedeConsultarExpediente() throws Exception {
        when(service.buscar(4L)).thenReturn(alumnoAda());
        mvc.perform(get("/api/v1/alumnos/4/expediente")
                        .with(user("otro@universidad.mx").roles("ALUMNO")))
                .andExpect(status().isForbidden());
    }
    @Test
    void administradorPuedeConsultarExpedienteAjeno() throws Exception {
        when(service.buscar(4L)).thenReturn(alumnoAda());
        mvc.perform(get("/api/v1/alumnos/4/expediente")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ada Byron"));
    }
}
