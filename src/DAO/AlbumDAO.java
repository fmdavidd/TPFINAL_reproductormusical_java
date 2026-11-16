package DAO;

import Conexion.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import Excepciones.ConexionBDException;
import modelo.Album;

public class AlbumDAO extends GenericDAO<Album> {

    private final ArtistaDAO artistaDAO = new ArtistaDAO();

    @Override
    protected String getNombreTabla() {
        return "album";
    }

    @Override
    protected String getNombreColumnaId() {
        return "id_album";
    }

    @Override
    public int insertar(Album a) throws ConexionBDException {
        String sql = "INSERT INTO album (titulo, anio_lanzamiento, portada, id_artista) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getTitulo());
            ps.setInt(2, a.getAnioLanzamiento());
            ps.setString(3, a.getPortada());
            ps.setInt(4, a.getArtista().getIdArtista());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al insertar album - " + e.getMessage());
        }
    }

    @Override
    public boolean modificar(Album a) throws ConexionBDException {
        String sql = "UPDATE album SET titulo = ?, anio_lanzamiento = ?, portada = ?, id_artista = ? "
                + "WHERE id_album = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getTitulo());
            ps.setInt(2, a.getAnioLanzamiento());
            ps.setString(3, a.getPortada());
            ps.setInt(4, a.getArtista().getIdArtista());
            ps.setInt(5, a.getIdAlbum());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ConexionBDException("Error al modificar album - " + e.getMessage());
        }
    }

    @Override
    public Album mapear(ResultSet rs) throws SQLException, ConexionBDException {
        Album a = new Album();
        a.setIdAlbum(rs.getInt("id_album"));
        a.setTitulo(rs.getString("titulo"));
        a.setAnioLanzamiento(rs.getInt("anio_lanzamiento"));
        a.setPortada(rs.getString("portada"));
        a.setActivo(rs.getBoolean("activo"));
        a.setArtista(artistaDAO.buscarPorId(rs.getInt("id_artista")));
        return a;
    }
}
