package GUI;

import Service.*;
import Excepciones.ConexionBDException;
import modelo.Usuario;

/** Comparte servicios; las cargas se ejecutan fuera del hilo gráfico. No contiene SQL. */
public class Datos {
    public UsuarioService usuarios;
    public SesionService sesion;
    public ArtistaService artistas;
    public CancionService canciones;
    public AlbumService albumes;
    public PlaylistService playlists;
    public void conectar() throws ConexionBDException {
        usuarios = new UsuarioService();
        sesion = new SesionService(usuarios); usuarios.setSesion(sesion);
    }
    public void cargarCatalogo() throws ConexionBDException {
        artistas = new ArtistaService(); artistas.setSesion(sesion);
        canciones = new CancionService(); canciones.setSesion(sesion);
        albumes = new AlbumService(canciones); albumes.setSesion(sesion);
        playlists = new PlaylistService(canciones); playlists.setSesion(sesion);
    }
    public Usuario actual() {
        sesion.exigirSesion();
        try { return usuarios.buscarPorId(sesion.getUsuarioActual().getIdUsuario()); }
        catch (Excepciones.ElementoNoEncontradoException e) {
            throw new Excepciones.PermisoDenegadoException("El usuario ya no está activo. Cerrá la sesión.");
        }
    }
    public String propietario(int id) {
        if (id == actual().getIdUsuario()) return actual().getNombre() + " (vos)";
        if (sesion.esAdministrador()) {
            try { return usuarios.buscarPorId(id).getNombre(); }
            catch (Excepciones.ElementoNoEncontradoException e) { return "Usuario inactivo"; }
        }
        return "Otro usuario";
    }
}
