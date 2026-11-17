package Vista.controladores;

import Vista.App;

import Excepciones.ElementoNoEncontradoException;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import modelo.Cancion;
import modelo.Usuario;
import modelo.UsuarioNormal;

public class FavoritosController {

    @FXML private Label lblResumen;
    @FXML private TableView<Cancion> tblFavoritos;
    @FXML private TableColumn<Cancion, String> colTitulo;
    @FXML private TableColumn<Cancion, String> colArtista;
    @FXML private TableColumn<Cancion, String> colAlbum;
    @FXML private TableColumn<Cancion, String> colGenero;
    @FXML private TableColumn<Cancion, String> colDuracion;

    @FXML
    private void initialize() {
        Ui.configurar(colTitulo, Cancion::getTitulo);
        Ui.configurar(colArtista, c -> c.getArtista() != null ? c.getArtista().getNombre() : "?");
        Ui.configurar(colAlbum, c -> c.getAlbum() != null ? c.getAlbum().getTitulo() : "Sin album");
        Ui.configurar(colGenero, c -> c.getGenero() != null ? c.getGenero().name() : "");
        Ui.configurar(colDuracion, c -> Ui.duracion(c.getDuracionSegundos()));

        tblFavoritos.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Cancion> fila = new javafx.scene.control.TableRow<>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && fila.getItem() != null) {
                    ReproductorAudioFX.getInstancia().reproducir(fila.getItem());
                }
            });
            return fila;
        });

        recargar();
    }

    @FXML
    private void recargar() {
        Usuario usuario = Reglas.usuario();
        if (!(usuario instanceof UsuarioNormal)) {
            lblResumen.setText("Solo los usuarios normales tienen favoritos.");
            tblFavoritos.getItems().clear();
            return;
        }

        List<Cancion> canciones = new ArrayList<>();
        int faltantes = 0;
        for (Integer id : ((UsuarioNormal) usuario).getFavoritos()) {
            try {
                canciones.add(App.getCanciones().buscarPorId(id));
            } catch (ElementoNoEncontradoException e) {
                faltantes++;
            }
        }
        if (faltantes > 0) {
            Ui.error("Favoritos", "No se pudieron cargar " + faltantes
                    + " cancion(es) favorita(s) porque ya no estan activas.");
        }
        tblFavoritos.getItems().setAll(canciones);
        int total = 0;
        for (Cancion cancion : canciones) {
            total += cancion.getDuracionSegundos();
        }
        lblResumen.setText(canciones.size() + " canciones - " + Ui.duracion(total));
    }

    @FXML
    private void reproducir() {
        Cancion cancion = tblFavoritos.getSelectionModel().getSelectedItem();
        if (cancion == null) {
            Ui.info("Favoritos", "Selecciona una cancion.");
            return;
        }
        int indice = tblFavoritos.getSelectionModel().getSelectedIndex();
        ReproductorAudioFX.getInstancia().reproducirLista(tblFavoritos.getItems(), indice);
    }

    @FXML
    private void quitar() {
        Usuario usuario = Reglas.usuario();
        Cancion cancion = tblFavoritos.getSelectionModel().getSelectedItem();
        if (!(usuario instanceof UsuarioNormal) || cancion == null) {
            return;
        }
        if (Ui.ejecutar("Favoritos", () -> App.getUsuarios().quitarFavorito(
                usuario.getIdUsuario(), cancion.getIdCancion()))) {
            recargar();
        }
    }

    @FXML
    private void agregarAPlaylist() {
        AgregarAPlaylistDialog.mostrar(tblFavoritos.getSelectionModel().getSelectedItem());
    }
}
