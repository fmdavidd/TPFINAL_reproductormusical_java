package DAO;

import Conexion.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import Enumeraciones.Genero;
import Excepciones.ConexionBDException;
import modelo.Cancion;

public class CancionDAO extends GenericDAO<Cancion> {

    private final ArtistaDAO artistaDAO = new ArtistaDAO();
    private final AlbumDAO albumDAO = new AlbumDAO();

    @Override
    protected String getNombreTabla() {
        return "cancion";
    }

    @Override
    protected String getNombreColumnaId() {
        return "id_cancion";
    }

    @Override
    public int insertar(Cancion c) throws ConexionBDException {
        String sql = "INSERT INTO cancion (titulo, duracion_segundos, ruta_archivo, genero, id_artista, id_album) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getTitulo());
            ps.setInt(2, c.getDuracionSegundos());
            ps.setString(3, c.getRutaArchivo());
            ps.setString(4, c.getGenero().name());
            ps.setInt(5, c.getArtista().getIdArtista());
            if (c.getAlbum() == null) {
                ps.setNull(6, Types.INTEGER); // single
            } else {
                ps.setInt(6, c.getAlbum().getIdAlbum());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al insertar cancion - " + e.getMessage());
        }
    }

    @Override
    public boolean modificar(Cancion c) throws ConexionBDException {
        String sql = "UPDATE cancion SET titulo = ?, duracion_segundos = ?, ruta_archivo = ?, genero = ?, "
                + "id_artista = ?, id_album = ? WHERE id_cancion = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getTitulo());
            ps.setInt(2, c.getDuracionSegundos());
            ps.setString(3, c.getRutaArchivo());
            ps.setString(4, c.getGenero().name());
            ps.setInt(5, c.getArtista().getIdArtista());
            if (c.getAlbum() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, c.getAlbum().getIdAlbum());
            }
            ps.setInt(7, c.getIdCancion());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ConexionBDException("Error al modificar cancion - " + e.getMessage());
        }
    }

    @Override
    public Cancion mapear(ResultSet rs) throws SQLException, ConexionBDException {
        Cancion c = new Cancion();
        c.setIdCancion(rs.getInt("id_cancion"));
        c.setTitulo(rs.getString("titulo"));
        c.setDuracionSegundos(rs.getInt("duracion_segundos"));
        c.setRutaArchivo(rs.getString("ruta_archivo"));
        c.setGenero(Genero.valueOf(rs.getString("genero")));
        c.setActivo(rs.getBoolean("activo"));
        c.setArtista(artistaDAO.buscarPorId(rs.getInt("id_artista")));

        int idAlbum = rs.getInt("id_album");
        if (rs.wasNull()) {
            c.setAlbum(null); // single
        } else {
            c.setAlbum(albumDAO.buscarPorId(idAlbum));
        }
        return c;
    }
}
