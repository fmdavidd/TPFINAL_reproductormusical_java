package Service;

import DAO.*;
import Excepciones.*;
import modelo.*;
import java.util.ArrayList;
import java.util.List;

public class CancionService {
    private final CancionDAO dao = new CancionDAO();
    private final SesionService sesion;
    public CancionService() throws ConexionBDException { this((SesionService) null); }
    public CancionService(SesionService sesion) { this.sesion = sesion; }

    public void alta(Cancion a) throws ConexionBDException, ElementoDuplicadoException, PermisoDenegadoException {
        Acceso.administrador(sesion, false);
        if (a == null || a.getIdCancion() != 0) throw new IllegalArgumentException("Un alta no debe tener ID asignado.");
        validar(a);
        int id = dao.insertar(a);
        if (id <= 0) throw new ConexionBDException("No se pudo crear cancion.");
        a.setIdCancion(id);
    }

    public void modificar(Cancion a) throws ConexionBDException, ElementoDuplicadoException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.administrador(sesion, false);
        if (a == null) throw new IllegalArgumentException("Entidad requerida.");
        buscarPorId(a.getIdCancion());
        validar(a);
        Acceso.resultado(dao.modificar(a));
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.administrador(sesion, false);
        buscarPorId(id);
        Acceso.resultado(dao.darDeBaja(id));
        try { new PlaylistDAO().limpiarCancionInactiva(id); }
        catch (ConexionBDException e) {
            throw new ConexionBDException("La cancion se dio de baja, pero quedo pendiente normalizar playlists. " + e.getMessage());
        }
    }

    public Cancion buscarPorId(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.autenticado(sesion);
        Cancion a = id <= 0 ? null : dao.buscarPorId(id);
        if (a == null || !a.isActivo() || a.getArtista() == null || !a.getArtista().isActivo()
                || (a.getAlbum() != null && (!a.getAlbum().isActivo() || a.getAlbum().getArtista().getIdArtista() != a.getArtista().getIdArtista()))) throw new ElementoNoEncontradoException("No existe cancion activo con id " + id);
        return a;
    }

    public List<Cancion> listar() throws ConexionBDException, PermisoDenegadoException {
        Acceso.autenticado(sesion);
        return dao.listarActivos();
    }

    private void validar(Cancion a) throws ConexionBDException, ElementoDuplicadoException {
        if (a == null || !a.isActivo()) throw new IllegalArgumentException("Se requiere una entidad activa.");
        Acceso.texto(a.getTitulo(), 100, "Nombre o titulo");

        Artista artista = new ArtistaDAO().buscarPorId(a.getArtista() == null ? 0 : a.getArtista().getIdArtista());
        if (artista == null || !artista.isActivo()) throw new IllegalArgumentException("El artista debe existir y estar activo.");
        if (a.getDuracionSegundos() <= 0 || a.getGenero() == null) throw new IllegalArgumentException("Duracion y genero invalidos.");
        if (a.getRutaArchivo() != null && a.getRutaArchivo().length() > 255) throw new IllegalArgumentException("Ruta demasiado larga.");
        if (a.getAlbum() != null) {
            Album album = new AlbumDAO().buscarPorId(a.getAlbum().getIdAlbum());
            if (album == null || !album.isActivo() || album.getArtista().getIdArtista() != artista.getIdArtista())
                throw new IllegalArgumentException("El album debe estar activo y pertenecer al artista.");
        }
        for (Cancion otro : dao.listarActivos()) {
            if (otro.getIdCancion() != a.getIdCancion() && otro.getTitulo().trim().equalsIgnoreCase(a.getTitulo().trim()) && otro.getArtista().getIdArtista() == a.getArtista().getIdArtista())
                throw new ElementoDuplicadoException("Ya existe cancion con ese nombre.");
        }
    }
    public List<Cancion> buscarPorTitulo(String texto) throws ConexionBDException, PermisoDenegadoException {
        List<Cancion> resultado = new ArrayList<>();
        for (Cancion c : listar()) if (c.getTitulo().toLowerCase().contains(texto.toLowerCase())) resultado.add(c);
        return resultado;
    }
    public List<Cancion> buscarPorGenero(Enumeraciones.Genero genero) throws ConexionBDException, PermisoDenegadoException {
        List<Cancion> resultado = new ArrayList<>();
        for (Cancion c : listar()) if (c.getGenero() == genero) resultado.add(c);
        return resultado;
    }
}
