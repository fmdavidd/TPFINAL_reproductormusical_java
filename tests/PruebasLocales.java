package Service;

import Excepciones.*;
import Enumeraciones.*;
import Seguridad.Seguridad;
import modelo.*;

// Pruebas sin servidor: modelos, sesiones y rechazo de operaciones antes de JDBC.
public class PruebasLocales {
    private static int comprobaciones;
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }

    private static class UsuariosPrueba extends UsuarioService {
        private Usuario usuario;
        UsuariosPrueba(Usuario usuario) throws ConexionBDException { this.usuario = usuario; }
        @Override Usuario autenticar(String email) {
            return usuario.getEmail().equalsIgnoreCase(email) ? usuario : null;
        }
        @Override Usuario cargarSesion(int id) { return usuario.getIdUsuario() == id ? usuario : null; }
    }

    public static void main(String[] args) throws Exception {
        Album album = new Album();
        Artista artista = new Artista();
        Playlist playlist = new Playlist();
        UsuarioNormal normal = new UsuarioNormal();
        comprobar(album.getDuracionTotal() == 0, "Album vacio");
        comprobar(artista.getGeneros().isEmpty(), "Generos inicializados");
        comprobar(playlist.getDuracionTotal() == 0, "Playlist vacia");
        comprobar(normal.getFavoritos().isEmpty() && normal.getPlaylists().isEmpty(), "Colecciones de usuario");
        Cancion c = new Cancion("Prueba", 120, "", Genero.ROCK, artista, null, true);
        c.setIdCancion(1);
        album.agregarCancion(c);
        album.getCanciones().clear();
        comprobar(album.getDuracionTotal() == 120, "Copia defensiva del album");
        playlist.agregarCancion(c);
        playlist.getCanciones().clear();
        comprobar(playlist.getCanciones().size() == 1, "Copia defensiva de playlist");
        artista.agregarGenero(Genero.ROCK);
        artista.getGeneros().clear();
        comprobar(artista.getGeneros().size() == 1, "Copia defensiva de generos");
        Cancion segunda = new Cancion("Segunda", 60, "", Genero.ROCK, artista, null, true);
        segunda.setIdCancion(2);
        playlist.agregarCancion(segunda);
        playlist.moverCancion(0, 1);
        comprobar(playlist.getCanciones().get(0).getIdCancion() == 2, "Orden en el modelo");
        try { playlist.moverCancion(-1, 0); throw new AssertionError("Posicion invalida aceptada"); }
        catch (IndexOutOfBoundsException esperado) { comprobaciones++; }

        UsuarioAdministrador admin = new UsuarioAdministrador("Admin", "admin@test.com", "hash", "salt", NivelAdmin.SUPERADMIN);
        for (int i = 0; i < 1000; i++) admin.registrarAccion("Accion " + i);
        comprobar(admin.getRegistroAcciones().size() == 1000, "Historial conserva todas las acciones");
        normal = new UsuarioNormal("Juan", "juan@test.com", Seguridad.hashear("1234", "salt"), "salt", false);
        normal.setIdUsuario(1);
        SesionService sesion = new SesionService(new UsuariosPrueba(normal));
        sesion.iniciarSesion("juan@test.com", "1234");
        comprobar(sesion.getUsuarioActual() != null, "Login correcto");
        try { sesion.iniciarSesion("juan@test.com", "incorrecta"); throw new AssertionError("Login invalido aceptado"); }
        catch (CredencialesInvalidasException esperado) { comprobaciones++; }
        comprobar(sesion.getUsuarioActual() == null, "Fallo de login cierra sesion previa");
        sesion.iniciarSesion("juan@test.com", "1234");
        try { new ArtistaService(sesion).alta(new Artista("Prueba", "Argentina", true)); throw new AssertionError("Normal administra catalogo"); }
        catch (PermisoDenegadoException esperado) { comprobaciones++; }
        try { Acceso.propietario(sesion, 2); throw new AssertionError("Otro propietario aceptado"); }
        catch (PermisoDenegadoException esperado) { comprobaciones++; }
        normal.setActivo(false);
        comprobar(sesion.getUsuarioActual() == null, "Una baja revoca la sesion");
        try { sesion.iniciarSesion("juan@test.com", "1234"); throw new AssertionError("Cuenta inactiva aceptada"); }
        catch (CredencialesInvalidasException esperado) { comprobaciones++; }
        admin.setIdUsuario(3);
        admin.setHashContrasenia(Seguridad.hashear("admin123", "salt"));
        sesion = new SesionService(new UsuariosPrueba(admin));
        sesion.iniciarSesion("admin@test.com", "admin123");
        Acceso.administrador(sesion, true);
        comprobaciones++;
        admin.setNivelAdmin(NivelAdmin.EDITOR);
        Acceso.administrador(sesion, false);
        comprobaciones++;
        try { Acceso.administrador(sesion, true); throw new AssertionError("Editor administra usuarios"); }
        catch (PermisoDenegadoException esperado) { comprobaciones++; }
        admin.setNivelAdmin(NivelAdmin.VISUALIZANTE);
        try { new CancionService(sesion).baja(1); throw new AssertionError("Visualizante modifica catalogo"); }
        catch (PermisoDenegadoException esperado) { comprobaciones++; }
        try { new ArtistaService().listar(); throw new AssertionError("Servicio sin sesion permite lectura"); }
        catch (PermisoDenegadoException esperado) { comprobaciones++; }
        try { new PlaylistService(new CancionService()).alta(new Playlist("Prueba", "", true, 1, true)); throw new AssertionError("Playlist sin sesion"); }
        catch (PermisoDenegadoException esperado) { comprobaciones++; }
        System.out.println("OK: " + comprobaciones + " comprobaciones locales. No prueban persistencia MySQL.");
    }
}
