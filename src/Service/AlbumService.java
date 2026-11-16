package Service;

import DAO.*;
import Excepciones.*;
import modelo.*;
import java.util.ArrayList;
import java.util.List;

public class AlbumService {
    private final AlbumDAO dao = new AlbumDAO();
    private final SesionService sesion;
    public AlbumService(CancionService canciones) throws ConexionBDException { this((SesionService) null); }
    public AlbumService(SesionService sesion) { this.sesion = sesion; }

    public void alta(Album a) throws ConexionBDException, ElementoDuplicadoException, PermisoDenegadoException {
        Acceso.administrador(sesion, false);
        if (a == null || a.getIdAlbum() != 0) throw new IllegalArgumentException("Un alta no debe tener ID asignado.");
        validar(a);
        int id = dao.insertar(a);
        if (id <= 0) throw new ConexionBDException("No se pudo crear album.");
        a.setIdAlbum(id);
    }

    public void modificar(Album a) throws ConexionBDException, ElementoDuplicadoException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.administrador(sesion, false);
        if (a == null) throw new IllegalArgumentException("Entidad requerida.");
        buscarPorId(a.getIdAlbum());
        validar(a);
        Acceso.resultado(dao.modificar(a));
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.administrador(sesion, false);
        buscarPorId(id);
        if (!listarCanciones(id).isEmpty()) throw new IllegalArgumentException("Primero de de baja las canciones del album.");
        Acceso.resultado(dao.darDeBaja(id));
    }

    public Album buscarPorId(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.autenticado(sesion);
        Album a = id <= 0 ? null : dao.buscarPorId(id);
        if (a == null || !a.isActivo() || a.getArtista() == null || !a.getArtista().isActivo()) throw new ElementoNoEncontradoException("No existe album activo con id " + id);
        return a;
    }

    public List<Album> listar() throws ConexionBDException, PermisoDenegadoException {
        Acceso.autenticado(sesion);
        return dao.listarActivos();
    }

    private void validar(Album a) throws ConexionBDException, ElementoDuplicadoException {
        if (a == null || !a.isActivo()) throw new IllegalArgumentException("Se requiere una entidad activa.");
        Acceso.texto(a.getTitulo(), 100, "Nombre o titulo");

        Artista artista = new ArtistaDAO().buscarPorId(a.getArtista() == null ? 0 : a.getArtista().getIdArtista());
        if (artista == null || !artista.isActivo()) throw new IllegalArgumentException("El artista debe existir y estar activo.");
        if (a.getAnioLanzamiento() < 1 || a.getAnioLanzamiento() > java.time.Year.now().getValue() + 1)
            throw new IllegalArgumentException("Anio de lanzamiento invalido.");
        if (a.getPortada() != null && a.getPortada().length() > 255) throw new IllegalArgumentException("Portada demasiado larga.");
        for (Cancion c : new CancionDAO().listarActivos()) {
            if (c.getAlbum() != null && c.getAlbum().getIdAlbum() == a.getIdAlbum()
                    && c.getArtista().getIdArtista() != artista.getIdArtista())
                throw new IllegalArgumentException("El album tiene canciones de otro artista.");
        }
        for (Album otro : dao.listarActivos()) {
            if (otro.getIdAlbum() != a.getIdAlbum() && otro.getTitulo().trim().equalsIgnoreCase(a.getTitulo().trim()) && otro.getArtista().getIdArtista() == a.getArtista().getIdArtista())
                throw new ElementoDuplicadoException("Ya existe album con ese nombre.");
        }
    }
    public List<Album> listarPorArtista(int idArtista) throws ConexionBDException, PermisoDenegadoException {
        List<Album> resultado = new ArrayList<>();
        for (Album a : listar()) if (a.getArtista().getIdArtista() == idArtista) resultado.add(a);
        return resultado;
    }
    public List<Cancion> listarCanciones(int idAlbum) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        buscarPorId(idAlbum);
        return new CancionDAO().listarPorAlbum(idAlbum);
    }
}
