package DAO;
import Conexion.ConexionBD;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
        return ejecutarInsert(
                "INSERT INTO usuario (nombre, email, hash_contrasenia, salt, rol, premium, nivel_admin, fecha_registro) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                u.getNombre(), u.getEmail(), u.getHashContrasenia(), u.getSalt(), u.getRol().name(),
                premiumONull(u), nivelAdminONull(u), java.sql.Date.valueOf(fecha));
    }

    @Override
    public boolean modificar(Usuario u) throws ConexionBDException {
        return ejecutarUpdate(
                "UPDATE usuario SET nombre = ?, email = ?, hash_contrasenia = ?, salt = ?, "
                        + "premium = ?, nivel_admin = ? WHERE id_usuario = ?",
                u.getNombre(), u.getEmail(), u.getHashContrasenia(), u.getSalt(),
                premiumONull(u), nivelAdminONull(u), u.getIdUsuario()) > 0;
    }

    public boolean agregarFavorito(int idUsuario, int idCancion) throws ConexionBDException {
        if (existeFavorito(idUsuario, idCancion)) {
            return false;
        }
        return ejecutarUpdate(
                "INSERT INTO favorito (id_usuario, id_cancion, fecha_agregado) VALUES (?, ?, ?)",
                idUsuario, idCancion, java.sql.Date.valueOf(LocalDate.now())) > 0;
    }

    public boolean quitarFavorito(int idUsuario, int idCancion) throws ConexionBDException {
        return ejecutarUpdate("DELETE FROM favorito WHERE id_usuario = ? AND id_cancion = ?",
                idUsuario, idCancion) > 0;
    }

    public void registrarAccion(int idAdmin, String accion) throws ConexionBDException {
        ejecutarUpdate("INSERT INTO registro_accion (id_admin, fecha_hora, accion) VALUES (?, ?, ?)",
                idAdmin, Timestamp.valueOf(LocalDateTime.now()), accion);
    }


    private boolean existeFavorito(int idUsuario, int idCancion) throws ConexionBDException {
        String sql = "SELECT 1 FROM favorito WHERE id_usuario = ? AND id_cancion = ?";
        try (java.sql.Connection con = ConexionBD.getConnection();
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

    private Set<Integer> cargarFavoritos(int idUsuario) throws ConexionBDException {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT id_cancion FROM favorito WHERE id_usuario = ?";
        try (java.sql.Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id_cancion"));
                }
            }
        } catch (SQLException e) {
            throw new ConexionBDException("Error al cargar favoritos del usuario " + idUsuario + " - " + e.getMessage());
        }
        return ids;
    }

    private void cargarRegistroAcciones(UsuarioAdministrador admin) throws ConexionBDException {
        String sql = "SELECT fecha_hora, accion FROM registro_accion WHERE id_admin = ?";
        try (java.sql.Connection con = ConexionBD.getConnection();
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

    private Boolean premiumONull(Usuario u) {
        return (u instanceof UsuarioNormal) ? ((UsuarioNormal) u).esPremium() : null;
    }

    private String nivelAdminONull(Usuario u) {
        return (u instanceof UsuarioAdministrador)
                ? ((UsuarioAdministrador) u).getNivelAdmin().name() : null;
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
            ((UsuarioNormal) u).setFavoritos(cargarFavoritos(u.getIdUsuario()));
        } else {
            cargarRegistroAcciones((UsuarioAdministrador) u);
        }
        return u;
    }
}