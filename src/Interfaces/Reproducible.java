package Interfaces;

import modelo.Cancion;

import java.util.List;

// Agrupa metadatos: titulo, duracion y canciones. No decodifica ni reproduce audio.
public interface Reproducible {

    String getTitulo();

    int getDuracionTotal();

    List<Cancion> getCanciones();
}

