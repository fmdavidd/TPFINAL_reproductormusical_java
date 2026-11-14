package modelo;

import Enumeraciones.Genero;
import Interfaces.Reproducible;

import java.util.ArrayList;
import java.util.List;

public class Cancion implements Reproducible {
    private int idCancion;
    private String titulo;
    private int duracionSegundos;
    private String rutaArchivo;
    private Genero genero;
    private Artista artista;
    private Album album;
    private boolean activo;

    public Cancion(String titulo, int duracionSegundos, String rutaArchivo,
                   Genero genero, Artista artista, Album album, boolean activo) {
        this.titulo = titulo;
        this.duracionSegundos = duracionSegundos;
        this.rutaArchivo = rutaArchivo;
        this.genero = genero;
        this.artista = artista;
        this.album = album;
        this.activo = activo;
    }

    public Cancion() {
    }

    public int getIdCancion() {
        return idCancion;
    }

    public void setIdCancion(int idCancion) {
        this.idCancion = idCancion;
    }

    @Override
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public int getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(int duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public Artista getArtista() {
        return artista;
    }

    public void setArtista(Artista artista) {
        this.artista = artista;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getDuracionFormateada() {
        int minutos = duracionSegundos / 60;
        int segundos = duracionSegundos % 60;
        return String.format("%02d:%02d", minutos, segundos);
    }

    @Override
    public int getDuracionTotal() {
        return duracionSegundos;
    }

    @Override
    public List<Cancion> getCanciones() {
        List<Cancion> canciones = new ArrayList<Cancion>();
        canciones.add(this);
        return canciones;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Cancion)) {
            return false;
        }

        Cancion otra = (Cancion) objeto;
        return idCancion > 0 && otra.idCancion > 0 && idCancion == otra.idCancion;
    }

    @Override
    public int hashCode() {
        return idCancion > 0 ? Integer.valueOf(idCancion).hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "Cancion{idCancion=" + idCancion + ", titulo='" + titulo + "'}";
    }
}
