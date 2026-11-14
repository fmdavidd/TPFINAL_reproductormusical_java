package Interfaces;

import modelo.Cancion;

import java.util.List;

/// Representa un elemento que puede reproducirse en el reproductor musical.
public interface Reproducible {

    String getTitulo();

    int getDuracionTotal();

    List<Cancion> getCanciones();
}

