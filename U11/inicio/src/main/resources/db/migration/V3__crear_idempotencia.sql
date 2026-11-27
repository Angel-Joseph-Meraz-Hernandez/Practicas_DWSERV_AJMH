CREATE TABLE idempotencia_solicitudes (
    clave VARCHAR(128) PRIMARY KEY,
    huella CHAR(64) NOT NULL,
    alumno_id BIGINT REFERENCES alumnos(id),
    status_code INTEGER,
    location VARCHAR(255),
    response_body TEXT,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
