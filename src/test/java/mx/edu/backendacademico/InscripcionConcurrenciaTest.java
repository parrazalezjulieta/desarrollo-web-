
package mx.edu.backendacademico;

import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.OperacionAcademicaException;
import mx.edu.backendacademico.repository.jpa.AlumnoEntity;
import mx.edu.backendacademico.repository.jpa.AlumnoSpringDataRepository;
import mx.edu.backendacademico.repository.jpa.MateriaEntity;
import mx.edu.backendacademico.repository.jpa.MateriaRepository;
import mx.edu.backendacademico.service.InscripcionService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InscripcionConcurrenciaTest {

    @Autowired
    private InscripcionService inscripcionService;

    @Autowired
    private AlumnoSpringDataRepository alumnos;

    @Autowired
    private MateriaRepository materias;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void dosAlumnosCompitenPorUnSoloCupo() throws Exception {

        String sufijo = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10);

        // Crear dos alumnos activos.
        Alumno alumno1 = new Alumno(
                null, "R8A" + sufijo,
                "Alumno Concurrencia 1",
                "r8a" + sufijo + "@test.com",
                EstatusAlumno.ACTIVO
        );

        Alumno alumno2 = new Alumno(
                null, "R8B" + sufijo,
                "Alumno Concurrencia 2",
                "r8b" + sufijo + "@test.com",
                EstatusAlumno.ACTIVO
        );

        AlumnoEntity guardado1 =
                alumnos.save(new AlumnoEntity(alumno1));

        AlumnoEntity guardado2 =
                alumnos.save(new AlumnoEntity(alumno2));

        // Crear una materia con un solo lugar.
        MateriaEntity materia = materias.save(
                new MateriaEntity(
                        "R8" + sufijo,
                        "Materia prueba concurrencia",
                        5,
                        1
                )
        );

        Long alumnoId1 = guardado1.getId();
        Long alumnoId2 = guardado2.getId();
        Long materiaId = materia.getId();

        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);

        ExecutorService ejecutor =
                Executors.newFixedThreadPool(2);

        try {
            Callable<Resultado> tarea1 = crearTarea(
                    alumnoId1, materiaId, listos, inicio
            );

            Callable<Resultado> tarea2 = crearTarea(
                    alumnoId2, materiaId, listos, inicio
            );

            Future<Resultado> futuro1 = ejecutor.submit(tarea1);
            Future<Resultado> futuro2 = ejecutor.submit(tarea2);

            // Esperar hasta que las dos tareas estén preparadas.
            assertTrue(
                    listos.await(10, TimeUnit.SECONDS),
                    "Las dos tareas debían estar preparadas"
            );

            // Permitir que ambas comiencen.
            inicio.countDown();

            Resultado resultado1 =
                    futuro1.get(20, TimeUnit.SECONDS);

            Resultado resultado2 =
                    futuro2.get(20, TimeUnit.SECONDS);

            int exitosas = 0;
            int sinCupo = 0;

            for (Resultado resultado :
                    new Resultado[]{resultado1, resultado2}) {

                if (resultado.inscripcionId() != null) {
                    exitosas++;
                }

                if ("SIN_CUPO".equals(resultado.codigo())) {
                    sinCupo++;
                }
            }

            assertEquals(1, exitosas,
                    "Solamente un alumno debe conseguir el cupo");

            assertEquals(1, sinCupo,
                    "El otro alumno debe recibir SIN_CUPO");

            assertEquals(1L, inscripcionesCount(materiaId),
                    "Debe existir una sola inscripción");

        } finally {
            inicio.countDown();
            ejecutor.shutdownNow();
            ejecutor.awaitTermination(10, TimeUnit.SECONDS);

            // Eliminar únicamente los registros creados por esta prueba.
            jdbc.update(
                    "DELETE FROM inscripciones WHERE materia_id = ?",
                    materiaId
            );

            materias.deleteById(materiaId);
            alumnos.deleteById(alumnoId1);
            alumnos.deleteById(alumnoId2);
        }
    }

    private Callable<Resultado> crearTarea(
            Long alumnoId,
            Long materiaId,
            CountDownLatch listos,
            CountDownLatch inicio
    ) {
        return () -> {
            listos.countDown();

            if (!inicio.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "No se liberó el inicio de las tareas"
                );
            }

            try {
                Long id = inscripcionService.inscribir(
                        alumnoId, materiaId
                );

                return new Resultado(id, null);

            } catch (OperacionAcademicaException ex) {
                return new Resultado(null, ex.getCodigo());
            }
        };
    }

    private long inscripcionesCount(Long materiaId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM inscripciones WHERE materia_id = ?",
                Long.class,
                materiaId
        );
    }

    private record Resultado(Long inscripcionId, String codigo) {
    }
}
