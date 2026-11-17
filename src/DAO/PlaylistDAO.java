package DAO;
import Conexion.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

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
            throw new ConexionBDException("Error al insertar playlist - " + e.getMessage());
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
            throw new ConexionBDException("Error al modificar playlist - " + e.getMessage());
        }
    }

    public void agregarCancion(int idPlaylist, int idCancion, int posicion) throws ConexionBDException {
        String sql = "INSERT INTO playlist_cancion (id_playlist, id_cancion, posicion) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPlaylist);
            ps.setInt(2, idCancion);
            // Puede haber vínculos con canciones inactivas que no aparecen en memoria.
            // Calcular el final real evita insertar antes de la última canción guardada.
            try (PreparedStatement ultima = con.prepareStatement(
                    "SELECT COALESCE(MAX(posicion), -1) + 1 FROM playlist_cancion WHERE id_playlist = ?")) {
                ultima.setInt(1, idPlaylist);
                try (ResultSet rs = ultima.executeQuery()) {
                    if (rs.next()) posicion = rs.getInt(1);
                }
            }
            ps.setInt(3, posicion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionBDException("Error al agregar cancion a la playlist " + idPlaylist + " - " + e.getMessage());
        }
    }

    public void quitarCancion(int idPlaylist, int idCancion) throws ConexionBDException {
        try (Connection con = ConexionBD.getConnection()) {
            con.setAutoCommit(false);
            try {
                int posicion;
                try (PreparedStatement buscar = con.prepareStatement(
                        "SELECT posicion FROM playlist_cancion WHERE id_playlist = ? AND id_cancion = ? FOR UPDATE")) {
                    buscar.setInt(1, idPlaylist); buscar.setInt(2, idCancion);
                    try (ResultSet rs = buscar.executeQuery()) {
                        if (!rs.next()) { con.rollback(); return; }
                        posicion = rs.getInt(1);
                    }
                }
                try (PreparedStatement borrar = con.prepareStatement(
                        "DELETE FROM playlist_cancion WHERE id_playlist = ? AND id_cancion = ?")) {
                    borrar.setInt(1, idPlaylist); borrar.setInt(2, idCancion); borrar.executeUpdate();
                }
                try (PreparedStatement ordenar = con.prepareStatement(
                        "UPDATE playlist_cancion SET posicion = posicion - 1 WHERE id_playlist = ? AND posicion > ?")) {
                    ordenar.setInt(1, idPlaylist); ordenar.setInt(2, posicion); ordenar.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                try { con.rollback(); } catch (SQLException falloRollback) { e.addSuppressed(falloRollback); }
                throw e;
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al quitar cancion de la playlist " + idPlaylist + " - " + e.getMessage());
        }
    }

    public void reordenarCanciones(int idPlaylist, List<Integer> anterior, List<Integer> nuevoOrden)
            throws ConexionBDException {
        if (anterior.size() != nuevoOrden.size() || new HashSet<>(nuevoOrden).size() != nuevoOrden.size()
                || !new HashSet<>(anterior).equals(new HashSet<>(nuevoOrden)))
            throw new IllegalArgumentException("El nuevo orden debe contener las mismas canciones, sin duplicados.");
        try (Connection con = ConexionBD.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement playlist = con.prepareStatement(
                        "SELECT id_playlist FROM playlist WHERE id_playlist = ? AND activo = TRUE FOR UPDATE")) {
                    playlist.setInt(1,idPlaylist);
                    try (ResultSet rs = playlist.executeQuery()) {
                        if (!rs.next()) throw new SQLException("La playlist ya no está activa. Actualizá la lista.");
                    }
                }
                List<Integer> todas = new ArrayList<>();
                List<Integer> visibles = new ArrayList<>();
                try (PreparedStatement buscar = con.prepareStatement(
                        "SELECT pc.id_cancion, c.activo FROM playlist_cancion pc "
                        + "JOIN cancion c ON c.id_cancion = pc.id_cancion "
                        + "WHERE pc.id_playlist = ? ORDER BY pc.posicion, pc.id_cancion FOR UPDATE")) {
                    buscar.setInt(1,idPlaylist);
                    try (ResultSet rs = buscar.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("id_cancion"); todas.add(id);
                            if (rs.getBoolean("activo")) visibles.add(id);
                        }
                    }
                }
                if (!visibles.equals(anterior))
                    throw new SQLException("Las canciones o su orden cambiaron. Actualizá la playlist antes de reordenar.");
                // Las canciones dadas de baja conservan sus lugares entre las canciones visibles.
                int siguiente = 0;
                HashSet<Integer> activas = new HashSet<>(visibles);
                try (PreparedStatement guardar = con.prepareStatement(
                        "UPDATE playlist_cancion SET posicion = ? WHERE id_playlist = ? AND id_cancion = ?")) {
                    for (int i = 0; i < todas.size(); i++) {
                        int id = todas.get(i);
                        if (activas.contains(id)) id = nuevoOrden.get(siguiente++);
                        guardar.setInt(1,i); guardar.setInt(2,idPlaylist); guardar.setInt(3,id);
                        if (guardar.executeUpdate() != 1) throw new SQLException("No se pudo guardar la posición de una canción.");
                    }
                }
                con.commit();
            } catch (SQLException e) {
                try { con.rollback(); } catch (SQLException falloRollback) { e.addSuppressed(falloRollback); }
                throw e;
            }
        } catch (SQLException e) {
            throw new ConexionBDException("No se pudo guardar el orden de la playlist " + idPlaylist + " - " + e.getMessage());
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
