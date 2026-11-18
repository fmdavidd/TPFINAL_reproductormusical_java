package DAO;
import Conexion.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        String sql = "INSERT INTO playlist (nombre, descripcion, publica, id_usuario) VALUES (?, ?, ?, ?)";
        int id = -1;
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setBoolean(3, p.isPublica());
            ps.setInt(4, p.getIdPropietario());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al insertar playlist", e);
        }
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
        String sql = "UPDATE playlist SET nombre = ?, descripcion = ?, publica = ? WHERE id_playlist = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setBoolean(3, p.isPublica());
            ps.setInt(4, p.getIdPlaylist());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ConexionBDException("Error al modificar playlist", e);
        }
    }

    public void agregarCancion(int idPlaylist, int idCancion, int posicion) throws ConexionBDException {
        String sql = "INSERT INTO playlist_cancion (id_playlist, id_cancion, posicion) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPlaylist);
            ps.setInt(2, idCancion);
            ps.setInt(3, posicion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionBDException("Error al agregar cancion a la playlist " + idPlaylist, e);
        }
    }

    public void quitarCancion(int idPlaylist, int idCancion) throws ConexionBDException {
        // 1) las canciones que estaban despues bajan una posicion
        String sqlPosiciones = "UPDATE playlist_cancion SET posicion = posicion - 1 WHERE id_playlist = ? AND posicion > "
                + "(SELECT posicion FROM (SELECT posicion FROM playlist_cancion "
                + "WHERE id_playlist = ? AND id_cancion = ?) AS actual)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlPosiciones)) {
            ps.setInt(1, idPlaylist);
            ps.setInt(2, idPlaylist);
            ps.setInt(3, idCancion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionBDException("Error al reordenar la playlist " + idPlaylist, e);
        }

        // 2) se borra el vinculo (tabla intermedia, no es baja logica)
        String sqlBorrar = "DELETE FROM playlist_cancion WHERE id_playlist = ? AND id_cancion = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlBorrar)) {
            ps.setInt(1, idPlaylist);
            ps.setInt(2, idCancion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionBDException("Error al quitar cancion de la playlist " + idPlaylist, e);
        }
    }

    // se reemplazan los vinculos en una transaccion: si algo falla, queda el orden anterior
    public void guardarOrden(int idPlaylist, List<Cancion> canciones) throws ConexionBDException {
        String sqlBorrar = "DELETE FROM playlist_cancion WHERE id_playlist = ?";
        String sqlInsertar = "INSERT INTO playlist_cancion (id_playlist, id_cancion, posicion) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement psBorrar = con.prepareStatement(sqlBorrar);
                 PreparedStatement psInsertar = con.prepareStatement(sqlInsertar)) {
                psBorrar.setInt(1, idPlaylist);
                psBorrar.executeUpdate();
                int posicion = 0;
                for (Cancion c : canciones) {
                    psInsertar.setInt(1, idPlaylist);
                    psInsertar.setInt(2, c.getIdCancion());
                    psInsertar.setInt(3, posicion++);
                    psInsertar.addBatch();
                }
                psInsertar.executeBatch();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al reordenar la playlist " + idPlaylist, e);
        }
    }

    private List<Cancion> cargarCanciones(int idPlaylist) throws ConexionBDException {
        List<Cancion> canciones = new ArrayList<>();
        String sql = "SELECT c.* FROM cancion c "
                + "JOIN playlist_cancion pc ON pc.id_cancion = c.id_cancion "
                + "WHERE pc.id_playlist = ? AND c.activo = TRUE "
                + "ORDER BY pc.posicion";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPlaylist);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    canciones.add(cancionDAO.mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al cargar canciones de la playlist " + idPlaylist, e);
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
