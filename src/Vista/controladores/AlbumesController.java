package Vista.controladores;

import Vista.App;

import Excepciones.ElementoNoEncontradoException;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import modelo.Album;
import modelo.Artista;
import modelo.Cancion;

public class AlbumesController {

    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkInactivos;
    @FXML private TableView<Album> tblAlbumes;
    @FXML private TableColumn<Album, String> colTitulo;
    @FXML private TableColumn<Album, String> colArtista;
    @FXML private TableColumn<Album, String> colAnio;
    @FXML private TableColumn<Album, String> colCanciones;
    @FXML private TableColumn<Album, String> colEstado;
    @FXML private ListView<Cancion> lstCanciones;

    @FXML private VBox formulario;
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtTitulo;
    @FXML private TextField txtAnio;
    @FXML private ComboBox<Artista> cmbArtista;
    @FXML private TextField txtPortada;
    @FXML private ComboBox<Cancion> cmbCancion;
    @FXML private Button btnBaja;
    @FXML private Button btnReactivar;
    @FXML private Label lblMensaje;

    private Album seleccionado;

    @FXML
    private void initialize() {
        tblAlbumes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Ui.configurar(colTitulo, Album::getTitulo);
        Ui.configurar(colArtista, a -> a.getArtista() != null ? a.getArtista().getNombre() : "?");
        Ui.configurar(colAnio, a -> String.valueOf(a.getAnioLanzamiento()));
        Ui.configurar(colCanciones, a -> String.valueOf(contarCanciones(a)));
        Ui.configurar(colEstado, a -> a.isActivo() ? "Activo" : "Inactivo");

        cmbArtista.setConverter(Ui.conversor(Artista::getNombre, "(sin artista)"));
        cmbArtista.getItems().setAll(App.getArtistas().listar());
        if (!cmbArtista.getItems().isEmpty()) {
            cmbArtista.getSelectionModel().selectFirst();
        }
        cmbCancion.setConverter(Ui.conversor(Ui::textoCancion));

        lstCanciones.setCellFactory(lv -> Ui.celda(Ui::textoCancion));
        lstCanciones.setOnMouseClicked(evento -> {
            if (evento.getClickCount() == 2) {
                reproducir();
            }
        });

        boolean editor = Reglas.puedeEditarContenido();
        formulario.setVisible(editor);
        formulario.setManaged(editor);
        chkInactivos.setVisible(editor);
        chkInactivos.setManaged(editor);

        tblAlbumes.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, nuevo) -> mostrar(nuevo));

        recargar();
        nuevo();
    }

    private int contarCanciones(Album album) {
        try {
            return App.getAlbumes().listarCanciones(album.getIdAlbum()).size();
        } catch (ElementoNoEncontradoException e) {
            Ui.errorUnaVez("album-" + album.getIdAlbum(), "Albumes", Ui.mensaje(e));
            return 0;
        }
    }

    @FXML
    private void recargar() {
        Integer seleccionadoId = seleccionado != null ? seleccionado.getIdAlbum() : null;
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().trim().toLowerCase() : "";
        List<Album> albumes = new ArrayList<>();
        for (Album album : App.getAlbumes().listar()) {
            if (texto.isEmpty() || album.getTitulo().toLowerCase().contains(texto)) {
                albumes.add(album);
            }
        }

        if (chkInactivos.isSelected()) {
            Ui.ejecutar("Albumes", () -> {
                for (Album inactivo : App.getAlbumes().listarInactivos()) {
                    boolean repetido = false;
                    for (Album activo : albumes) {
                        if (activo.getIdAlbum() == inactivo.getIdAlbum()) {
                            repetido = true;
                            break;
                        }
                    }
                    if (!repetido) {
                        albumes.add(inactivo);
                    }
                }
            });
        }

        tblAlbumes.getItems().setAll(albumes);
        if (seleccionadoId != null) {
            seleccionar(seleccionadoId);
        }
    }

    private void seleccionar(int id) {
        for (Album a : tblAlbumes.getItems()) {
            if (a.getIdAlbum() == id) {
                tblAlbumes.getSelectionModel().select(a);
                return;
            }
        }
    }

    private void mostrar(Album album) {
        seleccionado = album;
        if (album == null) {
            lstCanciones.getItems().clear();
            return;
        }
        lblFormTitulo.setText("Album #" + album.getIdAlbum());
        txtTitulo.setText(album.getTitulo());
        txtAnio.setText(String.valueOf(album.getAnioLanzamiento()));
        txtPortada.setText(album.getPortada());
        seleccionarArtista(album.getArtista());

        List<Cancion> canciones = cancionesDelAlbum(album);
        lstCanciones.getItems().setAll(canciones);
        actualizarComboCanciones(canciones);

        boolean activo = album.isActivo();
        btnBaja.setDisable(!activo);
        btnReactivar.setDisable(activo);
    }

    private void seleccionarArtista(Artista artista) {
        if (artista == null) {
            cmbArtista.getSelectionModel().clearSelection();
            return;
        }
        for (int i = 0; i < cmbArtista.getItems().size(); i++) {
            if (cmbArtista.getItems().get(i).getIdArtista() == artista.getIdArtista()) {
                cmbArtista.getSelectionModel().select(i);
                return;
            }
        }
    }

    private List<Cancion> cancionesDelAlbum(Album album) {
        try {
            return App.getAlbumes().listarCanciones(album.getIdAlbum());
        } catch (ElementoNoEncontradoException e) {
            Ui.error("Albumes", e);
            return new ArrayList<>();
        }
    }

    private void actualizarComboCanciones(List<Cancion> enAlbum) {
        List<Cancion> disponibles = new ArrayList<>();
        for (Cancion cancion : App.getCanciones().listar()) {
            boolean yaEsta = false;
            for (Cancion delAlbum : enAlbum) {
                if (delAlbum.getIdCancion() == cancion.getIdCancion()) {
                    yaEsta = true;
                    break;
                }
            }
            if (!yaEsta) {
                disponibles.add(cancion);
            }
        }
        cmbCancion.setItems(FXCollections.observableArrayList(disponibles));
        if (!disponibles.isEmpty()) {
            cmbCancion.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void nuevo() {
        seleccionado = null;
        lblFormTitulo.setText("Nuevo album");
        txtTitulo.clear();
        txtAnio.clear();
        txtPortada.clear();
        if (!cmbArtista.getItems().isEmpty()) {
            cmbArtista.getSelectionModel().selectFirst();
        }
        lstCanciones.getItems().clear();
        actualizarComboCanciones(new ArrayList<>());
        btnBaja.setDisable(true);
        btnReactivar.setDisable(true);
        lblMensaje.setText("");
        tblAlbumes.getSelectionModel().clearSelection();
    }

    @FXML
    private void buscar() {
        recargar();
    }

    @FXML
    private void limpiarBusqueda() {
        txtBuscar.clear();
        recargar();
    }

    @FXML
    private void guardar() {
        if (!Reglas.puedeEditarContenido()) {
            return;
        }
        String titulo = txtTitulo.getText() != null ? txtTitulo.getText().trim() : "";
        Artista artista = cmbArtista.getValue();
        int anio = parsear(txtAnio.getText(), 0);
        String portada = txtPortada.getText() != null ? txtPortada.getText().trim() : "";

        if (titulo.isEmpty()) {
            lblMensaje.setText("El titulo es obligatorio.");
            return;
        }
        if (artista == null) {
            lblMensaje.setText("Selecciona un artista.");
            return;
        }

        boolean ok;
        if (seleccionado == null) {
            Album nuevo = new Album(titulo, anio, portada, artista, true);
            ok = Ui.ejecutar("Guardar album", () -> App.getAlbumes().alta(nuevo));
            if (ok) {
                Reglas.registrarAccion("Alta del album '" + titulo + "'");
                seleccionado = nuevo;
            }
        } else {
            seleccionado.setTitulo(titulo);
            seleccionado.setAnioLanzamiento(anio);
            seleccionado.setPortada(portada);
            seleccionado.setArtista(artista);
            ok = Ui.ejecutar("Guardar album", () -> App.getAlbumes().modificar(seleccionado));
            if (ok) {
                Reglas.registrarAccion("Modificacion del album '" + titulo + "'");
            }
        }

        if (ok) {
            lblMensaje.setText("Cambios guardados.");
            recargar();
            if (seleccionado != null) {
                seleccionar(seleccionado.getIdAlbum());
            }
        }
    }

    @FXML
    private void agregarCancion() {
        if (seleccionado == null) {
            lblMensaje.setText("Guarda el album antes de agregar canciones.");
            return;
        }
        Cancion cancion = cmbCancion.getValue();
        if (cancion == null) {
            lblMensaje.setText("No hay canciones disponibles para agregar.");
            return;
        }
        cancion.setAlbum(seleccionado);
        if (Ui.ejecutar("Agregar cancion", () -> App.getCanciones().modificar(cancion))) {
            Reglas.registrarAccion("Cancion '" + cancion.getTitulo()
                    + "' agregada al album '" + seleccionado.getTitulo() + "'");
            recargar();
            seleccionar(seleccionado.getIdAlbum());
        }
    }

    @FXML
    private void quitarCancion() {
        Cancion cancion = lstCanciones.getSelectionModel().getSelectedItem();
        if (cancion == null) {
            lblMensaje.setText("Selecciona una cancion del album.");
            return;
        }
        cancion.setAlbum(null);
        if (Ui.ejecutar("Quitar cancion", () -> App.getCanciones().modificar(cancion))) {
            Reglas.registrarAccion("Cancion '" + cancion.getTitulo() + "' quitada de su album");
            recargar();
            if (seleccionado != null) {
                seleccionar(seleccionado.getIdAlbum());
            }
        }
    }

    @FXML
    private void darDeBaja() {
        if (seleccionado == null || !Reglas.puedeEditarContenido()) {
            return;
        }
        if (!Ui.confirmar("Dar de baja", "Dar de baja el album '" + seleccionado.getTitulo() + "'?")) {
            return;
        }
        int id = seleccionado.getIdAlbum();
        String titulo = seleccionado.getTitulo();
        if (Ui.ejecutar("Dar de baja", () -> App.getAlbumes().baja(id))) {
            Reglas.registrarAccion("Baja del album '" + titulo + "'");
            nuevo();
            recargar();
        }
    }

    @FXML
    private void reactivar() {
        if (seleccionado == null || !Reglas.puedeEditarContenido()) {
            return;
        }
        int id = seleccionado.getIdAlbum();
        if (Ui.ejecutar("Reactivar", () -> App.getAlbumes().reactivar(id))) {
            Reglas.registrarAccion("Reactivacion del album #" + id);
            recargar();
            seleccionar(id);
        }
    }

    @FXML
    private void reproducir() {
        if (lstCanciones.getItems().isEmpty()) {
            Ui.info("Albumes", "El album no tiene canciones.");
            return;
        }
        int indice = Math.max(0, lstCanciones.getSelectionModel().getSelectedIndex());
        ReproductorAudioFX.getInstancia().reproducirLista(lstCanciones.getItems(), indice);
    }

    @FXML
    private void agregarAPlaylist() {
        AgregarAPlaylistDialog.mostrar(lstCanciones.getSelectionModel().getSelectedItem());
    }

    private int parsear(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            Ui.error("Album", "El anio debe ser un numero entero.");
            return porDefecto;
        }
    }
}
