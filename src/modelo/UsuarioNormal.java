package modelo;

import Enumeraciones.RolUsuario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class UsuarioNormal extends Usuario {
    private boolean premium;
    private Set<Integer> favoritos;
    private Map<Integer,LocalDate> fechasFavoritos = new HashMap<>();
    private List<Playlist> playlists = new ArrayList<Playlist>();

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
        fechasFavoritos.keySet().retainAll(favoritos);
    }

    public Map<Integer,LocalDate> getFechasFavoritos() { return new HashMap<>(fechasFavoritos); }

    public void setFechasFavoritos(Map<Integer,LocalDate> fechas) {
        fechasFavoritos = new HashMap<>(fechas);
        favoritos.addAll(fechas.keySet());
    }

    public LocalDate getFechaFavorito(int idCancion) { return fechasFavoritos.get(idCancion); }

    public boolean agregarFavorito(int idCancion) {
        if (!favoritos.add(idCancion)) return false;
        fechasFavoritos.put(idCancion,LocalDate.now());
        return true;
    }

    public boolean quitarFavorito(int idCancion) {
        if (!favoritos.remove(idCancion)) return false;
        fechasFavoritos.remove(idCancion);
        return true;
    }

    public boolean esFavorita(int idCancion) {
        return favoritos.contains(idCancion);
    }

    public List<Playlist> getPlaylists() {
        return new ArrayList<Playlist>(playlists);
    }

    public void setPlaylists(List<Playlist> playlists) {
        if (playlists == null) {
            this.playlists = new ArrayList<Playlist>();
        } else {
            this.playlists = new ArrayList<Playlist>(playlists);
        }
    }

    public void agregarPlaylist(Playlist playlist) {
        if (playlist != null) {
            playlists.add(playlist);
        }
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
