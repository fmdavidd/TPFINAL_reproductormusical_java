package modelo;

import Interfaces.Reproducible;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Playlist implements Reproducible {
    private int idPlaylist;
    private String nombre;
    private String descripcion;
    private boolean publica;
    private int idPropietario;
    private boolean activo;
    private List<Cancion> canciones;

    public Playlist(String nombre, String descripcion, boolean publica, int idPropietario, boolean activo) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.publica = publica;
        this.idPropietario = idPropietario;
        this.activo = activo;
        this.canciones = new ArrayList<Cancion>();
    }

    public Playlist() {
    }

    public int getIdPlaylist() { return idPlaylist; }
    public void setIdPlaylist(int idPlaylist) { this.idPlaylist = idPlaylist; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isPublica() { return publica; }
    public void setPublica(boolean publica) { this.publica = publica; }
    public int getIdPropietario() { return idPropietario; }
    public void setIdPropietario(int idPropietario) { this.idPropietario = idPropietario; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public List<Cancion> getCanciones() { return canciones; }

    public void setCanciones(List<Cancion> canciones) {

        if (canciones == null) {
            this.canciones = new ArrayList<>();
        } else {
            // copiamos la lista para que cambios desde afuera no alteren la playlist
            this.canciones = new ArrayList<>(canciones);
        }

    }

    public boolean agregarCancion(Cancion cancion) {
        if (cancion == null || canciones.contains(cancion)) {
            return false;
        }
        canciones.add(cancion);
        return true;
    }

    public boolean quitarCancion(int idCancion) {
        for (int i = 0; i < canciones.size(); i++) {
            if (canciones.get(i).getIdCancion() == idCancion) {
                canciones.remove(i);
                return true;
            }
        }
        return false;
    }

    public void moverCancion(int posicionOrigen, int posicionDestino) {

        if (posicionOrigen < 0 || posicionOrigen >= canciones.size()
                || posicionDestino < 0 || posicionDestino >= canciones.size()) {
            throw new IndexOutOfBoundsException("Las posiciones deben existir en la playlist.");
        }

        // las demas canciones se corren solas al quitar e insertar en la lista
        Cancion cancion = canciones.remove(posicionOrigen);
        canciones.add(posicionDestino, cancion);
    }

    @Override
    public String getTitulo() { return nombre; }

    @Override
    public int getDuracionTotal() {
        int total = 0;
        for (Cancion cancion : canciones) {
            total += cancion.getDuracionTotal();
        }
        return total;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return idPlaylist == playlist.idPlaylist;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idPlaylist);
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "idPlaylist=" + idPlaylist +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", publica=" + publica +
                ", idPropietario=" + idPropietario +
                ", activo=" + activo +
                ", canciones=" + canciones +
                '}';
    }
}
