
package mx.edu.backendacademico.service;

import java.util.List;

import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.AlumnoNoEncontradoException;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import mx.edu.backendacademico.repository.AlumnoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlumnoService {

    private final AlumnoRepository repository;

    public AlumnoService(AlumnoRepository repository) {
        this.repository = repository;
    }

    public List<Alumno> listar() {
        return repository.buscarTodos();
    }

    public Alumno buscar(Long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new AlumnoNoEncontradoException(id));
    }

    // Busca el expediente mediante la identidad autenticada.
    public Alumno buscarMiExpediente(String usuario) {
        return repository.buscarPorUsuario(usuario)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "La cuenta autenticada no tiene un expediente asociado."));
    }

    // Comprueba la propiedad del expediente.
    public boolean perteneceAUsuario(Long alumnoId, String usuario) {
        return repository.perteneceAUsuario(alumnoId, usuario);
    }

    @Transactional
    public Alumno crear(String matricula, String nombre, String correo) {
        if (repository.existePorMatricula(matricula)) {
            throw new MatriculaDuplicadaException(matricula);
        }

        return repository.guardar(
                new Alumno(null, matricula, nombre, correo, EstatusAlumno.ACTIVO));
    }

    @Transactional
    public Alumno actualizar(Long id, String nombre, String correo) {
        var actual = buscar(id);

        return repository.guardar(
                new Alumno(id, actual.matricula(), nombre, correo, actual.estatus()));
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        repository.eliminarPorId(id);
    }
}
