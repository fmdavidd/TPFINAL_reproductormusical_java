package Service;

import java.util.ArrayList;
import java.util.List;

import DAO.UsuarioDAO;
import Enumeraciones.NivelAdmin;
import Excepciones.ConexionBDException;
import Excepciones.ElementoDuplicadoException;
import Excepciones.ElementoNoEncontradoException;
import modelo.Usuario;
import modelo.UsuarioAdministrador;
import modelo.UsuarioNormal;

public class UsuarioService {

    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    private List<Usuario> usuarios = new ArrayList<>();

    public UsuarioService() throws ConexionBDException {
        usuarios.addAll(usuarioDAO.listarActivos());
    }


    public void alta(Usuario u) throws ConexionBDException, ElementoDuplicadoException {
        validarEmail(u);
        int id = usuarioDAO.insertar(u);
        if (id <= 0) {
            throw new ConexionBDException("No se pudo dar de alta el usuario");
        }
        u.setIdUsuario(id);
        usuarios.add(u);
    }

    public void modificar(Usuario u)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        Usuario existente = buscarPorId(u.getIdUsuario());
        validarEmail(u);
        if (!usuarioDAO.modificar(u)) {
            throw new ElementoNoEncontradoException("El usuario " + u.getIdUsuario() + " no existe en la base");
        }
        usuarios.set(usuarios.indexOf(existente), u);
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException {
        Usuario u = buscarPorId(id);
        usuarioDAO.darDeBaja(id);
        usuarios.remove(u);
    }

    public List<Usuario> listarInactivos() throws ConexionBDException {
        return usuarioDAO.listarInactivos();
    }

    public void reactivar(int id)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        for (Usuario activo : usuarios) {
            if (activo.getIdUsuario() == id) {
                throw new ElementoDuplicadoException("El usuario " + id + " ya esta activo");
            }
        }
        Usuario u = usuarioDAO.buscarPorId(id);
        if (u == null) {
            throw new ElementoNoEncontradoException("No existe el usuario con id " + id);
        }
        validarEmail(u);
        usuarioDAO.darDeAlta(id);
        u.setActivo(true);
        usuarios.add(u);
    }

    public void cambiarNivelAdmin(int idUsuario, NivelAdmin nuevoNivel)
            throws ConexionBDException, ElementoNoEncontradoException {
        Usuario u = buscarPorId(idUsuario);
        if (!(u instanceof UsuarioAdministrador)) {
            throw new ElementoNoEncontradoException("El usuario " + idUsuario + " no es administrador");
        }
        UsuarioAdministrador admin = (UsuarioAdministrador) u;
        NivelAdmin anterior = admin.getNivelAdmin();
        admin.setNivelAdmin(nuevoNivel);
        try {
            usuarioDAO.modificar(admin);
        } catch (ConexionBDException e) {
            admin.setNivelAdmin(anterior); // si fallo la base, la coleccion queda como estaba
            throw e;
        }
    }

    public boolean agregarFavorito(int idUsuario, int idCancion)
            throws ConexionBDException, ElementoNoEncontradoException {
        UsuarioNormal u = obtenerNormal(idUsuario);
        if (usuarioDAO.agregarFavorito(idUsuario, idCancion)) {
            u.agregarFavorito(idCancion);
            return true;
        }
        return false;
    }

    public boolean quitarFavorito(int idUsuario, int idCancion)
            throws ConexionBDException, ElementoNoEncontradoException {
        UsuarioNormal u = obtenerNormal(idUsuario);
        if (usuarioDAO.quitarFavorito(idUsuario, idCancion)) {
            u.quitarFavorito(idCancion);
            return true;
        }
        return false;
    }

    public void registrarAccion(int idAdmin, String accion)
            throws ConexionBDException, ElementoNoEncontradoException {
        Usuario u = buscarPorId(idAdmin);
        if (!(u instanceof UsuarioAdministrador)) {
            throw new ElementoNoEncontradoException("El usuario " + idAdmin + " no es administrador");
        }
        usuarioDAO.registrarAccion(idAdmin, accion);
        ((UsuarioAdministrador) u).registrarAccion(accion);
    }


    public Usuario buscarPorId(int id) throws ElementoNoEncontradoException {
        for (Usuario u : usuarios) {
            if (u.getIdUsuario() == id) {
                return u;
            }
        }
        throw new ElementoNoEncontradoException("No existe el usuario con id " + id);
    }

    public Usuario buscarPorEmail(String email) {
        for (Usuario u : usuarios) {
            if (u.getEmail().equalsIgnoreCase(email.trim())) {
                return u;
            }
        }
        return null;
    }

    public List<Usuario> listar() {
        return new ArrayList<>(usuarios);
    }


    private UsuarioNormal obtenerNormal(int idUsuario) throws ElementoNoEncontradoException {
        Usuario u = buscarPorId(idUsuario);
        if (!(u instanceof UsuarioNormal)) {
            throw new ElementoNoEncontradoException("El usuario " + idUsuario + " no es un usuario normal");
        }
        return (UsuarioNormal) u;
    }

    private void validarEmail(Usuario u) throws ElementoDuplicadoException {
        for (Usuario otro : usuarios) {
            if (otro.getIdUsuario() != u.getIdUsuario() && otro.getEmail().equalsIgnoreCase(u.getEmail())) {
                throw new ElementoDuplicadoException("Ya existe un usuario con el email " + u.getEmail());
            }
        }
    }
}
