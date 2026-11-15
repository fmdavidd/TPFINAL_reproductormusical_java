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
