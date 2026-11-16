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
    public Album buscarPorId(int id) throws ConexionBDException {
        Album a = super.buscarPorId(id);
        if (a != null) a.setCanciones(new CancionDAO().listarPorAlbum(id));
        return a;
    }

    @Override
    public java.util.List<Album> listarActivos() throws ConexionBDException {
        java.util.List<Album> lista = consultar("SELECT al.* FROM album al "
                + "JOIN artista ar ON ar.id_artista = al.id_artista WHERE al.activo = TRUE AND ar.activo = TRUE");
        for (Album a : lista) a.setCanciones(new CancionDAO().listarPorAlbum(a.getIdAlbum()));
        return lista;
    }

    public Album buscarReferencia(int id) throws ConexionBDException { return super.buscarPorId(id); }

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
