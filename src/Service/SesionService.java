package Service;

import Excepciones.CredencialesInvalidasException;
import Excepciones.ConexionBDException;
import Excepciones.ElementoDuplicadoException;
import modelo.Usuario;
import modelo.UsuarioNormal;
import Seguridad.Seguridad;

public class SesionService {

    private Integer idUsuarioActual;
    private UsuarioService usuarioService;

    public SesionService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
        usuarioService.setSesion(this);
    }


    public Usuario iniciarSesion(String email, String contrasenia) throws CredencialesInvalidasException {
        Usuario u = usuarioService.buscarPorEmail(email);
        if (u == null || !u.isActivo() || !u.verificarContrasenia(contrasenia)) {
            throw new CredencialesInvalidasException("Email o contrasenia incorrectos");
        }
        idUsuarioActual = u.getIdUsuario();
        return u;
    }

    public UsuarioNormal registrar(String nombre, String email, String contrasenia)
            throws ConexionBDException, ElementoDuplicadoException {
        if (contrasenia == null || contrasenia.isBlank()) throw new IllegalArgumentException("Ingresá una contraseña.");
        String salt = Seguridad.generarSalt();
        String hash = Seguridad.hashear(contrasenia, salt);
        UsuarioNormal nuevo = new UsuarioNormal(nombre, email, hash, salt, false);
        usuarioService.registrarNormal(nuevo);
        return nuevo;
    }

    public void cerrarSesion() {
        idUsuarioActual = null;
    }

    public boolean esAdministrador() {
        Usuario actual = getUsuarioActual();
        return actual != null && actual.isActivo() && actual.puedeAdministrar();
    }

    public boolean puedeConsultarTodo() { return esAdministrador(); }

    public boolean puedeEditarContenido() {
        Usuario actual = getUsuarioActual();
        return esAdministrador() && actual instanceof modelo.UsuarioAdministrador admin
                && (admin.getNivelAdmin() == Enumeraciones.NivelAdmin.EDITOR
                || admin.getNivelAdmin() == Enumeraciones.NivelAdmin.SUPERADMIN);
    }

    public boolean puedeGestionarUsuarios() {
        Usuario actual = getUsuarioActual();
        return esAdministrador() && actual instanceof modelo.UsuarioAdministrador admin
                && admin.getNivelAdmin() == Enumeraciones.NivelAdmin.SUPERADMIN;
    }

    public boolean puedeGestionarAdministradores() { return puedeGestionarUsuarios(); }

    public void exigirEdicionContenido() {
        exigirSesion();
        if (!puedeEditarContenido()) throw new Excepciones.PermisoDenegadoException(
                "No tenés permisos para modificar el catálogo. Se requiere EDITOR o SUPERADMIN.");
    }

    public void exigirGestionUsuarios() {
        exigirSesion();
        if (!puedeGestionarUsuarios()) throw new Excepciones.PermisoDenegadoException(
                "Solo SUPERADMIN puede gestionar cuentas y niveles administrativos.");
    }

    public void registrarAccion(String accion) throws ConexionBDException {
        if (esAdministrador()) usuarioService.registrarAccion(getUsuarioActual().getIdUsuario(), accion);
    }

    public void exigirSesion() {
        Usuario actual = getUsuarioActual();
        if (actual == null || !actual.isActivo()) {
            throw new Excepciones.PermisoDenegadoException("Iniciá sesión para continuar.");
        }
    }

    public void exigirAdministrador() {
        exigirSesion();
        if (!esAdministrador()) {
            throw new Excepciones.PermisoDenegadoException("Esta operación requiere un administrador.");
        }
    }

    public void exigirPropietario(int idUsuario) {
        exigirSesion();
        if (getUsuarioActual().getIdUsuario() != idUsuario) {
            throw new Excepciones.PermisoDenegadoException("Solo podés modificar tus propios datos.");
        }
    }

    public Usuario getUsuarioActual() {
        if (idUsuarioActual == null) return null;
        try { return usuarioService.buscarPorId(idUsuarioActual); }
        catch (Excepciones.ElementoNoEncontradoException e) { idUsuarioActual = null; return null; }
    }
}
