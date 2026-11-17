package modelo;

import java.util.ArrayList;
import java.util.List;

import Enumeraciones.EstadoReproduccion;
import Interfaces.Reproducible;

// Reproductor en consola
public class ReproductorMusical {

    private List<Reproducible> cola = new ArrayList<>();
    private int indiceActual = 0;
    private EstadoReproduccion estado = EstadoReproduccion.DETENIDO;
    private int volumen = 50;

    // Reemplaza la cola por las canciones del elemento (cancion, album o playlist) y empieza a reproducir.
    public void reproducir(Reproducible elemento) {
        cola.clear();
        agregarACola(elemento);

        if (cola.isEmpty()) {
            estado = EstadoReproduccion.DETENIDO;
            System.out.println("'" + elemento.getTitulo() + "' no tiene canciones para reproducir.");
            return;
        }

        indiceActual = 0;
        estado = EstadoReproduccion.REPRODUCIENDO;
        mostrarActual();
    }

    // Agrega las canciones del elemento al final de la cola, sin cortar lo que suena.
    public void agregarACola(Reproducible elemento) {
        for (Cancion c : elemento.getCanciones()) {
            cola.add(c);
        }
    }

    public void pausar() {
        if (estado != EstadoReproduccion.REPRODUCIENDO) {
            System.out.println("No hay nada reproduciendose.");
            return;
        }
        estado = EstadoReproduccion.PAUSADO;
        System.out.println("Pausado: " + cola.get(indiceActual).getTitulo());
    }

    public void reanudar() {
        if (estado != EstadoReproduccion.PAUSADO) {
            System.out.println("No hay nada en pausa.");
            return;
        }
        estado = EstadoReproduccion.REPRODUCIENDO;
        mostrarActual();
    }

    public void detener() {
        estado = EstadoReproduccion.DETENIDO;
        indiceActual = 0;
        System.out.println("Reproduccion detenida.");
    }

    public void siguiente() {
        if (cola.isEmpty()) {
            System.out.println("La cola esta vacia.");
            return;
        }
        if (indiceActual < cola.size() - 1) {
            indiceActual++;
            estado = EstadoReproduccion.REPRODUCIENDO;
            mostrarActual();
        } else {
            detener();
            System.out.println("Se termino la cola.");
        }
    }

    public void anterior() {
        if (cola.isEmpty()) {
            System.out.println("La cola esta vacia.");
            return;
        }
        if (indiceActual > 0) {
            indiceActual--;
        }
        estado = EstadoReproduccion.REPRODUCIENDO;
        mostrarActual();
    }

    public void mostrarCola() {
        if (cola.isEmpty()) {
            System.out.println("La cola esta vacia.");
            return;
        }
        for (int i = 0; i < cola.size(); i++) {
            String marca = (i == indiceActual) ? "> " : "  ";
            System.out.println(marca + (i + 1) + ". " + cola.get(i).getTitulo()
                    + " (" + formatear(cola.get(i).getDuracionTotal()) + ")");
        }
        System.out.println("Estado: " + estado + " | Volumen: " + volumen);
    }

    public void setVolumen(int volumen) {
        if (volumen < 0 || volumen > 100) {
            throw new IllegalArgumentException("El volumen va de 0 a 100.");
        }
        this.volumen = volumen;
        System.out.println("Volumen: " + volumen);
    }

    public int getVolumen() {
        return volumen;
    }

    public EstadoReproduccion getEstado() {
        return estado;
    }

    public List<Reproducible> getCola() {
        return new ArrayList<>(cola);
    }

    // Reemplaza la cola por la lista indicada y arranca en el indice pedido.
    public void reproducirLista(List<? extends Reproducible> lista, int indice) {
        detener();
        for (Reproducible elemento : lista) {
            agregarACola(elemento);
        }
        if (lista.isEmpty()) {
            return;
        }
        indiceActual = Math.max(0, Math.min(indice, lista.size() - 1));
        estado = EstadoReproduccion.REPRODUCIENDO;
    }

    public int getIndiceActual() {
        return indiceActual;
    }

    public Reproducible getActual() {
        return cola.isEmpty() ? null : cola.get(indiceActual);
    }

    private void mostrarActual() {
        Reproducible actual = cola.get(indiceActual);
        System.out.println("Reproduciendo (" + (indiceActual + 1) + "/" + cola.size() + "): "
                + actual.getTitulo() + " (" + formatear(actual.getDuracionTotal()) + ")");
    }

    private String formatear(int segundos) {
        return segundos / 60 + ":" + String.format("%02d", segundos % 60);
    }
}