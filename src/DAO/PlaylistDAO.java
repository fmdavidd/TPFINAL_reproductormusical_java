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

    public void agregarCancion(int idPlaylist, int idCancion, int posicion) throws ConexionBDException {
        ejecutarUpdate(
                "INSERT INTO playlist_cancion (id_playlist, id_cancion, posicion) VALUES (?, ?, ?)",
                idPlaylist, idCancion, posicion);
    }

    public void quitarCancion(int idPlaylist, int idCancion) throws ConexionBDException {
        ejecutarUpdate("UPDATE playlist_cancion SET posicion = posicion - 1 WHERE id_playlist = ? AND posicion > "
                        + "(SELECT posicion FROM (SELECT posicion FROM playlist_cancion "
                        + "WHERE id_playlist = ? AND id_cancion = ?) AS actual)",
                idPlaylist, idPlaylist, idCancion);
        ejecutarUpdate("DELETE FROM playlist_cancion WHERE id_playlist = ? AND id_cancion = ?",
                idPlaylist, idCancion);
    }

    private List<Cancion> cargarCanciones(int idPlaylist) throws ConexionBDException {
        List<Cancion> canciones = new ArrayList<>();
        String sql = "SELECT c.* FROM cancion c "
                + "JOIN playlist_cancion pc ON pc.id_cancion = c.id_cancion "
                + "WHERE pc.id_playlist = ? AND c.activo = TRUE "
                + "ORDER BY pc.posicion";
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