package Vista.controladores;

import Vista.App;

import Enumeraciones.NivelAdmin;
import Excepciones.ConexionBDException;
import Excepciones.ElementoNoEncontradoException;
import modelo.Playlist;
import modelo.Usuario;
import modelo.UsuarioAdministrador;
import modelo.UsuarioNormal;

// Reglas de negocio que la consola tenia mezcladas con el menu. Quedan en la
// capa de vista para no modificar los services.
public final class Reglas {

    private Reglas() {
    }

    public static Usuario usuario() {
        return App.getSesion().getUsuarioActual();
    }

    public static boolean haySesion() {
        return usuario() != null;
    }

    public static boolean esAdmin() {
        Usuario u = usuario();
        return u != null && u.puedeAdministrar();
    }

    public static NivelAdmin nivel() {
        Usuario u = usuario();
        return u instanceof UsuarioAdministrador ? ((UsuarioAdministrador) u).getNivelAdmin() : null;
    }

    public static boolean esNormal() {
        return usuario() instanceof UsuarioNormal;
    }

    public static boolean puedeEditarContenido() {
        NivelAdmin n = nivel();
        return n == NivelAdmin.EDITOR || n == NivelAdmin.SUPERADMIN;
    }

    public static boolean puedeGestionarUsuarios() {
        return nivel() == NivelAdmin.SUPERADMIN;
    }

    public static boolean esPropietario(Playlist playlist) {
        return playlist != null && usuario() != null
                && playlist.getIdPropietario() == usuario().getIdUsuario();
    }

    public static boolean puedeVerPlaylist(Playlist playlist) {
        return playlist != null && (playlist.isPublica() || esAdmin() || esPropietario(playlist));
    }

    public static boolean puedeModificarPlaylist(Playlist playlist) {
        return playlist != null && (esPropietario(playlist) || puedeEditarContenido());
    }

    public static String descripcionRol() {
        Usuario u = usuario();
        if (u == null) {
            return "";
        }
        if (u instanceof UsuarioAdministrador) {
            return "Administrador (" + ((UsuarioAdministrador) u).getNivelAdmin() + ")";
        }
        return ((UsuarioNormal) u).esPremium() ? "Usuario premium" : "Usuario";
    }

    public static String nombreUsuario(int idUsuario) {
        try {
            return App.getUsuarios().buscarPorId(idUsuario).getNombre();
        } catch (ElementoNoEncontradoException e) {
            Ui.errorUnaVez("usuario-" + idUsuario, "Playlists", Ui.mensaje(e));
            return "#" + idUsuario;
        }
    }

    public static void registrarAccion(String accion) {
        Usuario u = usuario();
        if (u instanceof UsuarioAdministrador) {
            try {
                App.getUsuarios().registrarAccion(u.getIdUsuario(), accion);
            } catch (ConexionBDException | ElementoNoEncontradoException e) {
                Ui.errorUnaVez("registrar-accion", "Registro de acciones", Ui.mensaje(e));
            }
        }
    }
}
