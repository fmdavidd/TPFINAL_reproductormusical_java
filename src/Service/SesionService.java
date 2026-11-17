package Service;

import Excepciones.CredencialesInvalidasException;
import Excepciones.ConexionBDException;
import Excepciones.ElementoDuplicadoException;
import modelo.Usuario;
import modelo.UsuarioNormal;
import Seguridad.Seguridad;

public class SesionService {

    private Usuario usuarioActual;
    private UsuarioService usuarioService;

    public SesionService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }


    public Usuario iniciarSesion(String email, String contrasenia) throws CredencialesInvalidasException {
        Usuario u = usuarioService.buscarPorEmail(email);
        if (u == null || !u.verificarContrasenia(contrasenia)) {
            throw new CredencialesInvalidasException("Email o contrasenia incorrectos");
        }
        usuarioActual = u;
        return u;
    }

    public UsuarioNormal registrar(String nombre, String email, String contrasenia)
            throws ConexionBDException, ElementoDuplicadoException {
        String salt = Seguridad.generarSalt();
        String hash = Seguridad.hashear(contrasenia, salt);
        UsuarioNormal nuevo = new UsuarioNormal(nombre, email, hash, salt, false);
        usuarioService.alta(nuevo);
        return nuevo;
    }

    public void cerrarSesion() {
        usuarioActual = null;
    }

    // Se usa cuando el usuario modifica sus propios datos: el service reemplaza
    // la instancia y la sesion tiene que apuntar a la nueva.
    public void refrescar(Usuario usuario) {
        usuarioActual = usuario;
    }

    public boolean esAdministrador() {
        return usuarioActual != null && usuarioActual.puedeAdministrar();
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }
}
