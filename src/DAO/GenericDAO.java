package DAO;
import Interfaces.IGenericDAO;
import Conexion.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import Excepciones.ConexionBDException;

/**
 * Clase abstracta que implementa la logica JDBC comun a todos los DAOs.
 * Las subclases solo indican tabla, columna id y como mapear una fila.
 */
public abstract class GenericDAO<T> implements IGenericDAO<T> {

    // ---------- Metodos que deben definir las subclases ----------

    protected abstract String getNombreTabla();

    protected abstract String getNombreColumnaId();

    /** Convierte la fila actual del ResultSet en una entidad (no debe hacer rs.next()). */
    protected abstract T mapear(ResultSet rs) throws SQLException, ConexionBDException;

    // ---------- Implementacion comun ----------

    @Override
    public boolean darDeBaja(int id) throws ConexionBDException {
        return cambiarActivo(id, false);
    }

    @Override
    public boolean darDeAlta(int id) throws ConexionBDException {
        return cambiarActivo(id, true);
    }

    private boolean cambiarActivo(int id, boolean valor) throws ConexionBDException {
        String sql = "UPDATE " + getNombreTabla() + " SET activo = ? WHERE " + getNombreColumnaId() + " = ?";
        return ejecutarUpdate(sql, valor, id) > 0;
    }

    @Override
    public T buscarPorId(int id) throws ConexionBDException {
        String sql = "SELECT * FROM " + getNombreTabla() + " WHERE " + getNombreColumnaId() + " = ?";
        List<T> resultado = consultar(sql, id);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<T> listarActivos() throws ConexionBDException {
        return consultar("SELECT * FROM " + getNombreTabla() + " WHERE activo = TRUE");
    }

    @Override
    public List<T> listarTodos() throws ConexionBDException {
        return consultar("SELECT * FROM " + getNombreTabla());
    }

    // ---------- Helpers JDBC para las subclases ----------

    /** Ejecuta un SELECT con PreparedStatement y mapea cada fila a T. */
    protected List<T> consultar(String sql, Object... params) throws ConexionBDException {
        List<T> lista = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            asignarParametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al consultar: " + sql + " - " + e.getMessage());
        }
        return lista;
    }

    /** Ejecuta INSERT / UPDATE / DELETE y devuelve la cantidad de filas afectadas. */
    protected int ejecutarUpdate(String sql, Object... params) throws ConexionBDException {
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            asignarParametros(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionBDException("Error al ejecutar: " + sql + " - " + e.getMessage());
        }
    }

    /** Ejecuta un INSERT y devuelve el id autogenerado (o -1 si no hubo). */
    protected int ejecutarInsert(String sql, Object... params) throws ConexionBDException {
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignarParametros(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al insertar: " + sql + " - " + e.getMessage());
        }
    }

    private void asignarParametros(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
