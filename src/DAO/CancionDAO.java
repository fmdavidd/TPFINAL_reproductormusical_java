package DAO;

import java.sql.ResultSet;
import java.sql.SQLException;

import Enumeraciones.Genero;
import Excepciones.ConexionBDException;
import modelo.Album;
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
        return ejecutarInsert(
                "INSERT INTO cancion (titulo, duracion_segundos, ruta_archivo, genero, id_artista, id_album) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                c.getTitulo(), c.getDuracionSegundos(), c.getRutaArchivo(), c.getGenero().name(),
                c.getArtista().getIdArtista(), idAlbumONull(c.getAlbum()));
    }

    @Override
    public boolean modificar(Cancion c) throws ConexionBDException {
        return ejecutarUpdate(
                "UPDATE cancion SET titulo = ?, duracion_segundos = ?, ruta_archivo = ?, genero = ?, "
                        + "id_artista = ?, id_album = ? WHERE id_cancion = ?",
                c.getTitulo(), c.getDuracionSegundos(), c.getRutaArchivo(), c.getGenero().name(),
                c.getArtista().getIdArtista(), idAlbumONull(c.getAlbum()), c.getIdCancion()) > 0;
    }

    private Integer idAlbumONull(Album album) {
        return album == null ? null : album.getIdAlbum();
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
