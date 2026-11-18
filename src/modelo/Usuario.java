package modelo;

import Enumeraciones.RolUsuario;

import java.time.LocalDate;
import Seguridad.Seguridad;
public abstract class Usuario {
    private int idUsuario;
    private String nombre;
    private String email;
    private String hashContrasenia;
    private String salt;
    private boolean activo;
    private LocalDate fechaRegistro;

    public Usuario(){

    }

    public Usuario(String nombre, String email, String hashContrasenia, String salt) {
        this(0, nombre, email, hashContrasenia, salt, true, LocalDate.now());
    }

    public Usuario(int idUsuario, String nombre, String email, String hashContrasenia, String salt, boolean activo, LocalDate fechaRegistro) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.email = email;
        this.hashContrasenia = hashContrasenia;
        this.salt = salt;
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashContrasenia() {
        return hashContrasenia;
    }

    public void setHashContrasenia(String hashContrasenia) {
        this.hashContrasenia = hashContrasenia;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public void darDeBaja() {
        activo = false;
    }

    // comparamos hashes usando el salt guardado, no hace falta recuperar la contrasenia

    public boolean verificarContrasenia(String contrasenia) {
        return hashContrasenia.equals(Seguridad.hashear(contrasenia, salt));
    }

    public abstract RolUsuario getRol();

    public abstract boolean puedeAdministrar();

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Usuario)) {
            return false;
        }
        Usuario otro = (Usuario) objeto;
        // Sin un ID asignado, cada instancia representa un usuario diferente.
        return idUsuario > 0 && otro.idUsuario > 0 && idUsuario == otro.idUsuario;
    }

    @Override
    public int hashCode() {
        // Asignar el ID antes de usar el usuario como clave de un HashSet o HashMap.
        if (idUsuario > 0) {
            return Integer.hashCode(idUsuario);
        }
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "Usuario{idUsuario=" + idUsuario + ", nombre='" + nombre
                + "', email='" + email + "', activo=" + activo
                + ", fechaRegistro=" + fechaRegistro + ", rol=" + getRol() + "}";
    }
}
