package Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import DAO.AlbumDAO;
import Excepciones.ConexionBDException;
import Excepciones.ElementoDuplicadoException;
import Excepciones.ElementoNoEncontradoException;
import modelo.Album;
import modelo.Cancion;

public class AlbumService {

    private AlbumDAO albumDAO = new AlbumDAO();
    private CancionService cancionService;

    private Map<Integer, Album> albumes = new LinkedHashMap<>();

    public AlbumService(CancionService cancionService) throws ConexionBDException {
        this.cancionService = cancionService;
        for (Album a : albumDAO.listarActivos()) {
            albumes.put(a.getIdAlbum(), a);
        }
    }


    public void alta(Album a) throws ConexionBDException, ElementoDuplicadoException {
        validarDuplicado(a);
        int id = albumDAO.insertar(a);
        if (id <= 0) {
            throw new ConexionBDException("No se pudo dar de alta el album");
        }
        a.setIdAlbum(id);
        albumes.put(id, a);
    }

    public void modificar(Album a)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        buscarPorId(a.getIdAlbum());
        validarDuplicado(a);
        if (!albumDAO.modificar(a)) {
            throw new ElementoNoEncontradoException("El album " + a.getIdAlbum() + " no existe en la base");
        }
        albumes.put(a.getIdAlbum(), a);
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException {
        buscarPorId(id);
        albumDAO.darDeBaja(id);
        albumes.remove(id);
    }


    public List<Album> listarInactivos() throws ConexionBDException {
        return albumDAO.listarInactivos();
    }

    public void reactivar(int id)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        if (albumes.containsKey(id)) {
            throw new ElementoDuplicadoException("El album " + id + " ya esta activo");
        }
        Album a = albumDAO.buscarPorId(id);
        if (a == null) {
            throw new ElementoNoEncontradoException("No existe el album con id " + id);
        }
        if (!a.getArtista().isActivo()) {
            throw new ElementoNoEncontradoException("El artista del album esta dado de baja: reactivalo primero");
        }
        validarDuplicado(a);
        albumDAO.darDeAlta(id);
        a.setActivo(true);
        albumes.put(id, a);
    }


    public Album buscarPorId(int id) throws ElementoNoEncontradoException {
        Album a = albumes.get(id);
        if (a == null) {
            throw new ElementoNoEncontradoException("No existe el album con id " + id);
        }
        return a;
    }

    public List<Album> listar() {
        return new ArrayList<>(albumes.values());
    }

    public List<Album> listarPorArtista(int idArtista) {
        List<Album> resultado = new ArrayList<>();
        for (Album a : albumes.values()) {
            if (a.getArtista().getIdArtista() == idArtista) {
                resultado.add(a);
            }
        }
        return resultado;
    }

    // buscamos en el service para tomar las canciones activas del album
    public List<Cancion> listarCanciones(int idAlbum) throws ElementoNoEncontradoException {
        buscarPorId(idAlbum);
        List<Cancion> resultado = new ArrayList<>();
        for (Cancion c : cancionService.listar()) {
            if (c.getAlbum() != null && c.getAlbum().getIdAlbum() == idAlbum) {
                resultado.add(c);
            }
        }
        return resultado;
    }


    private void validarDuplicado(Album a) throws ElementoDuplicadoException {
        for (Album otro : albumes.values()) {
            if (otro.getIdAlbum() != a.getIdAlbum()
                    && otro.getTitulo().equalsIgnoreCase(a.getTitulo())
                    && otro.getArtista().getIdArtista() == a.getArtista().getIdArtista()) {
                throw new ElementoDuplicadoException("Ya existe el album '" + a.getTitulo() + "' de ese artista");
            }
        }
    }
}
