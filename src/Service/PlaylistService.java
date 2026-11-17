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
    private SesionService sesion;

    public void setSesion(SesionService sesion) { this.sesion = sesion; }

    private void exigirEdicionContenido() {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión autorizada.");
        sesion.exigirEdicionContenido();
    }


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
        exigirSesion();
        if (sesion.esAdministrador()) sesion.exigirEdicionContenido();
        if (p.getIdPropietario() != sesion.getUsuarioActual().getIdUsuario())
            throw new Excepciones.PermisoDenegadoException("La nueva playlist debe pertenecer al usuario actual.");
        validarDuplicado(p);
        int id = playlistDAO.insertar(p);
        if (id <= 0) {
            throw new ConexionBDException("No se pudo dar de alta la playlist");
        }
        p.setIdPlaylist(id);
        playlists.put(id, p);
        sesion.registrarAccion("Creó playlist ID " + id);
    }

    public void modificar(Playlist p)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        Playlist existente = buscarPorId(p.getIdPlaylist());
        exigirEdicion(existente);
        if (p.getIdPropietario() != existente.getIdPropietario())
            throw new Excepciones.PermisoDenegadoException("No se puede cambiar el propietario.");
        validarDuplicado(p);
        if (!playlistDAO.modificar(p)) {
            throw new ElementoNoEncontradoException("La playlist " + p.getIdPlaylist() + " no existe en la base");
        }
        existente.setNombre(p.getNombre());
        existente.setDescripcion(p.getDescripcion());
        existente.setPublica(p.isPublica());
        sesion.registrarAccion("Modificó playlist ID " + p.getIdPlaylist());
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException {
        exigirEdicion(buscarPorId(id));
        if (!playlistDAO.darDeBaja(id)) throw new ElementoNoEncontradoException("No existe la playlist " + id);
        playlists.remove(id);
        sesion.registrarAccion("Dio de baja playlist ID " + id);
    }

    public List<Playlist> listarInactivos() throws ConexionBDException {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión autorizada.");
        sesion.exigirAdministrador();
        return playlistDAO.listarInactivos();
    }

    public void reactivar(int id)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        exigirEdicionContenido();
        if (playlists.containsKey(id)) {
            throw new ElementoDuplicadoException("La playlist " + id + " ya esta activa");
        }
        Playlist p = playlistDAO.buscarPorId(id);
        if (p == null) {
            throw new ElementoNoEncontradoException("No existe la playlist con id " + id);
        }
        validarDuplicado(p);
        if (!playlistDAO.darDeAlta(id)) throw new ElementoNoEncontradoException("No existe la playlist " + id);
        p.setActivo(true);
        playlists.put(id, p);
        sesion.registrarAccion("Reactivó playlist ID " + id);
    }

    public void agregarCancion(int idPlaylist, int idCancion)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        Playlist p = buscarPorId(idPlaylist);
        exigirEdicion(p);
        Cancion c = cancionService.buscarPorId(idCancion);
        if (contieneCancion(p, idCancion)) {
            throw new ElementoDuplicadoException("La cancion '" + c.getTitulo() + "' ya esta en la playlist");
        }
        int posicion = p.getCanciones().size();
        playlistDAO.agregarCancion(idPlaylist, idCancion, posicion);
        p.agregarCancion(c);
        sesion.registrarAccion("Agregó canción ID " + idCancion + " a playlist ID " + idPlaylist);
    }

    public void quitarCancion(int idPlaylist, int idCancion)
            throws ConexionBDException, ElementoNoEncontradoException {
        Playlist p = buscarPorId(idPlaylist);
        exigirEdicion(p);
        if (!contieneCancion(p, idCancion)) {
            throw new ElementoNoEncontradoException("La cancion " + idCancion + " no esta en la playlist");
        }
        playlistDAO.quitarCancion(idPlaylist, idCancion);
        p.quitarCancion(idCancion);
        sesion.registrarAccion("Quitó canción ID " + idCancion + " de playlist ID " + idPlaylist);
    }


    public void moverCancion(int idPlaylist, int idCancion, int posicionDestino)
            throws ConexionBDException, ElementoNoEncontradoException {
        Playlist p = buscarPorId(idPlaylist);
        exigirEdicion(p);
        List<Cancion> orden = new ArrayList<>(p.getCanciones());
        int origen = -1;
        for (int i = 0; i < orden.size(); i++) if (orden.get(i).getIdCancion() == idCancion) origen = i;
        if (origen < 0) throw new ElementoNoEncontradoException("La canción no está en esta playlist.");
        if (posicionDestino < 0 || posicionDestino >= orden.size())
            throw new IllegalArgumentException("La posición de destino no existe en la playlist.");
        if (origen == posicionDestino) return;
        List<Integer> anterior = orden.stream().map(Cancion::getIdCancion).toList();
        Cancion movida = orden.remove(origen); orden.add(posicionDestino, movida);
        playlistDAO.reordenarCanciones(idPlaylist, anterior, orden.stream().map(Cancion::getIdCancion).toList());
        // La colección cambia recién después de confirmar la transacción.
        p.setCanciones(orden);
        sesion.registrarAccion("Reordenó canciones de playlist ID " + idPlaylist);
    }

    public Playlist buscarPorId(int id) throws ElementoNoEncontradoException {
        Playlist p = playlists.get(id);
        if (p == null) {
            throw new ElementoNoEncontradoException("No existe la playlist con id " + id);
        }
        exigirSesion();
        if (!puedeVer(p)) throw new Excepciones.PermisoDenegadoException("Esta playlist es privada.");
        return p;
    }

    private void exigirSesion() {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión.");
        sesion.exigirSesion();
    }

    public boolean puedeEditar(Playlist p) {
        exigirSesion();
        return p.isActivo() && (sesion.puedeEditarContenido() || (!sesion.esAdministrador()
                && p.getIdPropietario() == sesion.getUsuarioActual().getIdUsuario()));
    }

    private boolean puedeVer(Playlist p) { return p.isPublica() || sesion.puedeConsultarTodo() || puedeEditar(p); }

    private void exigirEdicion(Playlist p) {
        if (!puedeEditar(p)) throw new Excepciones.PermisoDenegadoException("No tenés permisos para modificar esta playlist.");
    }

    public List<Playlist> listar() {
        exigirSesion();
        List<Playlist> visibles = new ArrayList<>();
        for (Playlist p : playlists.values()) if (puedeVer(p)) visibles.add(p);
        return visibles;
    }

    public List<Playlist> listarPorUsuario(int idUsuario) {
        List<Playlist> resultado = new ArrayList<>();
        for (Playlist p : listar()) {
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

    public void recargar() throws ConexionBDException {
        List<Playlist> nuevos = playlistDAO.listarActivos();
        playlists.clear();
        for (Playlist item : nuevos) playlists.put(item.getIdPlaylist(), item);
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
