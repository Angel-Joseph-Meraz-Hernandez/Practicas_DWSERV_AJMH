package mx.edu.backendacademico.repository;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class IdempotenciaRepository {
    private final JdbcTemplate jdbc;
    public IdempotenciaRepository(JdbcTemplate jdbc) {this.jdbc = jdbc;}
    public boolean reservar(String clave, String huella) {
        int filas = jdbc.update("""
                INSERT INTO idempotencia_solicitudes (clave, huella)
                VALUES (?, ?)
                ON CONFLICT (clave) DO NOTHING
                """, clave, huella);
        return filas == 1;
    }
    public Optional<RegistroIdempotencia> buscar(String clave) {
        return jdbc.query("""
                SELECT clave, huella, alumno_id, status_code,
                       location, response_body
                FROM idempotencia_solicitudes
                WHERE clave = ?
                """,
                (rs, rowNum) -> new RegistroIdempotencia(rs.getString("clave"),
                        rs.getString("huella"), rs.getObject("alumno_id", Long.class),
                        (Integer) rs.getObject("status_code"), rs.getString("location"),
                        rs.getString("response_body")), clave).stream().findFirst();
    }
    public void completar(String clave, Long alumnoId, int statusCode, String location, String responseBody) {
        jdbc.update("""
                UPDATE idempotencia_solicitudes
                SET alumno_id = ?,
                    status_code = ?,
                    location = ?,
                    response_body = ?
                WHERE clave = ?
                """,
                alumnoId, statusCode, location, responseBody, clave);
    }
    public record RegistroIdempotencia(String clave, String huella, Long alumnoId, Integer statusCode,
            String location, String responseBody) {}
}