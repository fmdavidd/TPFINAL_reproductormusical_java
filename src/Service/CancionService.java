package Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import DAO.CancionDAO;
import Enumeraciones.Genero;
import Excepciones.ConexionBDException;
import Excepciones.ElementoDuplicadoException;
import Excepciones.ElementoNoEncontradoException;
import modelo.Cancion;

public class CancionService {
    private SesionService sesion;

    public void setSesion(SesionService sesion) { this.sesion = sesion; }

    private void exigirEdicionContenido() {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión autorizada.");
        sesion.exigirEdicionContenido();
    }


    private CancionDAO cancionDAO = new CancionDAO();

    private Map<Integer, Cancion> canciones = new LinkedHashMap<>();

    public CancionService() throws ConexionBDException {
        for (Cancion c : cancionDAO.listarActivos()) {
            canciones.put(c.getIdCancion(), c);
        }
    }


    public void alta(Cancion c) throws ConexionBDException, ElementoDuplicadoException {
        exigirEdicionContenido();
        validarDuplicado(c);
        int id = cancionDAO.insertar(c);
        if (id <= 0) {
            throw new ConexionBDException("No se pudo dar de alta la cancion");
        }
        c.setIdCancion(id);
        canciones.put(id, c);
        sesion.registrarAccion("Creó canción ID " + id);
    }

    public void modificar(Cancion c)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        exigirEdicionContenido();
        buscarPorId(c.getIdCancion());
        validarDuplicado(c);
        if (!cancionDAO.modificar(c)) {
            throw new ElementoNoEncontradoException("La cancion " + c.getIdCancion() + " no existe en la base");
        }
        canciones.put(c.getIdCancion(), c);
        sesion.registrarAccion("Modificó canción ID " + c.getIdCancion());
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException {
        exigirEdicionContenido();
        buscarPorId(id);
        if (!cancionDAO.darDeBaja(id)) throw new ElementoNoEncontradoException("No existe el registro " + id);
        canciones.remove(id);
        sesion.registrarAccion("Dio de baja canción ID " + id);
    }


    // Lista las canciones dadas de baja (activo = FALSE) para poder elegir cual reactivar.
    public List<Cancion> listarInactivos() throws ConexionBDException {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión autorizada.");
        sesion.exigirAdministrador();
        return cancionDAO.listarInactivos();
    }

    public void reactivar(int id)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        exigirEdicionContenido();
        if (canciones.containsKey(id)) {
            throw new ElementoDuplicadoException("La cancion " + id + " ya esta activa");
        }
        Cancion c = cancionDAO.buscarPorId(id);
        if (c == null) {
            throw new ElementoNoEncontradoException("No existe la cancion con id " + id);
        }
        if (!c.getArtista().isActivo()) {
            throw new ElementoNoEncontradoException("El artista de la cancion esta dado de baja: reactivalo primero");
        }
        if (c.getAlbum() != null && !c.getAlbum().isActivo()) {
            throw new ElementoNoEncontradoException("El album de la cancion esta dado de baja: reactivalo primero");
        }
        validarDuplicado(c);
        if (!cancionDAO.darDeAlta(id)) throw new ElementoNoEncontradoException("No existe el registro " + id);
        c.setActivo(true);
        canciones.put(id, c);
        sesion.registrarAccion("Reactivó canción ID " + id);
    }


    public Cancion buscarPorId(int id) throws ElementoNoEncontradoException {
        Cancion c = canciones.get(id);
        if (c == null) {
            throw new ElementoNoEncontradoException("No existe la cancion con id " + id);
        }
        return c;
    }

    public List<Cancion> listar() {
        return new ArrayList<>(canciones.values());
    }

    public List<Cancion> buscarPorTitulo(String texto) {
        List<Cancion> resultado = new ArrayList<>();
        for (Cancion c : canciones.values()) {
            if (c.getTitulo().toLowerCase().contains(texto.toLowerCase())) {
                resultado.add(c);
            }
        }
        return resultado;
    }

    public List<Cancion> buscarPorGenero(Genero genero) {
        List<Cancion> resultado = new ArrayList<>();
        for (Cancion c : canciones.values()) {
            if (c.getGenero() == genero) {
                resultado.add(c);
            }
        }
        return resultado;
    }


    public void recargar() throws ConexionBDException {
        List<Cancion> nuevos = cancionDAO.listarActivos();
        canciones.clear();
        for (Cancion item : nuevos) canciones.put(item.getIdCancion(), item);
    }

    private void validarDuplicado(Cancion c) throws ElementoDuplicadoException {
        for (Cancion otra : canciones.values()) {
            if (otra.getIdCancion() != c.getIdCancion()
                    && otra.getTitulo().equalsIgnoreCase(c.getTitulo())
                    && otra.getArtista().getIdArtista() == c.getArtista().getIdArtista()) {
                throw new ElementoDuplicadoException(
                        "Ya existe la cancion '" + c.getTitulo() + "' de ese artista");
            }
        }
    }
}
