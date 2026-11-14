package mx.edu.backendacademico;

import mx.edu.backendacademico.service.InscripcionService;
import mx.edu.backendacademico.repository.jpa.InscripcionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class InscripcionConcurrenciaTest {

    @Autowired
    private InscripcionService servicio;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InscripcionRepository inscripciones;

    @Test
    void dosAlumnosCompitenPorUnSoloCupo() throws Exception {

        // 1. Identificadores únicos para los datos de esta prueba.
        String sufijo = String.valueOf(System.nanoTime());
        String matriculaA = "CA" + sufijo;
        String matriculaB = "CB" + sufijo;
        String claveMateria = "CM" + sufijo;

        Long alumnoA = null;
        Long alumnoB = null;
        Long materiaId = null;

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // 2. Crear dos alumnos activos.
            alumnoA = jdbc.queryForObject(
                    "INSERT INTO alumnos (matricula, nombre, correo, estatus) " +
                            "VALUES (?, ?, ?, 'ACTIVO') RETURNING id",
                    Long.class,
                    matriculaA, "Alumno Concurrente A", matriculaA + "@test.com"
            );

            alumnoB = jdbc.queryForObject(
                    "INSERT INTO alumnos (matricula, nombre, correo, estatus) " +
                            "VALUES (?, ?, ?, 'ACTIVO') RETURNING id",
                    Long.class,
                    matriculaB, "Alumno Concurrente B", matriculaB + "@test.com"
            );

            // 3. Crear una materia con un solo lugar.
            materiaId = jdbc.queryForObject(
                    "INSERT INTO materias (clave, nombre, creditos, cupo) " +
                            "VALUES (?, ?, 8, 1) RETURNING id",
                    Long.class,
                    claveMateria, "Materia de concurrencia"
            );

            // 4. Preparar la sincronización de los dos hilos.
            CountDownLatch listos = new CountDownLatch(2);
            CountDownLatch iniciar = new CountDownLatch(1);

            final Long idAlumnoA = alumnoA;
            final Long idAlumnoB = alumnoB;
            final Long idMateria = materiaId;

            // 5. Primera solicitud.
            Future<String> tareaA = executor.submit(() -> {
                listos.countDown();
                iniciar.await();

                try {
                    Long id = servicio.inscribir(idAlumnoA, idMateria);
                    return "ACEPTADA: alumno A, inscripción " + id;
                } catch (Exception e) {
                    return "RECHAZADA: alumno A, " + e.getMessage();
                }
            });

            // 6. Segunda solicitud.
            Future<String> tareaB = executor.submit(() -> {
                listos.countDown();
                iniciar.await();

                try {
                    Long id = servicio.inscribir(idAlumnoB, idMateria);
                    return "ACEPTADA: alumno B, inscripción " + id;
                } catch (Exception e) {
                    return "RECHAZADA: alumno B, " + e.getMessage();
                }
            });

            // 7. Esperar hasta que ambos hilos estén preparados.
            listos.await();

            // 8. Liberar los dos hilos simultáneamente.
            iniciar.countDown();

            String resultadoA = tareaA.get();
            String resultadoB = tareaB.get();

            System.out.println("Resultado A: " + resultadoA);
            System.out.println("Resultado B: " + resultadoB);

            // 9. Comprobar el resultado en PostgreSQL.
            long total = inscripciones.countByMateriaId(idMateria);

            System.out.println("Inscripciones persistidas: " + total);

            assertEquals(1, total,
                    "Debe existir exactamente una inscripción");

            boolean aAceptada = resultadoA.startsWith("ACEPTADA");
            boolean bAceptada = resultadoB.startsWith("ACEPTADA");

            assertEquals(1, (aAceptada ? 1 : 0) + (bAceptada ? 1 : 0),
                    "Exactamente un alumno debe ser aceptado");

            String rechazo = aAceptada ? resultadoB : resultadoA;

            org.junit.jupiter.api.Assertions.assertTrue(
                    rechazo.toLowerCase().contains("cupo"),
                    "La otra solicitud debe rechazarse por falta de cupo"
            );

        } finally {
            // 10. Liberar los hilos y limpiar los datos de prueba.
            executor.shutdownNow();

            if (materiaId != null) {
                jdbc.update(
                        "DELETE FROM inscripciones WHERE materia_id = ?",
                        materiaId
                );
            }

            jdbc.update(
                    "DELETE FROM alumnos WHERE matricula IN (?, ?)",
                    matriculaA, matriculaB
            );

            if (materiaId != null) {
                jdbc.update(
                        "DELETE FROM materias WHERE id = ?",
                        materiaId
                );
            }
        }
    }
}