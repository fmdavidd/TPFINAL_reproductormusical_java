package Service;

import DAO.PlaylistDAO;
import Excepciones.*;
import modelo.*;
import java.util.ArrayList;
import java.util.List;

public class PlaylistService {
    private final PlaylistDAO dao = new PlaylistDAO();
    private final SesionService sesion;
    private final CancionService canciones;

    public PlaylistService(CancionService canciones) throws ConexionBDException {
        this(null, canciones);
    }
    public PlaylistService(SesionService sesion, CancionService canciones) {
        this.sesion = sesion;
        this.canciones = canciones;
    }

    public void alta(Playlist p) throws ConexionBDException, ElementoDuplicadoException, PermisoDenegadoException {
        if (p == null || p.getIdPlaylist() != 0) throw new IllegalArgumentException("Un alta no debe tener ID asignado.");
        validar(p);
        if (!p.getCanciones().isEmpty()) throw new IllegalArgumentException("Cree la playlist vacia y agregue las canciones luego.");
        int id = dao.insertar(p);
        if (id <= 0) throw new ConexionBDException("No se pudo crear la playlist.");
        p.setIdPlaylist(id);
    }

    public void modificar(Playlist p) throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException, PermisoDenegadoException {
        if (p == null) throw new IllegalArgumentException("Entidad requerida.");
        propia(p.getIdPlaylist());
        validar(p);
        Acceso.resultado(dao.modificar(p));
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        propia(id);
        Acceso.resultado(dao.darDeBaja(id));
    }

    public void agregarCancion(int id, int idCancion) throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException, PermisoDenegadoException {
        Playlist p = propia(id);
        canciones.buscarPorId(idCancion);
        for (Cancion c : p.getCanciones())
            if (c.getIdCancion() == idCancion) throw new ElementoDuplicadoException("La cancion ya esta en la playlist.");
        dao.agregarCancion(id, idCancion, p.getCanciones().size());
    }

    public void quitarCancion(int id, int idCancion) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Playlist p = propia(id);
        boolean existe = false;
        for (Cancion c : p.getCanciones()) if (c.getIdCancion() == idCancion) existe = true;
        if (!existe) throw new ElementoNoEncontradoException("La cancion no esta en la playlist.");
        dao.quitarCancion(id, idCancion);
    }

    // Posiciones desde cero dentro del servicio; la consola muestra desde uno.
    public void moverCancion(int id, int origen, int destino) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Playlist p = propia(id);
        p.moverCancion(origen, destino);
        dao.guardarOrden(id, p.getCanciones());
    }

    public Playlist buscarPorId(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Usuario u = Acceso.autenticado(sesion);
        Playlist p = id <= 0 ? null : dao.buscarPorId(id);
        if (p == null || !p.isActivo()) throw new ElementoNoEncontradoException("No existe playlist activa con id " + id);
        if (!p.isPublica() && p.getIdPropietario() != u.getIdUsuario()) throw new PermisoDenegadoException("La playlist es privada.");
        return p;
    }

    public List<Playlist> listar() throws ConexionBDException, PermisoDenegadoException {
        Usuario u = Acceso.autenticado(sesion);
        List<Playlist> lista = new ArrayList<>();
        for (Playlist p : dao.listarActivos())
            if (p.isPublica() || p.getIdPropietario() == u.getIdUsuario()) lista.add(p);
        return lista;
    }

    public List<Playlist> listarPorUsuario(int id) throws ConexionBDException, PermisoDenegadoException {
        List<Playlist> lista = new ArrayList<>();
        for (Playlist p : listar()) if (p.getIdPropietario() == id) lista.add(p);
        return lista;
    }

    private Playlist propia(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Playlist p = buscarPorId(id);
        Acceso.propietario(sesion, p.getIdPropietario());
        return p;
    }

    private void validar(Playlist p) throws ConexionBDException, ElementoDuplicadoException, PermisoDenegadoException {
        if (p == null || !p.isActivo()) throw new IllegalArgumentException("Playlist activa requerida.");
        Usuario u = Acceso.autenticado(sesion);
        if (!(u instanceof UsuarioNormal)) throw new PermisoDenegadoException("Las playlists corresponden a usuarios normales.");
        Acceso.propietario(sesion, p.getIdPropietario());
        Acceso.texto(p.getNombre(), 80, "Nombre");
        if (p.getDescripcion() != null && p.getDescripcion().length() > 200) throw new IllegalArgumentException("Descripcion demasiado larga.");
        for (Playlist otra : dao.listarPorUsuario(u.getIdUsuario()))
            if (otra.getIdPlaylist() != p.getIdPlaylist() && otra.getNombre().trim().equalsIgnoreCase(p.getNombre().trim()))
                throw new ElementoDuplicadoException("Ya tiene una playlist con ese nombre.");
    }
}
