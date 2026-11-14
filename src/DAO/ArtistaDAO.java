package DAO;
import Conexion.ConexionBD;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

import Enumeraciones.Genero;
import Excepciones.ConexionBDException;
import modelo.Artista;

public class ArtistaDAO extends GenericDAO<Artista> {

    @Override
    protected String getNombreTabla() {
        return "artista";
    }

    @Override
    protected String getNombreColumnaId() {
        return "id_artista";
    }

    @Override
    public int insertar(Artista a) throws ConexionBDException {
        int id = ejecutarInsert(
                "INSERT INTO artista (nombre, pais_origen) VALUES (?, ?)",
                a.getNombre(), a.getPaisOrigen());
        if (id > 0 && a.getGeneros() != null) {
            for (Genero g : a.getGeneros()) {
                agregarGenero(id, g);
            }
        }
        return id;
    }

    @Override
    public boolean modificar(Artista a) throws ConexionBDException {
        boolean ok = ejecutarUpdate(
                "UPDATE artista SET nombre = ?, pais_origen = ? WHERE id_artista = ?",
                a.getNombre(), a.getPaisOrigen(), a.getIdArtista()) > 0;
        // Se regeneran los generos del artista (tabla intermedia, no es baja logica)
        ejecutarUpdate("DELETE FROM artista_genero WHERE id_artista = ?", a.getIdArtista());
        if (a.getGeneros() != null) {
            for (Genero g : a.getGeneros()) {
                agregarGenero(a.getIdArtista(), g);
            }
        }
        return ok;
    }

    public void agregarGenero(int idArtista, Genero genero) throws ConexionBDException {
        if (cargarGeneros(idArtista).contains(genero)) {
            return;
        }
        ejecutarUpdate("INSERT INTO artista_genero (id_artista, genero) VALUES (?, ?)",
                idArtista, genero.name());
    }

    public Set<Genero> cargarGeneros(int idArtista) throws ConexionBDException {
        Set<Genero> generos = EnumSet.noneOf(Genero.class);
        try (java.sql.Connection con = ConexionBD.getConnection();
             java.sql.PreparedStatement ps = con
                .prepareStatement("SELECT genero FROM artista_genero WHERE id_artista = ?")) {
            ps.setInt(1, idArtista);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    generos.add(Genero.valueOf(rs.getString("genero")));
                }
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al cargar generos del artista " + idArtista + " - " + e.getMessage());
        }
        return generos;
    }

    @Override
    public Artista mapear(ResultSet rs) throws SQLException, ConexionBDException {
        Artista a = new Artista();
        a.setIdArtista(rs.getInt("id_artista"));
        a.setNombre(rs.getString("nombre"));
        a.setPaisOrigen(rs.getString("pais_origen"));
        a.setActivo(rs.getBoolean("activo"));
        a.setGeneros(cargarGeneros(a.getIdArtista()));
        return a;
    }
}
