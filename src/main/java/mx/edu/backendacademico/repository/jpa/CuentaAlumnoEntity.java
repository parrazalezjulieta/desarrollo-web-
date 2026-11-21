
package mx.edu.backendacademico.repository.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cuentas_alumno")
public class CuentaAlumnoEntity {

    @Id
    @Column(name = "usuario", length = 50)
    private String usuario;

    @Column(name = "alumno_id", nullable = false, unique = true)
    private Long alumnoId;

    protected CuentaAlumnoEntity() {
    }

    public String getUsuario() {
        return usuario;
    }

    public Long getAlumnoId() {
        return alumnoId;
    }
}
