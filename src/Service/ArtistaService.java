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
    private SesionService sesion;

    public void setSesion(SesionService sesion) { this.sesion = sesion; }

    private void exigirEdicionContenido() {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión autorizada.");
        sesion.exigirEdicionContenido();
    }


    private ArtistaDAO artistaDAO = new ArtistaDAO();

    // Coleccion en memoria: id -> artista
    private Map<Integer, Artista> artistas = new LinkedHashMap<>();

    public ArtistaService() throws ConexionBDException {
        for (Artista a : artistaDAO.listarActivos()) {
            artistas.put(a.getIdArtista(), a);
        }
    }


    public void alta(Artista a) throws ConexionBDException, ElementoDuplicadoException {
        exigirEdicionContenido();
        validarDuplicado(a);
        int id = artistaDAO.insertar(a);
        if (id <= 0) {
            throw new ConexionBDException("No se pudo dar de alta el artista");
        }
        a.setIdArtista(id);
        artistas.put(id, a);
        sesion.registrarAccion("Creó artista ID " + id);
    }

    public void modificar(Artista a)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        exigirEdicionContenido();
        buscarPorId(a.getIdArtista());
        validarDuplicado(a);
        if (!artistaDAO.modificar(a)) {
            throw new ElementoNoEncontradoException("El artista " + a.getIdArtista() + " no existe en la base");
        }
        artistas.put(a.getIdArtista(), a);
        sesion.registrarAccion("Modificó artista ID " + a.getIdArtista());
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException {
        exigirEdicionContenido();
        buscarPorId(id);
        if (!artistaDAO.darDeBaja(id)) throw new ElementoNoEncontradoException("No existe el registro " + id);
        artistas.remove(id);
        sesion.registrarAccion("Dio de baja artista ID " + id);
    }


    public List<Artista> listarInactivos() throws ConexionBDException {
        if (sesion == null) throw new Excepciones.PermisoDenegadoException("No hay sesión autorizada.");
        sesion.exigirAdministrador();
        return artistaDAO.listarInactivos();
    }

    public void reactivar(int id)
            throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException {
        exigirEdicionContenido();
        if (artistas.containsKey(id)) {
            throw new ElementoDuplicadoException("El artista " + id + " ya esta activo");
        }
        Artista a = artistaDAO.buscarPorId(id);
        if (a == null) {
            throw new ElementoNoEncontradoException("No existe el artista con id " + id);
        }
        validarDuplicado(a);
        if (!artistaDAO.darDeAlta(id)) throw new ElementoNoEncontradoException("No existe el registro " + id);
        a.setActivo(true);
        artistas.put(id, a);
        sesion.registrarAccion("Reactivó artista ID " + id);
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


    public void recargar() throws ConexionBDException {
        List<Artista> nuevos = artistaDAO.listarActivos();
        artistas.clear();
        for (Artista item : nuevos) artistas.put(item.getIdArtista(), item);
    }

    private void validarDuplicado(Artista a) throws ElementoDuplicadoException {
        for (Artista otro : artistas.values()) {
            if (otro.getIdArtista() != a.getIdArtista() && otro.getNombre().equalsIgnoreCase(a.getNombre())) {
                throw new ElementoDuplicadoException("Ya existe el artista '" + a.getNombre() + "'");
            }
        }
    }
}
