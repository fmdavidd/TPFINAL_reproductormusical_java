package Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import DAO.PlaylistDAO;
import Excepciones.ConexionBDException;
import Excepciones.ElementoDuplicadoException;
import Excepciones.ElementoNoEncontradoException;
import Excepciones.LimitePlaylistsException;
import modelo.Cancion;
import modelo.Playlist;
import modelo.Usuario;
import modelo.UsuarioNormal;

public class PlaylistService {

    public static final int LIMITE_PLAYLISTS_SIN_PREMIUM = 3;

    private PlaylistDAO playlistDAO = new PlaylistDAO();
    private CancionService cancionService;
    private UsuarioService usuarioService;

    private Map<Integer, Playlist> playlists = new LinkedHashMap<>();

    public PlaylistService(CancionService cancionService, UsuarioService usuarioService)
            throws ConexionBDException {
        this.cancionService = cancionService;
        this.usuarioService = usuarioService;
        for (Playlist p : playlistDAO.listarActivos()) {
            playlists.put(p.getIdPlaylist(), p);
        }
    }


    public void alta(Playlist p)
            throws ConexionBDException, ElementoDuplicadoException,
            ElementoNoEncontradoException, LimitePlaylistsException {
        validarDuplicado(p);
        validarLimite(p);
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

    public List<Playlist> listarInactivos() throws ConexionBDException {
        return playlistDAO.listarInactivos();
    }

    public void reactivar(int id)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        if (playlists.containsKey(id)) {
            throw new ElementoDuplicadoException("La playlist " + id + " ya esta activa");
        }
        Playlist p = playlistDAO.buscarPorId(id);
        if (p == null) {
            throw new ElementoNoEncontradoException("No existe la playlist con id " + id);
        }
        validarDuplicado(p);
        playlistDAO.darDeAlta(id);
        p.setActivo(true);
        playlists.put(id, p);
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


    public void moverCancion(int idPlaylist, int origen, int destino)
            throws ConexionBDException, ElementoNoEncontradoException {
        Playlist p = buscarPorId(idPlaylist);
        p.moverCancion(origen, destino);
        try {
            playlistDAO.guardarOrden(idPlaylist, p.getCanciones());
        } catch (ConexionBDException e) {
            // si falla la BD, deshacemos el movimiento tb en memoria
            p.moverCancion(destino, origen);
            throw e;
        }
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

    // el limite de playlists depende del propietario: solo aplica a cuentas
    // normales sin premium
    private void validarLimite(Playlist p)
            throws ElementoNoEncontradoException, LimitePlaylistsException {
        Usuario propietario = usuarioService.buscarPorId(p.getIdPropietario());
        if (!(propietario instanceof UsuarioNormal)) {
            return;
        }
        UsuarioNormal normal = (UsuarioNormal) propietario;
        if (normal.esPremium()) {
            return;
        }
        if (listarPorUsuario(normal.getIdUsuario()).size() >= LIMITE_PLAYLISTS_SIN_PREMIUM) {
            throw new LimitePlaylistsException("Las cuentas no premium pueden crear hasta "
                    + LIMITE_PLAYLISTS_SIN_PREMIUM
                    + " playlists. Las cuentas premium pueden crear playlists ilimitadas.");
        }
    }

    // el nombre puede repetirse entre usuarios, pero no dentro de la misma cuenta
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
