package Service;

import Excepciones.*;
import modelo.*;
import Seguridad.Seguridad;

public class SesionService {
    private int idUsuarioActual;
    private final UsuarioService usuarios;

    public SesionService(UsuarioService usuarios) {
        this.usuarios = usuarios;
        usuarios.setSesion(this);
    }

    public Usuario iniciarSesion(String email, String contrasenia) throws CredencialesInvalidasException, ConexionBDException {
        cerrarSesion(); // Un intento fallido siempre deja la sesion cerrada.
        Usuario u = email == null ? null : usuarios.autenticar(email.trim());
        if (u == null || !u.isActivo() || contrasenia == null || !u.verificarContrasenia(contrasenia))
            throw new CredencialesInvalidasException("Email o contrasenia incorrectos.");
        idUsuarioActual = u.getIdUsuario();
        return u;
    }

    public UsuarioNormal registrar(String nombre, String email, String contrasenia) throws ConexionBDException, ElementoDuplicadoException {
        Acceso.texto(contrasenia, 100, "Contrasenia");
        String salt = Seguridad.generarSalt();
        UsuarioNormal u = new UsuarioNormal(nombre == null ? null : nombre.trim(), email == null ? null : email.trim(), Seguridad.hashear(contrasenia, salt), salt, false);
        usuarios.registrarNormal(u);
        return u;
    }

    public void cerrarSesion() { idUsuarioActual = 0; }
    public boolean esAdministrador() {
        Usuario u = getUsuarioActual();
        return u != null && u.puedeAdministrar();
    }

    public Usuario getUsuarioActual() {
        if (idUsuarioActual == 0) return null;
        try {
            Usuario u = usuarios.cargarSesion(idUsuarioActual);
            if (u != null && u.isActivo()) return u;
            cerrarSesion();
            return null;
        } catch (ConexionBDException e) {
            cerrarSesion();
            throw new IllegalStateException("No se pudo validar la sesion: " + e.getMessage(), e);
        }
    }
}
