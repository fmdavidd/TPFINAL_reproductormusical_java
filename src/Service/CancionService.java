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

    private CancionDAO cancionDAO = new CancionDAO();

    // guardamos las activas en memoria para no consultar la BD en cada busqueda
    private Map<Integer, Cancion> canciones = new LinkedHashMap<>();

    public CancionService() throws ConexionBDException {
        for (Cancion c : cancionDAO.listarActivos()) {
            canciones.put(c.getIdCancion(), c);
        }
    }


    public void alta(Cancion c) throws ConexionBDException, ElementoDuplicadoException {
        validarDuplicado(c);
        int id = cancionDAO.insertar(c);
        if (id <= 0) {
            throw new ConexionBDException("No se pudo dar de alta la cancion");
        }
        c.setIdCancion(id);
        canciones.put(id, c);
    }

    public void modificar(Cancion c)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        buscarPorId(c.getIdCancion());
        validarDuplicado(c);
        if (!cancionDAO.modificar(c)) {
            throw new ElementoNoEncontradoException("La cancion " + c.getIdCancion() + " no existe en la base");
        }
        canciones.put(c.getIdCancion(), c);
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException {
        buscarPorId(id);
        cancionDAO.darDeBaja(id);
        canciones.remove(id);
    }


    // Lista las canciones dadas de baja (activo = FALSE) para poder elegir cual reactivar.
    public List<Cancion> listarInactivos() throws ConexionBDException {
        return cancionDAO.listarInactivos();
    }

    public void reactivar(int id)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        if (canciones.containsKey(id)) {
            throw new ElementoDuplicadoException("La cancion " + id + " ya esta activa");
        }
        Cancion c = cancionDAO.buscarPorId(id);
        if (c == null) {
            throw new ElementoNoEncontradoException("No existe la cancion con id " + id);
        }
        // primero hay que recuperar el artista y el album, si la cancion tiene uno
        if (!c.getArtista().isActivo()) {
            throw new ElementoNoEncontradoException("El artista de la cancion esta dado de baja: reactivalo primero");
        }
        if (c.getAlbum() != null && !c.getAlbum().isActivo()) {
            throw new ElementoNoEncontradoException("El album de la cancion esta dado de baja: reactivalo primero");
        }
        validarDuplicado(c);
        cancionDAO.darDeAlta(id);
        c.setActivo(true);
        canciones.put(id, c);
    }


    public Cancion buscarPorId(int id) throws ElementoNoEncontradoException {
        Cancion c = canciones.get(id);
        if (c == null) {
            throw new ElementoNoEncontradoException("No existe la cancion con id " + id);
        }
        return c;
    }

    // devolvemos otra lista para que la vista no pueda vaciar la coleccion del service
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
