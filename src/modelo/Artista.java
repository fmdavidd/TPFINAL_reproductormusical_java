package modelo;

import Enumeraciones.Genero;

import java.util.HashSet;
import java.util.Set;

public class Artista {
    private int idArtista;
    private String nombre;
    private String paisOrigen;
    private boolean activo;
    private Set<Genero> generos;

    public Artista(String nombre, String paisOrigen, boolean activo) {
        this.nombre = nombre;
        this.paisOrigen = paisOrigen;
        this.activo = activo;
        this.generos = new HashSet<Genero>();
    }

    public Artista() {
    }

    public int getIdArtista() {
        return idArtista;
    }

    public void setIdArtista(int idArtista) {
        this.idArtista = idArtista;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPaisOrigen() {
        return paisOrigen;
    }

    public void setPaisOrigen(String paisOrigen) {
        this.paisOrigen = paisOrigen;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Set<Genero> getGeneros() {
        return generos;
    }

    public void setGeneros(Set<Genero> generos) {

        if (generos == null) {
            this.generos = new HashSet<>();
        } else {
            this.generos = new HashSet<>(generos);
        }

    }

    public boolean agregarGenero(Genero genero) {
        if (genero == null) {
            return false;
        }
        return generos.add(genero);
    }

    public boolean quitarGenero(Genero genero) {
        return generos.remove(genero);
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Artista)) {
            return false;
        }

        Artista otro = (Artista) objeto;
        return idArtista > 0 && otro.idArtista > 0 && idArtista == otro.idArtista;
    }

    @Override
    public int hashCode() {
        return idArtista > 0 ? Integer.valueOf(idArtista).hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "Artista{idArtista=" + idArtista + ", nombre='" + nombre + "'}";
    }
}
