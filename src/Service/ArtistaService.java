package Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import DAO.ArtistaDAO;
import Excepciones.ConexionBDException;
import Excepciones.ElementoDuplicadoException;
import Excepciones.ElementoNoEncontradoException;
import modelo.Artista;

public class ArtistaService {

    private ArtistaDAO artistaDAO = new ArtistaDAO();

    // Coleccion en memoria: id -> artista
    private Map<Integer, Artista> artistas = new LinkedHashMap<>();

    public ArtistaService() throws ConexionBDException {
        for (Artista a : artistaDAO.listarActivos()) {
            artistas.put(a.getIdArtista(), a);
        }
    }


    public void alta(Artista a) throws ConexionBDException, ElementoDuplicadoException {
        validarDuplicado(a);
        int id = artistaDAO.insertar(a);
        if (id <= 0) {
            throw new ConexionBDException("No se pudo dar de alta el artista");
        }
        a.setIdArtista(id);
        artistas.put(id, a);
    }

    public void modificar(Artista a)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        buscarPorId(a.getIdArtista());
        validarDuplicado(a);
        if (!artistaDAO.modificar(a)) {
            throw new ElementoNoEncontradoException("El artista " + a.getIdArtista() + " no existe en la base");
        }
        artistas.put(a.getIdArtista(), a);
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException {
        buscarPorId(id);
        artistaDAO.darDeBaja(id);
        artistas.remove(id);
    }


    public Artista buscarPorId(int id) throws ElementoNoEncontradoException {
        Artista a = artistas.get(id);
        if (a == null) {
            throw new ElementoNoEncontradoException("No existe el artista con id " + id);
        }
        return a;
    }

    public List<Artista> listar() {
        return new ArrayList<>(artistas.values());
    }

    public List<Artista> buscarPorNombre(String texto) {
        List<Artista> resultado = new ArrayList<>();
        for (Artista a : artistas.values()) {
            if (a.getNombre().toLowerCase().contains(texto.toLowerCase())) {
                resultado.add(a);
            }
        }
        return resultado;
    }


    private void validarDuplicado(Artista a) throws ElementoDuplicadoException {
        for (Artista otro : artistas.values()) {
            if (otro.getIdArtista() != a.getIdArtista() && otro.getNombre().equalsIgnoreCase(a.getNombre())) {
                throw new ElementoDuplicadoException("Ya existe el artista '" + a.getNombre() + "'");
            }
        }
    }
}
