
package mx.edu.backendacademico.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mx.edu.backendacademico.config.SecurityConfig;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.service.AlumnoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlumnoController.class)
@Import(SecurityConfig.class)
class SeguridadTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AlumnoService service;

    private static final String DATOS = """
        {"matricula":"S1","nombre":"Ada","correo":"ada@u.mx"}
        """;

    private static final Alumno ALUMNO_11 =
            new Alumno(11L, "U10A001", "Ada Byron",
                    "ada@u.mx", EstatusAlumno.ACTIVO);

    @Test
    void sinCredencialesRecibe401() throws Exception {
        mvc.perform(get("/api/v1/alumnos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void alumnoNoPuedeCrearAunqueTengaCsrf() throws Exception {
        mvc.perform(post("/api/v1/alumnos")
                        .with(httpBasic("alumno", "alumno123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS))
                .andExpect(status().isForbidden());
    }

    @Test
    void docenteSinCsrfRecibe403() throws Exception {
        mvc.perform(post("/api/v1/alumnos")
                        .with(httpBasic("docente", "docente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS))
                .andExpect(status().isForbidden());
    }

    @Test
    void docenteConCsrfPuedeCrear() throws Exception {
        when(service.crear(anyString(), anyString(), anyString()))
                .thenReturn(new Alumno(1L, "S1", "Ada",
                        "ada@u.mx", EstatusAlumno.ACTIVO));

        mvc.perform(post("/api/v1/alumnos")
                        .with(httpBasic("docente", "docente123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS))
                .andExpect(status().isCreated());
    }

    // CASO 1: el propietario consulta su propio expediente.
    @Test
    void propietarioPuedeConsultarMiExpediente() throws Exception {
        when(service.buscarMiExpediente("alumno"))
                .thenReturn(ALUMNO_11);

        mvc.perform(get("/api/v1/alumnos/mi-expediente")
                        .with(httpBasic("alumno", "alumno123")))
                .andExpect(status().isOk());
    }

    // CASO 2: otro alumno intenta consultar un expediente ajeno.
    @Test
    void alumnoAjenoNoPuedeConsultarOtroExpediente() throws Exception {
        when(service.perteneceAUsuario(11L, "alumno2"))
                .thenReturn(false);

        mvc.perform(get("/api/v1/alumnos/11")
                        .with(httpBasic("alumno2", "alumno2123")))
                .andExpect(status().isForbidden());
    }

    // CASO 3: el administrador puede consultar cualquier expediente.
    @Test
    void administradorPuedeConsultarExpedienteAjeno() throws Exception {
        when(service.buscar(11L))
                .thenReturn(ALUMNO_11);

        mvc.perform(get("/api/v1/alumnos/11")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }
}
