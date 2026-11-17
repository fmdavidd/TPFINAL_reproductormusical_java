package DAO;
import Conexion.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
        String sql = "INSERT INTO artista (nombre, pais_origen) VALUES (?, ?)";
        int id = -1;
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getNombre());
            ps.setString(2, a.getPaisOrigen());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al insertar artista", e);
        }
        if (id > 0 && a.getGeneros() != null) {
            for (Genero g : a.getGeneros()) {
                agregarGenero(id, g);
            }
        }
        return id;
    }

    @Override
    public boolean modificar(Artista a) throws ConexionBDException {
        String sql = "UPDATE artista SET nombre = ?, pais_origen = ? WHERE id_artista = ?";
        boolean ok;
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getNombre());
            ps.setString(2, a.getPaisOrigen());
            ps.setInt(3, a.getIdArtista());
            ok = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ConexionBDException("Error al modificar artista", e);
        }

        // Se regeneran los generos del artista (tabla intermedia, no es baja logica)
        String sqlBorrar = "DELETE FROM artista_genero WHERE id_artista = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlBorrar)) {
            ps.setInt(1, a.getIdArtista());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionBDException("Error al borrar generos del artista", e);
        }
        if (a.getGeneros() != null) {
            for (Genero g : a.getGeneros()) {
                agregarGenero(a.getIdArtista(), g);
            }
        }
        return ok;
    }

    public List<Artista> buscarPorNombre(String nombre) throws ConexionBDException {
        String sql = "SELECT * FROM artista WHERE nombre LIKE ? AND activo = TRUE";
        List<Artista> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al buscar artistas", e);
        }
        return lista;
    }

    public void agregarGenero(int idArtista, Genero genero) throws ConexionBDException {
        if (cargarGeneros(idArtista).contains(genero)) {
            return;
        }
        String sql = "INSERT INTO artista_genero (id_artista, genero) VALUES (?, ?)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idArtista);
            ps.setString(2, genero.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionBDException("Error al agregar genero al artista " + idArtista, e);
        }
    }

    public Set<Genero> cargarGeneros(int idArtista) throws ConexionBDException {
        Set<Genero> generos = EnumSet.noneOf(Genero.class);
        String sql = "SELECT genero FROM artista_genero WHERE id_artista = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idArtista);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    generos.add(Genero.valueOf(rs.getString("genero")));
                }
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al cargar generos del artista " + idArtista, e);
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
