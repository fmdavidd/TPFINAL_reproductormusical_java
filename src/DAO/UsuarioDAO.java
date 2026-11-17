package DAO;
import Conexion.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import modelo.RegistroAdministrativo;
import java.util.TreeMap;

import Enumeraciones.NivelAdmin;
import Enumeraciones.RolUsuario;
import Excepciones.ConexionBDException;
import modelo.Usuario;
import modelo.UsuarioAdministrador;
import modelo.UsuarioNormal;

public class UsuarioDAO extends GenericDAO<Usuario> {

    @Override
    protected String getNombreTabla() {
        return "usuario";
    }

    @Override
    protected String getNombreColumnaId() {
        return "id_usuario";
    }

    @Override
    public int insertar(Usuario u) throws ConexionBDException {
        LocalDate fecha = u.getFechaRegistro() != null ? u.getFechaRegistro() : LocalDate.now();
        String sql = "INSERT INTO usuario (nombre, email, hash_contrasenia, salt, rol, premium, nivel_admin, fecha_registro) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getHashContrasenia());
            ps.setString(4, u.getSalt());
            ps.setString(5, u.getRol().name());
            asignarPremiumYNivel(ps, 6, u);
            ps.setDate(8, java.sql.Date.valueOf(fecha));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al insertar usuario - " + e.getMessage());
        }
    }

    @Override
    public boolean modificar(Usuario u) throws ConexionBDException {
        String sql = "UPDATE usuario SET nombre = ?, email = ?, hash_contrasenia = ?, salt = ?, "
                + "premium = ?, nivel_admin = ? WHERE id_usuario = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getHashContrasenia());
            ps.setString(4, u.getSalt());
            asignarPremiumYNivel(ps, 5, u);
            ps.setInt(7, u.getIdUsuario());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ConexionBDException("Error al modificar usuario - " + e.getMessage());
        }
    }

    public boolean agregarFavorito(int idUsuario, int idCancion) throws ConexionBDException {
        if (existeFavorito(idUsuario, idCancion)) {
            return false;
        }
        String sql = "INSERT INTO favorito (id_usuario, id_cancion, fecha_agregado) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idCancion);
            ps.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ConexionBDException("Error al agregar favorito - " + e.getMessage());
        }
    }

    public boolean quitarFavorito(int idUsuario, int idCancion) throws ConexionBDException {
        String sql = "DELETE FROM favorito WHERE id_usuario = ? AND id_cancion = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idCancion);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ConexionBDException("Error al quitar favorito - " + e.getMessage());
        }
    }

    public void registrarAccion(int idAdmin, String accion) throws ConexionBDException {
        String sql = "INSERT INTO registro_accion (id_admin, fecha_hora, accion) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAdmin);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, accion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ConexionBDException("Error al registrar accion - " + e.getMessage());
        }
    }

    public List<RegistroAdministrativo> listarHistorialAdministrativo() throws ConexionBDException {
        // No filtrar por activo: las acciones de administradores dados de baja siguen siendo parte del historial.
        String sql = "SELECT r.id_registro, r.id_admin, r.fecha_hora, r.accion, u.nombre, u.email "
                + "FROM registro_accion r LEFT JOIN usuario u ON u.id_usuario = r.id_admin "
                + "ORDER BY r.fecha_hora DESC, r.id_registro DESC";
        List<RegistroAdministrativo> historial = new ArrayList<>();
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                historial.add(new RegistroAdministrativo(rs.getInt("id_registro"), rs.getInt("id_admin"),
                        rs.getString("nombre"), rs.getString("email"),
                        rs.getTimestamp("fecha_hora").toLocalDateTime(), rs.getString("accion")));
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al consultar el historial administrativo - " + e.getMessage());
        }
        return historial;
    }

    // Completa los parametros premium y nivel_admin (posicion y posicion + 1).
    // Un usuario normal tiene premium y nivel NULL; un administrador, al reves.
    private void asignarPremiumYNivel(PreparedStatement ps, int posicion, Usuario u) throws SQLException {
        if (u instanceof UsuarioNormal) {
            ps.setBoolean(posicion, ((UsuarioNormal) u).esPremium());
            ps.setNull(posicion + 1, Types.VARCHAR);
        } else {
            ps.setNull(posicion, Types.BOOLEAN);
            ps.setString(posicion + 1, ((UsuarioAdministrador) u).getNivelAdmin().name());
        }
    }


    private boolean existeFavorito(int idUsuario, int idCancion) throws ConexionBDException {
        String sql = "SELECT 1 FROM favorito WHERE id_usuario = ? AND id_cancion = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idCancion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al verificar favorito" + " - " + e.getMessage());
        }
    }

    private Map<Integer,LocalDate> cargarFavoritos(int idUsuario) throws ConexionBDException {
        Map<Integer,LocalDate> fechas = new HashMap<>();
        String sql = "SELECT id_cancion, fecha_agregado FROM favorito WHERE id_usuario = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date fecha = rs.getDate("fecha_agregado");
                    fechas.put(rs.getInt("id_cancion"),fecha == null ? null : fecha.toLocalDate());
                }
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al cargar favoritos del usuario " + idUsuario + " - " + e.getMessage());
        }
        return fechas;
    }

    private void cargarRegistroAcciones(UsuarioAdministrador admin) throws ConexionBDException {
        String sql = "SELECT fecha_hora, accion FROM registro_accion WHERE id_admin = ?";
        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, admin.getIdUsuario());
            TreeMap<LocalDateTime, String> registro = new TreeMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registro.put(rs.getTimestamp("fecha_hora").toLocalDateTime(), rs.getString("accion"));
                }
            }
            admin.setRegistroAcciones(registro);
        } catch (SQLException e) {
            throw new ConexionBDException("Error al cargar acciones del admin " + admin.getIdUsuario() + " - " + e.getMessage());
        }
    }

    @Override
    public Usuario mapear(ResultSet rs) throws SQLException, ConexionBDException {
        RolUsuario rol = RolUsuario.valueOf(rs.getString("rol"));
        Usuario u;

        if (rol == RolUsuario.ADMIN) {
            UsuarioAdministrador admin = new UsuarioAdministrador();
            admin.setNivelAdmin(NivelAdmin.valueOf(rs.getString("nivel_admin")));
            u = admin;
        } else {
            UsuarioNormal normal = new UsuarioNormal();
            normal.setPremium(rs.getBoolean("premium"));
            u = normal;
        }

        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setEmail(rs.getString("email"));
        u.setHashContrasenia(rs.getString("hash_contrasenia"));
        u.setSalt(rs.getString("salt"));
        u.setActivo(rs.getBoolean("activo"));
        u.setFechaRegistro(rs.getDate("fecha_registro").toLocalDate());

        if (u instanceof UsuarioNormal) {
            ((UsuarioNormal) u).setFavoritos(java.util.Collections.emptySet());
            ((UsuarioNormal) u).setFechasFavoritos(cargarFavoritos(u.getIdUsuario()));
        } else {
            cargarRegistroAcciones((UsuarioAdministrador) u);
        }
        return u;
    }
}
