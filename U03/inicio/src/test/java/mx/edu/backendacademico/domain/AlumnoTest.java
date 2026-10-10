package mx.edu.backendacademico.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AlumnoTest {
    @Test void bajaConservaIdentidadSinModificarElOriginal() {
        var alumno = new Alumno(1L, " a001 ", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO);
        var baja = alumno.darDeBaja();
        // Comprueba tanto el resultado como la ausencia de efectos laterales.
        assertEquals("A001", baja.matricula());
        assertEquals(alumno.id(), baja.id());
        assertEquals(EstatusAlumno.BAJA, baja.estatus());
        assertEquals(EstatusAlumno.ACTIVO, alumno.estatus());
    }

    @Test void rechazaMatriculaVacia() {
        assertThrows(IllegalArgumentException.class, () ->
                new Alumno(null, " ", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO));
    }

    @Test void materiaRechazaCreditosNoPositivos() {
        assertThrows(IllegalArgumentException.class, () -> new Materia(null, "M1", "Análisis", 0));
    }

    @Test
    void reactivarConservaIdentidadSinModificarElOriginal() {
        var alumno = new Alumno(2L, "2067151", "Angel Joseph Meraz Hdz",
                "angel.merazh@uanl.edu.mx", EstatusAlumno.BAJA);
        var reactivado = alumno.reactivar();
        // Comprueba la transición y la conservación del original.
        assertEquals(alumno.id(), reactivado.id());
        assertEquals(EstatusAlumno.ACTIVO, reactivado.estatus());
        assertEquals(EstatusAlumno.BAJA, alumno.estatus());
    }

    @Test
    void alumnosConMismoIdYDistintoCorreoNoSonIguales() {
        var alumno1 = new Alumno(1L, "206", "Joseph", "joseph@u.mx", EstatusAlumno.ACTIVO);
        var alumno2 = new Alumno(1L, "206", "Joseph", "angel@u.mx", EstatusAlumno.ACTIVO);
        // Ambos representan el mismo ID, pero tienen valores diferentes.
        assertEquals(alumno1.id(), alumno2.id());
        assertNotEquals(alumno1, alumno2);
    }
}