package modelo;

import Interfaces.Reproducible;

import java.util.ArrayList;
import java.util.List;

public class Album implements Reproducible {
    private int idAlbum;
    private String titulo;
    private int anioLanzamiento;
    private String portada;
    private Artista artista;
    private boolean activo;
    private List<Cancion> canciones;

    public Album(String titulo, int anioLanzamiento, String portada, Artista artista, boolean activo) {
        this.titulo = titulo;
        this.anioLanzamiento = anioLanzamiento;
        this.portada = portada;
        this.artista = artista;
        this.activo = activo;
        this.canciones = new ArrayList<Cancion>();
    }

    public int getIdAlbum() { return idAlbum; }
    public void setIdAlbum(int idAlbum) { this.idAlbum = idAlbum; }
    @Override public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public int getAnioLanzamiento() { return anioLanzamiento; }
    public void setAnioLanzamiento(int anioLanzamiento) { this.anioLanzamiento = anioLanzamiento; }
    public String getPortada() { return portada; }
    public void setPortada(String portada) { this.portada = portada; }
    public Artista getArtista() { return artista; }
    public void setArtista(Artista artista) { this.artista = artista; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public List<Cancion> getCanciones() { return canciones; }

    public void setCanciones(List<Cancion> canciones) {
        this.canciones = canciones == null ? new ArrayList<Cancion>() : new ArrayList<Cancion>(canciones);
    }

    public void agregarCancion(Cancion cancion) {
        if (cancion != null && !canciones.contains(cancion)) {
            canciones.add(cancion);
            cancion.setAlbum(this);
        }
    }

    public boolean quitarCancion(int idCancion) {
        for (int i = 0; i < canciones.size(); i++) {
            Cancion cancion = canciones.get(i);
            if (cancion.getIdCancion() == idCancion) {
                canciones.remove(i);
                if (cancion.getAlbum() == this) {
                    cancion.setAlbum(null);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int getDuracionTotal() {
        int total = 0;
        for (Cancion cancion : canciones) {
            total += cancion.getDuracionTotal();
        }
        return total;
    }
}
