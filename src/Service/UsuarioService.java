package Service;

import java.util.ArrayList;
import java.util.List;

import DAO.UsuarioDAO;
import Enumeraciones.NivelAdmin;
import Excepciones.ConexionBDException;
import Excepciones.ElementoDuplicadoException;
import Excepciones.ElementoNoEncontradoException;
import modelo.Usuario;
import modelo.RegistroAdministrativo;
import modelo.UsuarioAdministrador;
import modelo.UsuarioNormal;

public class UsuarioService {
    private SesionService sesion;

    public void setSesion(SesionService sesion) { this.sesion = sesion; }

    private void exigirGestionUsuarios() {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión autorizada.");
        sesion.exigirGestionUsuarios();
    }


    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    private List<Usuario> usuarios = new ArrayList<>();

    public UsuarioService() throws ConexionBDException {
        usuarios.addAll(usuarioDAO.listarActivos());
    }


    public void alta(Usuario u) throws ConexionBDException, ElementoDuplicadoException {
        exigirGestionUsuarios();
        insertar(u);
        guardarAccion(sesion.getUsuarioActual().getIdUsuario(), "Creó " +
                (u instanceof UsuarioAdministrador ? "administrador" : "usuario") + " ID " + u.getIdUsuario());
    }

    // Solo SesionService puede usar el alta pública y siempre construye un UsuarioNormal.
    void registrarNormal(UsuarioNormal u) throws ConexionBDException, ElementoDuplicadoException {
        if (u.esPremium()) throw new Excepciones.PermisoDenegadoException("El registro público crea cuentas normales sin premium.");
        insertar(u);
    }

    private void insertar(Usuario u) throws ConexionBDException, ElementoDuplicadoException {
        validarDatos(u);
        validarEmail(u);
        int id = usuarioDAO.insertar(u);
        if (id <= 0) {
            throw new ConexionBDException("No se pudo dar de alta el usuario");
        }
        u.setIdUsuario(id);
        usuarios.add(copiaAdministrador(u));
    }

    public void modificar(Usuario u)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        exigirGestionUsuarios();
        Usuario existente = obtenerInterno(u.getIdUsuario());
        int actor = sesion.getUsuarioActual().getIdUsuario();
        validarDatos(u);
        if (existente.getRol() != u.getRol()) throw new Excepciones.PermisoDenegadoException(
                "No se puede cambiar el tipo de cuenta desde este formulario.");
        if (!u.isActivo()) throw new Excepciones.PermisoDenegadoException("Usá la baja para desactivar una cuenta.");
        protegerUltimoSuperadmin(existente, u instanceof UsuarioAdministrador a ? a.getNivelAdmin() : null);
        NivelAdmin nivelAnterior = existente instanceof UsuarioAdministrador a ? a.getNivelAdmin() : null;
        validarEmail(u);
        if (!usuarioDAO.modificar(u)) {
            throw new ElementoNoEncontradoException("El usuario " + u.getIdUsuario() + " no existe en la base");
        }
        usuarios.set(usuarios.indexOf(existente), copiaAdministrador(u));
        guardarAccion(actor, "Modificó usuario ID " + u.getIdUsuario());
        if (u instanceof UsuarioAdministrador a && a.getNivelAdmin() != nivelAnterior)
            guardarAccion(actor, "Cambió el nivel del administrador ID " + u.getIdUsuario() + " a " + a.getNivelAdmin());
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException {
        exigirGestionUsuarios();
        if (sesion.getUsuarioActual().getIdUsuario() == id)
            throw new Excepciones.PermisoDenegadoException("No podés dar de baja tu propia sesión.");
        Usuario u = obtenerInterno(id);
        protegerUltimoSuperadmin(u, null);
        if (!usuarioDAO.darDeBaja(id)) throw new ElementoNoEncontradoException("No existe el usuario " + id);
        usuarios.remove(u);
        guardarAccion(sesion.getUsuarioActual().getIdUsuario(), "Dio de baja usuario ID " + id);
    }

    public List<Usuario> listarInactivos() throws ConexionBDException {
        exigirGestionUsuarios();
        return usuarioDAO.listarInactivos();
    }

    public void reactivar(int id)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        exigirGestionUsuarios();
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
        if (!usuarioDAO.darDeAlta(id)) throw new ElementoNoEncontradoException("No existe el usuario " + id);
        u.setActivo(true);
        usuarios.add(copiaAdministrador(u));
        guardarAccion(sesion.getUsuarioActual().getIdUsuario(), "Reactivó usuario ID " + id);
    }

    public void cambiarNivelAdmin(int idUsuario, NivelAdmin nuevoNivel)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        exigirGestionUsuarios();
        if (nuevoNivel == null) throw new IllegalArgumentException("Seleccioná un nivel administrativo.");
        Usuario u = buscarPorId(idUsuario);
        if (!(u instanceof UsuarioAdministrador admin)) {
            throw new ElementoNoEncontradoException("El usuario " + idUsuario + " no es administrador");
        }
        admin.setNivelAdmin(nuevoNivel);
        modificar(admin);
    }

    public boolean agregarFavorito(int idUsuario, int idCancion)
            throws ConexionBDException, ElementoNoEncontradoException {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión.");
        sesion.exigirPropietario(idUsuario);
        UsuarioNormal u = obtenerNormal(idUsuario);
        if (usuarioDAO.agregarFavorito(idUsuario, idCancion)) {
            u.agregarFavorito(idCancion);
            return true;
        }
        return false;
    }

    public boolean quitarFavorito(int idUsuario, int idCancion)
            throws ConexionBDException, ElementoNoEncontradoException {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión.");
        sesion.exigirPropietario(idUsuario);
        UsuarioNormal u = obtenerNormal(idUsuario);
        if (usuarioDAO.quitarFavorito(idUsuario, idCancion)) {
            u.quitarFavorito(idCancion);
            return true;
        }
        return false;
    }

    public void registrarAccion(int idAdmin, String accion)
            throws ConexionBDException {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión.");
        sesion.exigirAdministrador();
        if (sesion.getUsuarioActual().getIdUsuario() != idAdmin)
            throw new Excepciones.PermisoDenegadoException("No podés registrar acciones de otro administrador.");
        guardarAccion(idAdmin, accion);
    }

    public List<RegistroAdministrativo> listarHistorialAdministrativo() throws ConexionBDException {
        exigirGestionUsuarios();
        return usuarioDAO.listarHistorialAdministrativo();
    }

    private void guardarAccion(int idAdmin, String accion) throws ConexionBDException {
        try { usuarioDAO.registrarAccion(idAdmin, accion); }
        catch (ConexionBDException e) {
            throw new ConexionBDException("El cambio se guardó, pero no se pudo registrar la acción: " + e.getMessage());
        }
        for (Usuario u : usuarios) {
            if (u.getIdUsuario() == idAdmin && u instanceof UsuarioAdministrador admin) admin.registrarAccion(accion);
        }
    }


    public Usuario buscarPorId(int id) throws ElementoNoEncontradoException {
        return copiaAdministrador(obtenerInterno(id));
    }

    private Usuario obtenerInterno(int id) throws ElementoNoEncontradoException {
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
                return copiaAdministrador(u);
            }
        }
        return null;
    }

    public List<Usuario> listar() {
        exigirGestionUsuarios();
        return usuarios.stream().map(this::copiaAdministrador).toList();
    }


    private UsuarioNormal obtenerNormal(int idUsuario) throws ElementoNoEncontradoException {
        Usuario u = buscarPorId(idUsuario);
        if (!(u instanceof UsuarioNormal)) {
            throw new ElementoNoEncontradoException("El usuario " + idUsuario + " no es un usuario normal");
        }
        return (UsuarioNormal) u;
    }

    public void recargar() throws ConexionBDException {
        List<Usuario> nuevos = usuarioDAO.listarActivos();
        usuarios.clear();
        usuarios.addAll(nuevos);
    }

    private void protegerUltimoSuperadmin(Usuario existente, NivelAdmin nuevoNivel) {
        if (existente instanceof UsuarioAdministrador a && a.getNivelAdmin() == NivelAdmin.SUPERADMIN
                && nuevoNivel != NivelAdmin.SUPERADMIN) {
            long cantidad = usuarios.stream().filter(u -> u.isActivo() && u instanceof UsuarioAdministrador admin
                    && admin.getNivelAdmin() == NivelAdmin.SUPERADMIN).count();
            if (cantidad <= 1) throw new Excepciones.PermisoDenegadoException(
                    "No se puede quitar ni degradar al último SUPERADMIN activo.");
        }
    }

    // No entregar la instancia interna: cambiar un ComboBox o un setter no otorga privilegios.
    private Usuario copiaAdministrador(Usuario u) {
        if (!(u instanceof UsuarioAdministrador a)) return u;
        UsuarioAdministrador copia = new UsuarioAdministrador(a.getIdUsuario(), a.getNombre(), a.getEmail(),
                a.getHashContrasenia(), a.getSalt(), a.isActivo(), a.getFechaRegistro(), a.getNivelAdmin());
        copia.setRegistroAcciones(a.getRegistroAcciones());
        return copia;
    }

    private void validarDatos(Usuario u) {
        if (u == null || u.getNombre() == null || u.getNombre().isBlank() || u.getNombre().length() > 60
                || u.getEmail() == null || !u.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || u.getEmail().length() > 100 || u.getHashContrasenia() == null || u.getHashContrasenia().isBlank()
                || u.getSalt() == null || u.getSalt().isBlank())
            throw new IllegalArgumentException("Completá nombre, correo válido y contraseña.");
        if (u instanceof UsuarioAdministrador a && a.getNivelAdmin() == null)
            throw new IllegalArgumentException("Seleccioná un nivel administrativo.");
    }

    private void validarEmail(Usuario u) throws ElementoDuplicadoException {
        for (Usuario otro : usuarios) {
            if (otro.getIdUsuario() != u.getIdUsuario() && otro.getEmail().equalsIgnoreCase(u.getEmail())) {
                throw new ElementoDuplicadoException("Ya existe un usuario con el email " + u.getEmail());
            }
        }
    }
}
