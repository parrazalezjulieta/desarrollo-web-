CREATE TABLE cuentas_alumno (
                                usuario VARCHAR(50) PRIMARY KEY,
                                alumno_id BIGINT NOT NULL UNIQUE,

                                CONSTRAINT fk_cuentas_alumno_alumno
                                    FOREIGN KEY (alumno_id)
                                        REFERENCES alumnos(id)
                                        ON DELETE CASCADE
);

-- Alumnos de laboratorio necesarios para las cuentas de U10.
-- Se buscan por matrícula para no depender de IDs fijos.
INSERT INTO alumnos (matricula, nombre, correo, estatus)
VALUES
    ('U10A001', 'Ada Byron', 'ada@u.mx', 'ACTIVO'),
    ('U08A002', 'Alumno Prueba U08', 'u08a002@example.com', 'ACTIVO')
    ON CONFLICT (matricula) DO NOTHING;

-- Cuenta del alumno propietario de U10A001.
INSERT INTO cuentas_alumno (usuario, alumno_id)
SELECT 'alumno', id
FROM alumnos
WHERE matricula = 'U10A001';

-- Cuenta del alumno propietario de U08A002.
INSERT INTO cuentas_alumno (usuario, alumno_id)
SELECT 'alumno2', id
FROM alumnos
WHERE matricula = 'U08A002';
