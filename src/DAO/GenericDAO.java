package DAO;

import Interfaces.IGenericDAO;
import Conexion.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import Excepciones.ConexionBDException;


public abstract class GenericDAO<T> implements IGenericDAO<T> {

    protected abstract String getNombreTabla();

    protected abstract String getNombreColumnaId();

    protected abstract T mapear(ResultSet rs) throws SQLException, ConexionBDException;


    // Baja logica: no se borra la fila, solo se marca activo = FALSE.
    @Override
    public boolean darDeBaja(int id) throws ConexionBDException {
        String sql = "UPDATE " + getNombreTabla() + " SET activo = FALSE WHERE " + getNombreColumnaId() + " = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ConexionBDException("Error al dar de baja en " + getNombreTabla(), e);
        }
    }

    // Reactivar: vuelve a marcar activo = TRUE.
    @Override
    public boolean darDeAlta(int id) throws ConexionBDException {
        String sql = "UPDATE " + getNombreTabla() + " SET activo = TRUE WHERE " + getNombreColumnaId() + " = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ConexionBDException("Error al dar de alta en " + getNombreTabla(), e);
        }
    }

    // Busca por id sin mirar activo (lo usan los mappers y la reactivacion).
    @Override
    public T buscarPorId(int id) throws ConexionBDException {
        String sql = "SELECT * FROM " + getNombreTabla() + " WHERE " + getNombreColumnaId() + " = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al buscar en " + getNombreTabla(), e);
        }
    }

    @Override
    public List<T> listarActivos() throws ConexionBDException {
        String sql = "SELECT * FROM " + getNombreTabla() + " WHERE activo = TRUE";
        List<T> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al listar activos de " + getNombreTabla(), e);
        }
        return lista;
    }

    @Override
    public List<T> listarInactivos() throws ConexionBDException {
        String sql = "SELECT * FROM " + getNombreTabla() + " WHERE activo = FALSE";
        List<T> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al listar inactivos de " + getNombreTabla(), e);
        }
        return lista;
    }
}
