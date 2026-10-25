
package mx.edu.backendacademico.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import org.junit.jupiter.api.Test;

class AlumnoRepositoryEnMemoriaTest {

    @Test
    void dosTareasConLaMismaMatriculaProducenUnRegistroYUnConflicto()
            throws Exception {

        var repository = new AlumnoRepositoryEnMemoria();

        var alumno1 = new Alumno(
                null, "A100", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO);

        var alumno2 = new Alumno(
                null, "A100", "Grace", "grace@u.mx", EstatusAlumno.ACTIVO);

        var preparados = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            var tarea1 = executor.submit(() -> {
                preparados.countDown();
                inicio.await();

                try {
                    repository.guardar(alumno1);
                    return "GUARDADO";
                } catch (MatriculaDuplicadaException e) {
                    return "CONFLICTO";
                }
            });

            var tarea2 = executor.submit(() -> {
                preparados.countDown();
                inicio.await();

                try {
                    repository.guardar(alumno2);
                    return "GUARDADO";
                } catch (MatriculaDuplicadaException e) {
                    return "CONFLICTO";
                }
            });

            // Ambas tareas están preparadas antes de darles la salida.
            preparados.await();
            inicio.countDown();

            var resultado1 = tarea1.get();
            var resultado2 = tarea2.get();

            int guardados = (int) java.util.List.of(resultado1, resultado2)
                    .stream()
                    .filter("GUARDADO"::equals)
                    .count();

            int conflictos = (int) java.util.List.of(resultado1, resultado2)
                    .stream()
                    .filter("CONFLICTO"::equals)
                    .count();

            assertEquals(1, guardados, "Debe guardarse exactamente un alumno");
            assertEquals(1, conflictos, "Debe producirse exactamente un conflicto");

            var alumnos = repository.buscarTodos();

            assertEquals(1, alumnos.size(), "Debe existir un solo registro");
            assertEquals("A100", alumnos.get(0).matricula());
        }
    }
}
