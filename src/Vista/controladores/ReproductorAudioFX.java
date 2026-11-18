package Vista.controladores;

import Vista.App;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import Enumeraciones.EstadoReproduccion;
import Interfaces.Reproducible;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import modelo.Cancion;
import modelo.ReproductorMusical;

// Une la cola logica del modelo con un MediaPlayer real de JavaFX.
public final class ReproductorAudioFX {

    private static final ReproductorAudioFX INSTANCIA = new ReproductorAudioFX();

    private final ReproductorMusical cola = new ReproductorMusical();
    private MediaPlayer media;

    private final ObjectProperty<Reproducible> actual = new SimpleObjectProperty<>();
    private final ObjectProperty<EstadoReproduccion> estado =
            new SimpleObjectProperty<>(EstadoReproduccion.DETENIDO);
    private final DoubleProperty volumen = new SimpleDoubleProperty(50);
    private final ReadOnlyDoubleWrapper posicion = new ReadOnlyDoubleWrapper(0);
    private final ReadOnlyDoubleWrapper duracion = new ReadOnlyDoubleWrapper(0);

    private ReproductorAudioFX() {
        volumen.addListener((obs, anterior, nuevo) -> {
            if (media != null) {
                media.setVolume(nuevo.doubleValue() / 100.0);
            }
        });
    }

    public static ReproductorAudioFX getInstancia() {
        return INSTANCIA;
    }

    public void reproducir(Reproducible elemento) {
        cola.reproducir(elemento);
        sincronizar(true);
    }

    public void agregarACola(Reproducible elemento) {
        cola.agregarACola(elemento);
        if (media == null) {
            actual.set(cola.getActual());
        }
    }

    // Reproduce una lista completa (album, playlist, artista) desde un indice.
    public void reproducirLista(List<Cancion> canciones, int indice) {
        List<Reproducible> elementos = new ArrayList<>();
        elementos.addAll(canciones);
        if (elementos.isEmpty()) {
            return;
        }
        cola.reproducirLista(elementos, indice);
        sincronizar(true);
    }

    public void alternar() {
        if (media == null) {
            if (cola.getActual() != null) {
                sincronizar(true);
            }
            return;
        }
        if (media.getStatus() == MediaPlayer.Status.PLAYING) {
            cola.pausar();
            media.pause();
            estado.set(EstadoReproduccion.PAUSADO);
        } else if (cola.getEstado() == EstadoReproduccion.PAUSADO) {
            cola.reanudar();
            media.play();
            estado.set(EstadoReproduccion.REPRODUCIENDO);
        } else {
            sincronizar(true);
        }
    }

    public void detener() {
        cola.detener();
        liberarMedia();
        posicion.set(0);
        duracion.set(0);
        actual.set(null);
        estado.set(EstadoReproduccion.DETENIDO);
    }

    public void limpiarCola() {
        detener();
    }

    public void siguiente() {
        cola.siguiente();
        if (cola.getEstado() == EstadoReproduccion.DETENIDO) {
            conservarFinal();
        } else {
            sincronizar(true);
        }
    }

    public void siguienteAlTerminar() {
        cola.avanzarAlTerminar();
        if (cola.getEstado() == EstadoReproduccion.DETENIDO) {
            conservarFinal();
        } else {
            sincronizar(true);
        }
    }

    private void conservarFinal() {
        if (media != null) {
            media.pause();
            media.seek(Duration.seconds(duracion.get()));
        }
        posicion.set(duracion.get());
        estado.set(EstadoReproduccion.DETENIDO);
    }

    public void anterior() {
        cola.anterior();
        sincronizar(true);
    }

    public void setVolumen(int valor) {
        int volumenSeguro = Math.max(0, Math.min(100, valor));
        cola.setVolumen(volumenSeguro);
        volumen.set(volumenSeguro);
    }

    public double getVolumen() {
        return volumen.get();
    }

    public DoubleProperty volumenProperty() {
        return volumen;
    }

    public ReadOnlyDoubleProperty posicionProperty() {
        return posicion.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty duracionProperty() {
        return duracion.getReadOnlyProperty();
    }

    public double getPosicionSegundos() {
        return posicion.get();
    }

    public double getDuracionSegundos() {
        return duracion.get();
    }

    public void buscar(double segundos) {
        if (media == null) {
            return;
        }
        double total = duracion.get();
        double destino = Math.max(0, Math.min(segundos, total > 0 ? total : segundos));
        media.seek(Duration.seconds(destino));
    }

    public ReadOnlyObjectProperty<Reproducible> actualProperty() {
        return actual;
    }

    public ObjectProperty<EstadoReproduccion> estadoProperty() {
        return estado;
    }

    public List<Reproducible> getCola() {
        return new ArrayList<>(cola.getCola());
    }

    public Reproducible getActual() {
        return actual.get();
    }

    public static String tituloDe(Reproducible elemento) {
        if (elemento == null) {
            return "Sin reproduccion";
        }
        if (elemento instanceof Cancion) {
            Cancion cancion = (Cancion) elemento;
            String artista = cancion.getArtista() != null ? cancion.getArtista().getNombre() : "?";
            return cancion.getTitulo() + " - " + artista;
        }
        return elemento.getTitulo();
    }

    public void dispose() {
        detener();
    }

    private void sincronizar(boolean autoPlay) {
        Reproducible elemento = cola.getActual();
        posicion.set(0);
        duracion.set(elemento instanceof Cancion
                ? Math.max(0, ((Cancion) elemento).getDuracionSegundos()) : 0);
        actual.set(elemento);
        liberarMedia();
        if (!(elemento instanceof Cancion)) {
            estado.set(EstadoReproduccion.DETENIDO);
            return;
        }

        Cancion cancion = (Cancion) elemento;
        Path archivo = Ui.resolverArchivo(cancion.getRutaArchivo());
        if (archivo == null) {
            duracion.set(0);
            estado.set(EstadoReproduccion.DETENIDO);
            Ui.error("Reproductor", "No se encontro el archivo de audio:\n" + cancion.getRutaArchivo());
            return;
        }

        try {
            Media recurso = new Media(archivo.toUri().toString());
            media = new MediaPlayer(recurso);
            media.setVolume(volumen.get() / 100.0);
            media.setOnEndOfMedia(this::siguienteAlTerminar);
            media.currentTimeProperty().addListener((obs, anterior, nuevo) ->
                    posicion.set(nuevo == null ? 0 : nuevo.toSeconds()));
            // al cargar el audio usamos su duracion real en lugar de la guardada en la BD
            media.totalDurationProperty().addListener((obs, anterior, nuevo) -> {
                if (nuevo != null && nuevo.toSeconds() > 0) {
                    duracion.set(nuevo.toSeconds());
                }
            });
            media.setOnReady(() -> {
                if (media != null && media.getTotalDuration() != null
                        && media.getTotalDuration().toSeconds() > 0) {
                    duracion.set(media.getTotalDuration().toSeconds());
                }
            });
            media.setOnError(() -> {
                duracion.set(0);
                estado.set(EstadoReproduccion.DETENIDO);
                MediaException error = media != null ? media.getError() : null;
                Ui.error("Reproductor", error != null
                        ? "No se pudo reproducir el archivo:\n" + Ui.mensaje(error)
                        : "No se pudo reproducir el archivo.");
            });
            estado.set(EstadoReproduccion.REPRODUCIENDO);
            if (autoPlay) {
                media.play();
            }
        } catch (MediaException | IllegalArgumentException e) {
            duracion.set(0);
            estado.set(EstadoReproduccion.DETENIDO);
            Ui.error("Reproductor", "No se pudo reproducir el archivo:\n" + Ui.mensaje(e));
        }
    }

    private void liberarMedia() {
        if (media != null) {
            media.stop();
            // libera el archivo y los recursos del reproductor anterior
            media.dispose();
            media = null;
        }
    }
}
