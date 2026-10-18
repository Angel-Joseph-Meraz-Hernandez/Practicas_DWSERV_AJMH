package mx.edu.backendacademico.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import org.junit.jupiter.api.Test;

class AlumnoRepositoryConcurrenciaTest {

    @Test
    void soloUnaTareaPuedeGuardarLaMismaMatricula() throws Exception {
        var repository = new AlumnoRepositoryEnMemoria();

        var listas = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);

        try (ExecutorService ejecutor = Executors.newFixedThreadPool(2)) {
            Future<String> tarea1 = ejecutor.submit(() ->
                    intentarGuardar(repository, "Alumno Uno", listas, inicio)
            );

            Future<String> tarea2 = ejecutor.submit(() ->
                    intentarGuardar(repository, "Alumno Dos", listas, inicio)
            );

            // Espera hasta que ambas tareas estén listas para competir.
            listas.await();

            // Libera ambas tareas sin introducir pausas arbitrarias.
            inicio.countDown();

            String resultado1 = tarea1.get();
            String resultado2 = tarea2.get();

            assertEquals(1,
                    java.util.List.of(resultado1, resultado2).stream()
                            .filter("GUARDADO"::equals)
                            .count());

            assertEquals(1,
                    java.util.List.of(resultado1, resultado2).stream()
                            .filter("DUPLICADO"::equals)
                            .count());

            assertEquals(1, repository.buscarTodos().size());
        }
    }

    private String intentarGuardar(
            AlumnoRepository repository,
            String nombre,
            CountDownLatch listas,
            CountDownLatch inicio) throws InterruptedException {

        listas.countDown();
        inicio.await();

        var alumno = new Alumno(
                null,
                "A001",
                nombre,
                nombre.toLowerCase().replace(" ", ".") + "@ejemplo.com",
                EstatusAlumno.ACTIVO
        );

        try {
            repository.guardar(alumno);
            return "GUARDADO";
        } catch (MatriculaDuplicadaException e) {
            assertInstanceOf(MatriculaDuplicadaException.class, e);
            return "DUPLICADO";
        }
    }
}
