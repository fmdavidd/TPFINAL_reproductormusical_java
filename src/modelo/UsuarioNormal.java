package modelo;

import Enumeraciones.RolUsuario;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class UsuarioNormal extends Usuario {
    private boolean premium;
    private Set<Integer> favoritos;

    public UsuarioNormal(){
        super();
    }

    public UsuarioNormal(String nombre, String email, String hashContrasenia, String salt, boolean premium) {
        super(nombre, email, hashContrasenia, salt);
        this.premium = premium;
        this.favoritos = new HashSet<Integer>();
    }

    public UsuarioNormal(int idUsuario, String nombre, String email, String hashContrasenia, String salt, boolean activo, LocalDate fechaRegistro, boolean premium) {
        super(idUsuario, nombre, email, hashContrasenia, salt, activo, fechaRegistro);
        this.premium = premium;
        this.favoritos = new HashSet<Integer>();
    }

    public boolean esPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public Set<Integer> getFavoritos() {
        return new HashSet<Integer>(favoritos);
    }

    public void setFavoritos(Set<Integer> favoritos) {
        this.favoritos = new HashSet<Integer>(favoritos);
    }

    public boolean agregarFavorito(int idCancion) {
        return favoritos.add(idCancion);
    }

    public boolean quitarFavorito(int idCancion) {
        return favoritos.remove(idCancion);
    }

    public boolean esFavorita(int idCancion) {
        return favoritos.contains(idCancion);
    }

    @Override
    public RolUsuario getRol() {
        return RolUsuario.NORMAL;
    }

    @Override
    public boolean puedeAdministrar() {
        return false;
    }
}
