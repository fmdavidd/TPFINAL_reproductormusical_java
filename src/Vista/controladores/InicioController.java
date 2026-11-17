package Vista.controladores;

import Vista.App;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import modelo.Album;
import modelo.Cancion;
import modelo.Playlist;
import modelo.Usuario;

public class InicioController {

    @FXML private Label lblBienvenida;
    @FXML private Label lblResumen;
    @FXML private TableView<Cancion> tblRecientes;
    @FXML private TableColumn<Cancion, String> colTitulo;
    @FXML private TableColumn<Cancion, String> colArtista;
    @FXML private TableColumn<Cancion, String> colAlbum;
    @FXML private TableColumn<Cancion, String> colDuracion;
    @FXML private ListView<Playlist> lstPlaylists;

    @FXML
    private void initialize() {
        Usuario usuario = Reglas.usuario();
        lblBienvenida.setText("Hola, " + (usuario != null ? usuario.getNombre() : ""));
        lblResumen.setText(Reglas.descripcionRol() + "  |  "
                + App.getCanciones().listar().size() + " canciones  |  "
                + App.getArtistas().listar().size() + " artistas  |  "
                + App.getAlbumes().listar().size() + " albumes");

        Ui.configurar(colTitulo, Cancion::getTitulo);
        Ui.configurar(colArtista, c -> c.getArtista() != null ? c.getArtista().getNombre() : "?");
        Ui.configurar(colAlbum, c -> c.getAlbum() != null ? c.getAlbum().getTitulo() : "Sin album");
        Ui.configurar(colDuracion, c -> Ui.duracion(c.getDuracionSegundos()));

        List<Cancion> todas = new ArrayList<>(App.getCanciones().listar());
        todas.sort(Comparator.comparingInt(Cancion::getIdCancion).reversed());
        tblRecientes.getItems().setAll(todas.size() > 8 ? todas.subList(0, 8) : todas);

        if (Reglas.esNormal()) {
            lstPlaylists.getItems().setAll(App.getPlaylists().listarPorUsuario(usuario.getIdUsuario()));
            lstPlaylists.setCellFactory(lv -> Ui.celda(p ->
                    p.getNombre() + " (" + p.getCanciones().size() + " canciones)"));
        } else {
            List<Playlist> visibles = new ArrayList<>();
            for (Playlist p : App.getPlaylists().listar()) {
                if (Reglas.puedeVerPlaylist(p)) {
                    visibles.add(p);
                }
            }
            lstPlaylists.getItems().setAll(visibles);
            lstPlaylists.setCellFactory(lv -> Ui.celda(Playlist::getNombre));
        }

        tblRecientes.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Cancion> fila = new javafx.scene.control.TableRow<>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && fila.getItem() != null) {
                    ReproductorAudioFX.getInstancia().reproducir(fila.getItem());
                }
            });
            return fila;
        });

        lstPlaylists.setOnMouseClicked(evento -> {
            if (evento.getClickCount() == 2 && lstPlaylists.getSelectionModel().getSelectedItem() != null) {
                reproducirPlaylist();
            }
        });
    }

    @FXML
    private void reproducirSeleccion() {
        Cancion cancion = tblRecientes.getSelectionModel().getSelectedItem();
        if (cancion == null) {
            Ui.info("Inicio", "Selecciona una cancion.");
            return;
        }
        ReproductorAudioFX.getInstancia().reproducir(cancion);
    }

    @FXML
    private void reproducirPlaylist() {
        Playlist playlist = lstPlaylists.getSelectionModel().getSelectedItem();
        if (playlist == null) {
            Ui.info("Inicio", "Selecciona una playlist.");
            return;
        }
        List<Cancion> canciones = playlist.getCanciones();
        if (canciones.isEmpty()) {
            Ui.info("Inicio", "La playlist esta vacia.");
            return;
        }
        ReproductorAudioFX.getInstancia().reproducirLista(canciones, 0);
    }
}
