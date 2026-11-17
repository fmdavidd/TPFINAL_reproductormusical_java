package Vista.controladores;

import Vista.App;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import Enumeraciones.Genero;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import modelo.Album;
import modelo.Artista;
import modelo.Cancion;
import modelo.Usuario;
import modelo.UsuarioNormal;

public class CancionesController {

    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkInactivas;
    @FXML private TableView<Cancion> tblCanciones;
    @FXML private TableColumn<Cancion, String> colTitulo;
    @FXML private TableColumn<Cancion, String> colArtista;
    @FXML private TableColumn<Cancion, String> colAlbum;
    @FXML private TableColumn<Cancion, String> colGenero;
    @FXML private TableColumn<Cancion, String> colDuracion;
    @FXML private TableColumn<Cancion, String> colEstado;
    @FXML private Button btnFavorito;
    @FXML private Button btnAgregarPlaylist;

    @FXML private VBox formulario;
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtTitulo;
    @FXML private ComboBox<Artista> cmbArtista;
    @FXML private ComboBox<Album> cmbAlbum;
    @FXML private ComboBox<Genero> cmbGenero;
    @FXML private TextField txtDuracion;
    @FXML private TextField txtRuta;
    @FXML private Button btnGuardar;
    @FXML private Button btnBaja;
    @FXML private Button btnReactivar;
    @FXML private Label lblMensaje;

    private Cancion seleccionada;

    @FXML
    private void initialize() {
        tblCanciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Ui.configurar(colTitulo, Cancion::getTitulo);
        Ui.configurar(colArtista, c -> c.getArtista() != null ? c.getArtista().getNombre() : "?");
        Ui.configurar(colAlbum, c -> c.getAlbum() != null ? c.getAlbum().getTitulo() : "Sin album");
        Ui.configurar(colGenero, c -> c.getGenero() != null ? c.getGenero().name() : "");
        Ui.configurar(colDuracion, c -> Ui.duracion(c.getDuracionSegundos()));
        Ui.configurar(colEstado, c -> c.isActivo() ? "Activa" : "Inactiva");

        cmbGenero.getItems().setAll(Genero.values());
        cmbGenero.setConverter(Ui.conversor(Genero::name));
        cmbGenero.getSelectionModel().selectFirst();

        cmbArtista.setConverter(Ui.conversor(Artista::getNombre, "(sin artista)"));
        cmbAlbum.setConverter(Ui.conversor(Album::getTitulo, "Sin album"));
        cargarArtistas();

        cmbArtista.valueProperty().addListener((obs, anterior, nuevo) ->
                cargarAlbumes(nuevo != null ? nuevo.getIdArtista() : 0));

        boolean editor = Reglas.puedeEditarContenido();
        formulario.setVisible(editor);
        formulario.setManaged(editor);
        chkInactivas.setVisible(editor);
        chkInactivas.setManaged(editor);
        btnFavorito.setVisible(Reglas.esNormal());
        btnFavorito.setManaged(Reglas.esNormal());
        btnAgregarPlaylist.setVisible(Reglas.esNormal());
        btnAgregarPlaylist.setManaged(Reglas.esNormal());

        tblCanciones.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, nuevo) -> mostrar(nuevo));
        tblCanciones.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Cancion> fila = new javafx.scene.control.TableRow<>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && fila.getItem() != null) {
                    ReproductorAudioFX.getInstancia().reproducir(fila.getItem());
                }
            });
            return fila;
        });

        cargarAlbumes(0);
        recargar();
        nueva();
    }

    private void cargarArtistas() {
        cmbArtista.getItems().setAll(App.getArtistas().listar());
        if (!cmbArtista.getItems().isEmpty()) {
            cmbArtista.getSelectionModel().selectFirst();
        }
    }

    private void cargarAlbumes(int idArtista) {
        List<Album> albumes = idArtista > 0
                ? App.getAlbumes().listarPorArtista(idArtista)
                : App.getAlbumes().listar();
        List<Album> items = new ArrayList<>();
        items.add(null);
        items.addAll(albumes);
        cmbAlbum.setItems(FXCollections.observableArrayList(items));
        cmbAlbum.getSelectionModel().selectFirst();
    }

    @FXML
    private void recargar() {
        Integer seleccionadoId = seleccionada != null ? seleccionada.getIdCancion() : null;
        List<Cancion> canciones = new ArrayList<>();
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().trim() : "";
        canciones.addAll(texto.isEmpty()
                ? App.getCanciones().listar()
                : App.getCanciones().buscarPorTitulo(texto));

        if (chkInactivas.isSelected()) {
            Ui.ejecutar("Canciones", () -> {
                for (Cancion inactiva : App.getCanciones().listarInactivos()) {
                    boolean repetida = false;
                    for (Cancion activa : canciones) {
                        if (activa.getIdCancion() == inactiva.getIdCancion()) {
                            repetida = true;
                            break;
                        }
                    }
                    if (!repetida) {
                        canciones.add(inactiva);
                    }
                }
            });
        }

        tblCanciones.getItems().setAll(canciones);
        if (seleccionadoId != null) {
            seleccionar(seleccionadoId);
        }
    }

    private void seleccionar(int id) {
        for (Cancion c : tblCanciones.getItems()) {
            if (c.getIdCancion() == id) {
                tblCanciones.getSelectionModel().select(c);
                return;
            }
        }
    }

    private void mostrar(Cancion cancion) {
        seleccionada = cancion;
        if (cancion == null) {
            return;
        }
        lblFormTitulo.setText("Cancion #" + cancion.getIdCancion());
        txtTitulo.setText(cancion.getTitulo());
        txtDuracion.setText(String.valueOf(cancion.getDuracionSegundos()));
        txtRuta.setText(cancion.getRutaArchivo());
        cmbGenero.getSelectionModel().select(cancion.getGenero());
        seleccionarArtista(cancion.getArtista());
        cargarAlbumes(cancion.getArtista() != null ? cancion.getArtista().getIdArtista() : 0);
        seleccionarAlbum(cancion.getAlbum());
        boolean activa = cancion.isActivo();
        btnBaja.setDisable(!activa);
        btnReactivar.setDisable(activa);
        actualizarFavorito();
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

    private void seleccionarAlbum(Album album) {
        if (album == null) {
            cmbAlbum.getSelectionModel().selectFirst();
            return;
        }
        for (int i = 0; i < cmbAlbum.getItems().size(); i++) {
            Album item = cmbAlbum.getItems().get(i);
            if (item != null && item.getIdAlbum() == album.getIdAlbum()) {
                cmbAlbum.getSelectionModel().select(i);
                return;
            }
        }
    }

    @FXML
    private void nueva() {
        seleccionada = null;
        lblFormTitulo.setText("Nueva cancion");
        txtTitulo.clear();
        txtDuracion.clear();
        txtRuta.clear();
        cmbGenero.getSelectionModel().selectFirst();
        if (!cmbArtista.getItems().isEmpty()) {
            cmbArtista.getSelectionModel().selectFirst();
        }
        cargarAlbumes(cmbArtista.getValue() != null ? cmbArtista.getValue().getIdArtista() : 0);
        btnBaja.setDisable(true);
        btnReactivar.setDisable(true);
        lblMensaje.setText("");
        tblCanciones.getSelectionModel().clearSelection();
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
        Genero genero = cmbGenero.getValue();
        int duracion = parsear(txtDuracion.getText(), -1);

        if (titulo.isEmpty()) {
            lblMensaje.setText("El titulo es obligatorio.");
            return;
        }
        if (artista == null) {
            lblMensaje.setText("Selecciona un artista.");
            return;
        }
        if (genero == null) {
            lblMensaje.setText("Selecciona un genero.");
            return;
        }
        if (duracion <= 0) {
            lblMensaje.setText("La duracion debe ser un numero mayor a cero.");
            return;
        }

        Album album = cmbAlbum.getValue();
        String ruta = txtRuta.getText() != null ? txtRuta.getText().trim() : "";

        boolean ok;
        if (seleccionada == null) {
            final Cancion nueva = new Cancion(titulo, duracion, ruta, genero, artista, album, true);
            ok = Ui.ejecutar("Guardar cancion", () -> App.getCanciones().alta(nueva));
            if (ok) {
                Reglas.registrarAccion("Alta de cancion '" + titulo + "'");
                seleccionada = nueva;
            }
        } else {
            seleccionada.setTitulo(titulo);
            seleccionada.setDuracionSegundos(duracion);
            seleccionada.setRutaArchivo(ruta);
            seleccionada.setGenero(genero);
            seleccionada.setArtista(artista);
            seleccionada.setAlbum(album);
            ok = Ui.ejecutar("Guardar cancion", () -> App.getCanciones().modificar(seleccionada));
            if (ok) {
                Reglas.registrarAccion("Modificacion de la cancion '" + titulo + "'");
            }
        }

        if (ok) {
            lblMensaje.setText("Cambios guardados.");
            recargar();
            if (seleccionada != null) {
                seleccionar(seleccionada.getIdCancion());
            }
        }
    }

    @FXML
    private void darDeBaja() {
        if (seleccionada == null || !Reglas.puedeEditarContenido()) {
            return;
        }
        if (!Ui.confirmar("Dar de baja", "Dar de baja la cancion '" + seleccionada.getTitulo() + "'?")) {
            return;
        }
        int id = seleccionada.getIdCancion();
        String titulo = seleccionada.getTitulo();
        if (Ui.ejecutar("Dar de baja", () -> App.getCanciones().baja(id))) {
            Reglas.registrarAccion("Baja de la cancion '" + titulo + "'");
            if (ReproductorAudioFX.getInstancia().getActual() instanceof Cancion
                    && ((Cancion) ReproductorAudioFX.getInstancia().getActual()).getIdCancion() == id) {
                ReproductorAudioFX.getInstancia().detener();
            }
            nueva();
            recargar();
        }
    }

    @FXML
    private void reactivar() {
        if (seleccionada == null || !Reglas.puedeEditarContenido()) {
            return;
        }
        int id = seleccionada.getIdCancion();
        if (Ui.ejecutar("Reactivar", () -> App.getCanciones().reactivar(id))) {
            Reglas.registrarAccion("Reactivacion de la cancion #" + id);
            recargar();
            seleccionar(id);
        }
    }

    @FXML
    private void elegirArchivo() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir archivo de audio");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Audio", "*.mp3", "*.wav", "*.m4a", "*.aac", "*.flac"));
        File archivo = selector.showOpenDialog(formulario.getScene().getWindow());
        if (archivo != null) {
            txtRuta.setText(archivo.getAbsolutePath());
        }
    }

    @FXML
    private void reproducir() {
        Cancion cancion = tblCanciones.getSelectionModel().getSelectedItem();
        if (cancion == null) {
            Ui.info("Canciones", "Selecciona una cancion.");
            return;
        }
        if (!cancion.isActivo()) {
            Ui.info("Canciones", "La cancion esta dada de baja.");
            return;
        }
        ReproductorAudioFX.getInstancia().reproducir(cancion);
    }

    @FXML
    private void alternarFavorito() {
        Usuario usuario = Reglas.usuario();
        if (!(usuario instanceof UsuarioNormal)) {
            return;
        }
        Cancion cancion = tblCanciones.getSelectionModel().getSelectedItem();
        if (cancion == null) {
            Ui.info("Favoritos", "Selecciona una cancion.");
            return;
        }
        UsuarioNormal normal = (UsuarioNormal) usuario;
        boolean esFavorita = normal.esFavorita(cancion.getIdCancion());
        boolean ok = esFavorita
                ? Ui.ejecutar("Favoritos", () -> App.getUsuarios().quitarFavorito(
                        normal.getIdUsuario(), cancion.getIdCancion()))
                : Ui.ejecutar("Favoritos", () -> App.getUsuarios().agregarFavorito(
                        normal.getIdUsuario(), cancion.getIdCancion()));
        if (ok) {
            actualizarFavorito();
        }
    }

    private void actualizarFavorito() {
        Usuario usuario = Reglas.usuario();
        if (!(usuario instanceof UsuarioNormal)) {
            return;
        }
        boolean esFavorita = seleccionada != null
                && ((UsuarioNormal) usuario).esFavorita(seleccionada.getIdCancion());
        btnFavorito.setText(esFavorita ? "Quitar favorito" : "Favorito");
    }

    @FXML
    private void agregarAPlaylist() {
        Cancion cancion = tblCanciones.getSelectionModel().getSelectedItem();
        AgregarAPlaylistDialog.mostrar(cancion);
    }

    private int parsear(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            Ui.error("Cancion", "La duracion debe ser un numero entero de segundos.");
            return porDefecto;
        }
    }
}
