package Service;

import DAO.*;
import Enumeraciones.NivelAdmin;
import Excepciones.*;
import modelo.*;
import java.util.List;

public class UsuarioService {
    private final UsuarioDAO dao = new UsuarioDAO();
    private SesionService sesion;

    public UsuarioService() throws ConexionBDException { }
    void setSesion(SesionService sesion) { this.sesion = sesion; }
    Usuario autenticar(String email) throws ConexionBDException { return dao.buscarPorEmail(email); }
    Usuario cargarSesion(int id) throws ConexionBDException { return dao.buscarPorId(id); }

    public void alta(Usuario u) throws ConexionBDException, ElementoDuplicadoException, PermisoDenegadoException {
        Acceso.administrador(sesion, true);
        insertar(u);
    }

    void registrarNormal(UsuarioNormal u) throws ConexionBDException, ElementoDuplicadoException {
        insertar(u);
    }

    private void insertar(Usuario u) throws ConexionBDException, ElementoDuplicadoException {
        if (u == null || u.getIdUsuario() != 0) throw new IllegalArgumentException("Un alta no debe tener ID asignado.");
        validar(u);
        int id = dao.insertar(u);
        if (id <= 0) throw new ConexionBDException("No se pudo crear el usuario.");
        u.setIdUsuario(id);
    }

    public void modificar(Usuario u) throws ConexionBDException, ElementoNoEncontradoException, ElementoDuplicadoException, PermisoDenegadoException {
        Acceso.administrador(sesion, true);
        if (u == null) throw new IllegalArgumentException("Entidad requerida.");
        Usuario anterior = buscarPorId(u.getIdUsuario());
        if (anterior.getRol() != u.getRol()) throw new IllegalArgumentException("No se puede cambiar el tipo de usuario.");
        if (u instanceof UsuarioAdministrador && u.getIdUsuario() == Acceso.autenticado(sesion).getIdUsuario()
                && ((UsuarioAdministrador) u).getNivelAdmin() != ((UsuarioAdministrador) anterior).getNivelAdmin())
            throw new IllegalArgumentException("No puede cambiar su propio nivel.");
        validar(u);
        Acceso.resultado(dao.modificar(u));
    }

    public void baja(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.administrador(sesion, true);
        buscarPorId(id);
        if (Acceso.autenticado(sesion).getIdUsuario() == id) throw new IllegalArgumentException("No puede dar de baja su propia sesion.");
        for (Playlist playlist : new PlaylistDAO().listarActivos())
            if (playlist.getIdPropietario() == id) throw new IllegalArgumentException("El usuario debe dar de baja sus playlists primero.");
        Acceso.resultado(dao.darDeBaja(id));
    }

    public void cambiarNivelAdmin(int id, NivelAdmin nivel) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.administrador(sesion, true);
        Usuario u = buscarPorId(id);
        if (!(u instanceof UsuarioAdministrador) || nivel == null) throw new IllegalArgumentException("Administrador o nivel invalido.");
        if (Acceso.autenticado(sesion).getIdUsuario() == id) throw new IllegalArgumentException("No puede cambiar su propio nivel.");
        ((UsuarioAdministrador) u).setNivelAdmin(nivel);
        Acceso.resultado(dao.modificar(u));
    }

    public boolean agregarFavorito(int idUsuario, int idCancion) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        obtenerNormal(idUsuario);
        new CancionService(sesion).buscarPorId(idCancion);
        return dao.agregarFavorito(idUsuario, idCancion);
    }

    public boolean quitarFavorito(int idUsuario, int idCancion) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        obtenerNormal(idUsuario);
        return dao.quitarFavorito(idUsuario, idCancion);
    }

    public void registrarAccion(int idAdmin, String accion) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.administrador(sesion, false);
        Acceso.propietario(sesion, idAdmin);
        Acceso.texto(accion, 200, "Accion");
        dao.registrarAccion(idAdmin, accion);
    }

    public Usuario buscarPorId(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Usuario actual = Acceso.autenticado(sesion);
        if (actual.getIdUsuario() != id && !actual.puedeAdministrar()) throw new PermisoDenegadoException("Solo puede consultar su usuario.");
        Usuario u = id <= 0 ? null : dao.buscarPorId(id);
        if (u == null || !u.isActivo()) throw new ElementoNoEncontradoException("No existe usuario activo con id " + id);
        return datosVisibles(u, actual.getIdUsuario());
    }

    public Usuario buscarPorEmail(String email) throws ConexionBDException, PermisoDenegadoException {
        Usuario actual = Acceso.autenticado(sesion);
        if (!actual.puedeAdministrar() && !actual.getEmail().equalsIgnoreCase(email)) throw new PermisoDenegadoException("Solo puede consultar su usuario.");
        Usuario u = dao.buscarPorEmail(email);
        return u != null && u.isActivo() ? datosVisibles(u, actual.getIdUsuario()) : null;
    }

    public List<Usuario> listar() throws ConexionBDException, PermisoDenegadoException {
        Usuario actual = Acceso.autenticado(sesion);
        if (!actual.puedeAdministrar()) throw new PermisoDenegadoException("Requiere administrador.");
        List<Usuario> lista = dao.listarActivos();
        for (Usuario u : lista) datosVisibles(u, actual.getIdUsuario());
        return lista;
    }

    // Consultar usuarios tampoco permite saltarse la privacidad de PlaylistService.
    private Usuario datosVisibles(Usuario u, int idSolicitante) {
        if (u instanceof UsuarioNormal && u.getIdUsuario() != idSolicitante) {
            UsuarioNormal normal = (UsuarioNormal) u;
            List<Playlist> publicas = new java.util.ArrayList<>();
            for (Playlist p : normal.getPlaylists()) if (p.isPublica()) publicas.add(p);
            normal.setPlaylists(publicas);
            normal.setFavoritos(new java.util.HashSet<Integer>());
        }
        return u;
    }

    private UsuarioNormal obtenerNormal(int id) throws ConexionBDException, ElementoNoEncontradoException, PermisoDenegadoException {
        Acceso.propietario(sesion, id);
        Usuario u = buscarPorId(id);
        if (!(u instanceof UsuarioNormal)) throw new IllegalArgumentException("Los favoritos corresponden a usuarios normales.");
        return (UsuarioNormal) u;
    }

    private void validar(Usuario u) throws ConexionBDException, ElementoDuplicadoException {
        if (u == null || !u.isActivo()) throw new IllegalArgumentException("Usuario activo requerido.");
        Acceso.texto(u.getNombre(), 60, "Nombre");
        Acceso.texto(u.getEmail(), 100, "Email");
        if (!u.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Formato de email invalido.");
        Acceso.texto(u.getHashContrasenia(), 128, "Hash");
        Acceso.texto(u.getSalt(), 64, "Salt");
        if (u instanceof UsuarioAdministrador && ((UsuarioAdministrador) u).getNivelAdmin() == null) throw new IllegalArgumentException("Nivel requerido.");
        Usuario otro = dao.buscarPorEmail(u.getEmail());
        if (otro != null && otro.getIdUsuario() != u.getIdUsuario())
            throw new ElementoDuplicadoException("Email ya registrado, incluso si la cuenta esta inactiva. Utilice otro email.");
    }
}
