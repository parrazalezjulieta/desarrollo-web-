
package mx.edu.backendacademico.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaAlumnoSpringDataRepository
        extends JpaRepository<CuentaAlumnoEntity, String> {

    boolean existsByUsuarioAndAlumnoId(String usuario, Long alumnoId);
}
