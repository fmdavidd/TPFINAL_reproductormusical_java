package Service;

import DAO.*;
import Excepciones.*;
import modelo.*;
import java.util.ArrayList;
import java.util.List;

public class ArtistaService {
    private final ArtistaDAO dao = new ArtistaDAO();
    private final SesionService sesion;
    public ArtistaService() throws ConexionBDException { this((SesionService) null); }
    public ArtistaService(SesionService sesion) { this.sesion = sesion; }

    public void alta(Artista a) throws ConexionBDException, ElementoDuplicadoException, PermisoDenegadoException {
        Acceso.administrador(sesion, false);
        if (a == null || a.getIdArtista() != 0) throw new IllegalArgumentException("Un alta no debe tener ID asignado.");
        validar(a);
        int id = dao.insertar(a);
        if (id <= 0) throw new ConexionBDException("No se pudo crear artista.");
        a.setIdArtista(id);
    }

    public void modificar(Artista a) throws ConexionBDException, ElementoDuplicadoException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.administrador(sesion, false);
        if (a == null) throw new IllegalArgumentException("Entidad requerida.");
        buscarPorId(a.getIdArtista());
        validar(a);
        Acceso.resultado(dao.modificar(a));
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.administrador(sesion, false);
        buscarPorId(id);
        for (Album a : new AlbumDAO().listarActivos())
            if (a.getArtista().getIdArtista() == id) throw new IllegalArgumentException("Primero de de baja los albumes del artista.");
        for (Cancion c : new CancionDAO().listarActivos())
            if (c.getArtista().getIdArtista() == id) throw new IllegalArgumentException("Primero de de baja las canciones del artista.");
        Acceso.resultado(dao.darDeBaja(id));
    }

    public Artista buscarPorId(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.autenticado(sesion);
        Artista a = id <= 0 ? null : dao.buscarPorId(id);
        if (a == null || !a.isActivo()) throw new ElementoNoEncontradoException("No existe artista activo con id " + id);
        return a;
    }

    public List<Artista> listar() throws ConexionBDException, PermisoDenegadoException {
        Acceso.autenticado(sesion);
        return dao.listarActivos();
    }

    private void validar(Artista a) throws ConexionBDException, ElementoDuplicadoException {
        if (a == null || !a.isActivo()) throw new IllegalArgumentException("Se requiere una entidad activa.");
        Acceso.texto(a.getNombre(), 80, "Nombre o titulo");

        Acceso.texto(a.getPaisOrigen(), 50, "Pais");
        for (Artista otro : dao.listarActivos()) {
            if (otro.getIdArtista() != a.getIdArtista() && otro.getNombre().trim().equalsIgnoreCase(a.getNombre().trim()))
                throw new ElementoDuplicadoException("Ya existe artista con ese nombre.");
        }
    }
    public List<Artista> buscarPorNombre(String texto) throws ConexionBDException, PermisoDenegadoException {
        List<Artista> resultado = new ArrayList<>();
        for (Artista a : listar()) if (a.getNombre().toLowerCase().contains(texto.toLowerCase())) resultado.add(a);
        return resultado;
    }
}
