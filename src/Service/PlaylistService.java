package Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import DAO.PlaylistDAO;
import Excepciones.ConexionBDException;
import Excepciones.ElementoDuplicadoException;
import Excepciones.ElementoNoEncontradoException;
import modelo.Cancion;
import modelo.Playlist;

public class PlaylistService {

    private PlaylistDAO playlistDAO = new PlaylistDAO();
    private CancionService cancionService;

    private Map<Integer, Playlist> playlists = new LinkedHashMap<>();

    public PlaylistService(CancionService cancionService) throws ConexionBDException {
        this.cancionService = cancionService;
        for (Playlist p : playlistDAO.listarActivos()) {
            playlists.put(p.getIdPlaylist(), p);
        }
    }


    public void alta(Playlist p) throws ConexionBDException, ElementoDuplicadoException {
        validarDuplicado(p);
        int id = playlistDAO.insertar(p);
        if (id <= 0) {
            throw new ConexionBDException("No se pudo dar de alta la playlist");
        }
        p.setIdPlaylist(id);
        playlists.put(id, p);
    }

    public void modificar(Playlist p)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        Playlist existente = buscarPorId(p.getIdPlaylist());
        validarDuplicado(p);
        if (!playlistDAO.modificar(p)) {
            throw new ElementoNoEncontradoException("La playlist " + p.getIdPlaylist() + " no existe en la base");
        }
        existente.setNombre(p.getNombre());
        existente.setDescripcion(p.getDescripcion());
        existente.setPublica(p.isPublica());
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException {
        buscarPorId(id);
        playlistDAO.darDeBaja(id);
        playlists.remove(id);
    }

    public void agregarCancion(int idPlaylist, int idCancion)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        Playlist p = buscarPorId(idPlaylist);
        Cancion c = cancionService.buscarPorId(idCancion);
        if (contieneCancion(p, idCancion)) {
            throw new ElementoDuplicadoException("La cancion '" + c.getTitulo() + "' ya esta en la playlist");
        }
        int posicion = p.getCanciones().size();
        playlistDAO.agregarCancion(idPlaylist, idCancion, posicion);
        p.agregarCancion(c);
    }

    public void quitarCancion(int idPlaylist, int idCancion)
            throws ConexionBDException, ElementoNoEncontradoException {
        Playlist p = buscarPorId(idPlaylist);
        if (!contieneCancion(p, idCancion)) {
            throw new ElementoNoEncontradoException("La cancion " + idCancion + " no esta en la playlist");
        }
        playlistDAO.quitarCancion(idPlaylist, idCancion);
        p.quitarCancion(idCancion);
    }


    public Playlist buscarPorId(int id) throws ElementoNoEncontradoException {
        Playlist p = playlists.get(id);
        if (p == null) {
            throw new ElementoNoEncontradoException("No existe la playlist con id " + id);
        }
        return p;
    }

    public List<Playlist> listar() {
        return new ArrayList<>(playlists.values());
    }

    public List<Playlist> listarPorUsuario(int idUsuario) {
        List<Playlist> resultado = new ArrayList<>();
        for (Playlist p : playlists.values()) {
            if (p.getIdPropietario() == idUsuario) {
                resultado.add(p);
            }
        }
        return resultado;
    }


    private boolean contieneCancion(Playlist p, int idCancion) {
        for (Cancion c : p.getCanciones()) {
            if (c.getIdCancion() == idCancion) {
                return true;
            }
        }
        return false;
    }

    private void validarDuplicado(Playlist p) throws ElementoDuplicadoException {
        for (Playlist otra : playlists.values()) {
            if (otra.getIdPlaylist() != p.getIdPlaylist()
                    && otra.getIdPropietario() == p.getIdPropietario()
                    && otra.getNombre().equalsIgnoreCase(p.getNombre())) {
                throw new ElementoDuplicadoException("Ya tenes una playlist llamada '" + p.getNombre() + "'");
            }
        }
    }
}
