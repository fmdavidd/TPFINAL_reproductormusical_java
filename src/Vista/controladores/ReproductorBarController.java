package Vista.controladores;

import Vista.App;

import java.nio.file.Files;
import java.nio.file.Path;

import Enumeraciones.EstadoReproduccion;
import Interfaces.Reproducible;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import modelo.Album;
import modelo.Cancion;

public class ReproductorBarController {

    @FXML private Label lblActual;
    @FXML private Label lblTiempo;
    @FXML private Label lblEstado;
    @FXML private Label lblPortada;
    @FXML private ImageView imgPortada;
    @FXML private Button btnPlay;
    @FXML private Slider sldVolumen;
    @FXML private Slider sldProgreso;

    private final ReproductorAudioFX reproductor = ReproductorAudioFX.getInstancia();
    private boolean sincronizandoProgreso = false;

    @FXML
    private void initialize() {
        reproductor.actualProperty().addListener((obs, anterior, nuevo) -> {
            actualizarTitulo(nuevo);
            actualizarPortada(nuevo);
            actualizarTiempo();
        });
        reproductor.estadoProperty().addListener((obs, anterior, nuevo) -> actualizarEstado(nuevo));
        reproductor.posicionProperty().addListener((obs, anterior, nuevo) -> actualizarTiempo());
        reproductor.duracionProperty().addListener((obs, anterior, nuevo) -> actualizarTiempo());

        sldVolumen.setValue(reproductor.getVolumen());
        sldVolumen.valueProperty().addListener((obs, anterior, nuevo) -> {
            int valor = (int) Math.round(nuevo.doubleValue());
            if (Math.abs(valor - reproductor.getVolumen()) > 0.5) {
                reproductor.setVolumen(valor);
            }
        });

        sldProgreso.setValue(0);
        sldProgreso.valueProperty().addListener((obs, anterior, nuevo) -> {
            if (sincronizandoProgreso) {
                return;
            }
            double duracion = reproductor.getDuracionSegundos();
            if (duracion <= 0) {
                return;
            }
            double segundos = nuevo.doubleValue() * duracion;
            lblTiempo.setText(formatear(segundos, duracion));
            reproductor.buscar(segundos);
        });

        actualizarTitulo(reproductor.getActual());
        actualizarPortada(reproductor.getActual());
        actualizarEstado(reproductor.estadoProperty().get());
        actualizarTiempo();
    }

    private void actualizarTitulo(Reproducible elemento) {
        lblActual.setText(ReproductorAudioFX.tituloDe(elemento));
    }

    private void actualizarEstado(EstadoReproduccion estado) {
        lblEstado.setText(estado == null ? "" : estado.name());
        btnPlay.setText(estado == EstadoReproduccion.REPRODUCIENDO ? "Pausa" : "Play");
    }

    private void actualizarTiempo() {
        Reproducible elemento = reproductor.getActual();
        double duracion = reproductor.getDuracionSegundos();
        double posicion = reproductor.getPosicionSegundos();
        sldProgreso.setDisable(elemento == null || duracion <= 0);

        if (elemento == null) {
            lblTiempo.setText("");
            sincronizarProgreso(0);
            return;
        }
        if (duracion <= 0) {
            lblTiempo.setText("00:00 / --:--");
            sincronizarProgreso(0);
            return;
        }
        if (!sldProgreso.isValueChanging()) {
            sincronizarProgreso(Math.max(0, Math.min(1, posicion / duracion)));
            lblTiempo.setText(formatear(posicion, duracion));
        }
    }

    // evita que actualizar la barra desde el audio dispare otra busqueda de posicion
    private void sincronizarProgreso(double fraccion) {
        sincronizandoProgreso = true;
        sldProgreso.setValue(fraccion);
        sincronizandoProgreso = false;
    }

    private String formatear(double posicion, double duracion) {
        return Ui.duracion((int) Math.floor(posicion)) + " / " + Ui.duracion((int) Math.round(duracion));
    }

    private void actualizarPortada(Reproducible elemento) {
        imgPortada.setImage(null);
        lblPortada.setVisible(true);
        if (!(elemento instanceof Cancion)) {
            return;
        }
        Album album = ((Cancion) elemento).getAlbum();
        if (album == null || album.getPortada() == null || album.getPortada().isBlank()) {
            return;
        }
        String ruta = album.getPortada().trim();
        Image imagen;
        try {
            if (ruta.startsWith("http://") || ruta.startsWith("https://")) {
                imagen = new Image(ruta, 84, 84, true, true, true);
            } else {
                Path archivo = Ui.resolverArchivo(ruta);
                if (archivo == null || !Files.isRegularFile(archivo)) {
                    return;
                }
                imagen = new Image(archivo.toUri().toString(), 84, 84, true, true, true);
            }
        } catch (IllegalArgumentException e) {
            Ui.error("Portada", "No se pudo cargar la portada:\n" + Ui.mensaje(e));
            return;
        }
        imgPortada.setImage(imagen);
        imagen.errorProperty().addListener((obs, anterior, error) -> {
            if (Boolean.TRUE.equals(error) && imgPortada.getImage() == imagen) {
                imgPortada.setImage(null);
                lblPortada.setVisible(true);
            }
        });
        imagen.progressProperty().addListener((obs, anterior, progreso) -> {
            if (progreso != null && progreso.doubleValue() >= 1 && !imagen.isError()
                    && imgPortada.getImage() == imagen) {
                lblPortada.setVisible(false);
            }
        });
    }

    @FXML
    private void alternar() {
        reproductor.alternar();
    }

    @FXML
    private void detener() {
        reproductor.detener();
    }

    @FXML
    private void limpiarCola() {
        reproductor.limpiarCola();
    }

    @FXML
    private void siguiente() {
        reproductor.siguiente();
    }

    @FXML
    private void anterior() {
        reproductor.anterior();
    }

    @FXML
    private void verCola() {
        ListView<String> lista = new ListView<>();
        int posicion = 1;
        for (Reproducible elemento : reproductor.getCola()) {
            lista.getItems().add(posicion++ + ". " + ReproductorAudioFX.tituloDe(elemento));
        }
        if (lista.getItems().isEmpty()) {
            lista.getItems().add("La cola esta vacia");
        }
        VBox caja = new VBox(8, new Label("Cola de reproduccion"), lista);
        caja.setPrefSize(440, 320);

        Stage ventana = new Stage();
        ventana.setTitle("Cola");
        ventana.setScene(new Scene(caja));
        ventana.show();
    }
}
