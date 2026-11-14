package DAO;

import java.sql.ResultSet;
import java.sql.SQLException;

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
        return ejecutarInsert(
                "INSERT INTO album (titulo, anio_lanzamiento, portada, id_artista) VALUES (?, ?, ?, ?)",
                a.getTitulo(), a.getAnioLanzamiento(), a.getPortada(),
                a.getArtista().getIdArtista());
    }

    @Override
    public boolean modificar(Album a) throws ConexionBDException {
        return ejecutarUpdate(
                "UPDATE album SET titulo = ?, anio_lanzamiento = ?, portada = ?, id_artista = ? "
                        + "WHERE id_album = ?",
                a.getTitulo(), a.getAnioLanzamiento(), a.getPortada(),
                a.getArtista().getIdArtista(), a.getIdAlbum()) > 0;
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
