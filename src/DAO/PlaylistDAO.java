package DAO;
import Conexion.ConexionBD;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import Excepciones.ConexionBDException;
import modelo.Cancion;
import modelo.Playlist;

public class PlaylistDAO extends GenericDAO<Playlist> {

    private final CancionDAO cancionDAO = new CancionDAO();

    @Override
    protected String getNombreTabla() {
        return "playlist";
    }

    @Override
    protected String getNombreColumnaId() {
        return "id_playlist";
    }

    @Override
    public int insertar(Playlist p) throws ConexionBDException {
        int id = ejecutarInsert(
                "INSERT INTO playlist (nombre, descripcion, publica, id_usuario) VALUES (?, ?, ?, ?)",
                p.getNombre(), p.getDescripcion(), p.isPublica(), p.getIdPropietario());
        if (id > 0 && p.getCanciones() != null) {
            int posicion = 0;
            for (Cancion c : p.getCanciones()) {
                agregarCancion(id, c.getIdCancion(), posicion++);
            }
        }
        return id;
    }

    @Override
    public boolean modificar(Playlist p) throws ConexionBDException {
        return ejecutarUpdate(
                "UPDATE playlist SET nombre = ?, descripcion = ?, publica = ? WHERE id_playlist = ?",
                p.getNombre(), p.getDescripcion(), p.isPublica(), p.getIdPlaylist()) > 0;
    }

    public List<Playlist> listarPorUsuario(int id) throws ConexionBDException {
        return consultar("SELECT * FROM playlist WHERE id_usuario = ? AND activo = TRUE ORDER BY id_playlist", id);
    }

    public void agregarCancion(int idPlaylist, int idCancion, int posicion) throws ConexionBDException {
        limpiarOrden(idPlaylist);
        if (ejecutarUpdate("INSERT INTO playlist_cancion (id_playlist, id_cancion, posicion) "
                + "SELECT ?, ?, COALESCE(MAX(posicion) + 1, 0) FROM playlist_cancion WHERE id_playlist = ?",
                idPlaylist, idCancion, idPlaylist) != 1) throw new ConexionBDException("No se pudo agregar la cancion.");
    }

    public void quitarCancion(int idPlaylist, int idCancion) throws ConexionBDException {
        if (ejecutarUpdate("DELETE FROM playlist_cancion WHERE id_playlist = ? AND id_cancion = ?", idPlaylist, idCancion) == 0)
            throw new ConexionBDException("La cancion ya no pertenece a la playlist.");
        limpiarOrden(idPlaylist);
    }

    // Cada sentencia usa autocommit. Si se interrumpe una secuencia, la proxima
    // modificacion normaliza las posiciones; las lecturas siempre tienen desempate.
    private void limpiarOrden(int id) throws ConexionBDException {
        guardarOrden(id, cargarCanciones(id));
    }

    public void limpiarCancionInactiva(int idCancion) throws ConexionBDException {
        List<Integer> ids = new ArrayList<>();
        try (java.sql.Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id_playlist FROM playlist_cancion WHERE id_cancion = ?")) {
            ps.setInt(1, idCancion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("id_playlist"));
            }
        } catch (SQLException e) {
            throw new ConexionBDException("No se pudieron consultar las playlists afectadas: " + e.getMessage());
        }
        for (int id : ids) limpiarOrden(id);
    }

    public void guardarOrden(int id, List<Cancion> canciones) throws ConexionBDException {
        ejecutarUpdate("DELETE pc FROM playlist_cancion pc JOIN cancion c ON c.id_cancion = pc.id_cancion "
                + "JOIN artista ar ON ar.id_artista = c.id_artista LEFT JOIN album al ON al.id_album = c.id_album "
                + "WHERE pc.id_playlist = ? AND (c.activo = FALSE OR ar.activo = FALSE "
                + "OR (c.id_album IS NOT NULL AND (al.activo = FALSE OR al.id_artista <> c.id_artista)))", id);
        if (canciones.isEmpty()) return;
        String sql = "UPDATE playlist_cancion SET posicion = CASE id_cancion";
        List<Object> parametros = new ArrayList<>();
        for (int i = 0; i < canciones.size(); i++) {
            sql += " WHEN ? THEN ?";
            parametros.add(canciones.get(i).getIdCancion());
            parametros.add(i);
        }
        sql += " ELSE posicion END WHERE id_playlist = ?";
        parametros.add(id);
        if (ejecutarUpdate(sql, parametros.toArray()) == 0)
            throw new ConexionBDException("No se pudo confirmar el orden de la playlist.");
    }

    private List<Cancion> cargarCanciones(int idPlaylist) throws ConexionBDException {
        List<Cancion> canciones = new ArrayList<>();
        String sql = "SELECT c.* FROM cancion c "
                + "JOIN playlist_cancion pc ON pc.id_cancion = c.id_cancion "
                + "JOIN artista ar ON ar.id_artista = c.id_artista LEFT JOIN album al ON al.id_album = c.id_album "
                + "WHERE pc.id_playlist = ? AND c.activo = TRUE AND ar.activo = TRUE "
                + "AND (c.id_album IS NULL OR (al.activo = TRUE AND al.id_artista = c.id_artista)) "
                + "ORDER BY pc.posicion, pc.id_cancion";
        try (java.sql.Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPlaylist);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    canciones.add(cancionDAO.mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al cargar canciones de la playlist " + idPlaylist + " - " + e.getMessage());
        }
        return canciones;
    }

    @Override
    public Playlist mapear(ResultSet rs) throws SQLException, ConexionBDException {
        Playlist p = new Playlist();
        p.setIdPlaylist(rs.getInt("id_playlist"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPublica(rs.getBoolean("publica"));
        p.setIdPropietario(rs.getInt("id_usuario"));
        p.setActivo(rs.getBoolean("activo"));
        p.setCanciones(cargarCanciones(p.getIdPlaylist()));
        return p;
    }
}