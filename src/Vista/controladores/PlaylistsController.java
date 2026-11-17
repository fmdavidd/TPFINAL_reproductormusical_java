package Vista.controladores;

import Vista.App;

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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import modelo.Cancion;
import modelo.Playlist;
import modelo.Usuario;
import modelo.UsuarioNormal;

public class PlaylistsController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<Playlist> tblPlaylists;
    @FXML private TableColumn<Playlist, String> colNombre;
    @FXML private TableColumn<Playlist, String> colPropietario;
    @FXML private TableColumn<Playlist, String> colCanciones;
    @FXML private TableColumn<Playlist, String> colDuracion;
    @FXML private TableColumn<Playlist, String> colPublica;
    @FXML private Label lblPlaylist;
    @FXML private ListView<Cancion> lstCanciones;
    @FXML private Button btnSubir;
    @FXML private Button btnBajar;
    @FXML private Button btnQuitar;

    @FXML private VBox formulario;
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private CheckBox chkPublica;
    @FXML private ComboBox<Cancion> cmbCancion;
    @FXML private Button btnAgregar;
    @FXML private Button btnGuardar;
    @FXML private Button btnNueva;
    @FXML private Button btnEliminar;
    @FXML private Label lblMensaje;

    private Playlist seleccionada;
    private boolean puedeCrear;

    @FXML
    private void initialize() {
        tblPlaylists.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Ui.configurar(colNombre, Playlist::getNombre);
        Ui.configurar(colPropietario, p -> Reglas.nombreUsuario(p.getIdPropietario()));
        Ui.configurar(colCanciones, p -> String.valueOf(p.getCanciones().size()));
        Ui.configurar(colDuracion, p -> Ui.duracion(p.getDuracionTotal()));
        Ui.configurar(colPublica, p -> p.isPublica() ? "Si" : "No");

        cmbCancion.setConverter(Ui.conversor(Ui::textoCancion));
        lstCanciones.setCellFactory(lv -> Ui.celda(Ui::textoCancion));
        lstCanciones.setOnMouseClicked(evento -> {
            if (evento.getClickCount() == 2) {
                reproducir();
            }
        });

        puedeCrear = Reglas.esNormal();
        btnNueva.setVisible(puedeCrear);
        btnNueva.setManaged(puedeCrear);

        boolean editor = Reglas.puedeEditarContenido();
        formulario.setVisible(editor || puedeCrear);
        formulario.setManaged(editor || puedeCrear);

        tblPlaylists.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, nuevo) -> mostrar(nuevo));

        recargar();
        nueva();
    }

    @FXML
    private void recargar() {
        Integer seleccionadoId = seleccionada != null ? seleccionada.getIdPlaylist() : null;
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().trim().toLowerCase() : "";
        List<Playlist> playlists = new ArrayList<>();
        for (Playlist playlist : App.getPlaylists().listar()) {
            if (Reglas.puedeVerPlaylist(playlist)
                    && (texto.isEmpty() || playlist.getNombre().toLowerCase().contains(texto))) {
                playlists.add(playlist);
            }
        }
        tblPlaylists.getItems().setAll(playlists);
        if (seleccionadoId != null) {
            seleccionar(seleccionadoId);
        }
    }

    private void seleccionar(int id) {
        for (Playlist p : tblPlaylists.getItems()) {
            if (p.getIdPlaylist() == id) {
                tblPlaylists.getSelectionModel().select(p);
                return;
            }
        }
    }

    private void mostrar(Playlist playlist) {
        seleccionada = playlist;
        if (playlist == null) {
            lstCanciones.getItems().clear();
            lblPlaylist.setText("Canciones de la playlist");
            aplicarPermisos(false);
            return;
        }

        lblPlaylist.setText("Canciones de '" + playlist.getNombre() + "'");
        lblFormTitulo.setText("Playlist #" + playlist.getIdPlaylist());
        txtNombre.setText(playlist.getNombre());
        txtDescripcion.setText(playlist.getDescripcion());
        chkPublica.setSelected(playlist.isPublica());
        lstCanciones.getItems().setAll(playlist.getCanciones());
        actualizarComboCanciones(playlist);

        boolean puede = Reglas.puedeModificarPlaylist(playlist);
        aplicarPermisos(puede);
        btnEliminar.setDisable(!(Reglas.esPropietario(playlist) || Reglas.puedeEditarContenido()));
    }

    private void aplicarPermisos(boolean puede) {
        btnSubir.setDisable(!puede);
        btnBajar.setDisable(!puede);
        btnQuitar.setDisable(!puede);
        btnAgregar.setDisable(!puede);
        btnGuardar.setDisable(!puede);
        btnEliminar.setDisable(!puede);
        txtNombre.setDisable(!puede);
        txtDescripcion.setDisable(!puede);
        chkPublica.setDisable(!puede);
    }

    private void actualizarComboCanciones(Playlist playlist) {
        List<Cancion> disponibles = new ArrayList<>();
        for (Cancion cancion : App.getCanciones().listar()) {
            boolean yaEsta = false;
            for (Cancion dentro : playlist.getCanciones()) {
                if (dentro.getIdCancion() == cancion.getIdCancion()) {
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
    private void nueva() {
        seleccionada = null;
        lblFormTitulo.setText("Nueva playlist");
        txtNombre.clear();
        txtDescripcion.clear();
        chkPublica.setSelected(false);
        lstCanciones.getItems().clear();
        lblPlaylist.setText("Canciones de la playlist");
        cmbCancion.setItems(FXCollections.observableArrayList());
        aplicarPermisos(puedeCrear);
        btnEliminar.setDisable(true);
        lblMensaje.setText("");
        tblPlaylists.getSelectionModel().clearSelection();
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
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
        if (nombre.isEmpty()) {
            lblMensaje.setText("El nombre es obligatorio.");
            return;
        }

        boolean ok;
        if (seleccionada == null) {
            Usuario usuario = Reglas.usuario();
            if (!(usuario instanceof UsuarioNormal)) {
                lblMensaje.setText("Solo los usuarios normales pueden crear playlists.");
                return;
            }
            Playlist nueva = new Playlist(nombre, descripcion, chkPublica.isSelected(),
                    usuario.getIdUsuario(), true);
            ok = Ui.ejecutar("Guardar playlist", () -> App.getPlaylists().alta(nueva));
            if (ok) {
                Reglas.registrarAccion("Alta de la playlist '" + nombre + "'");
                seleccionada = nueva;
            }
        } else {
            if (!Reglas.puedeModificarPlaylist(seleccionada)) {
                return;
            }
            seleccionada.setNombre(nombre);
            seleccionada.setDescripcion(descripcion);
            seleccionada.setPublica(chkPublica.isSelected());
            ok = Ui.ejecutar("Guardar playlist", () -> App.getPlaylists().modificar(seleccionada));
            if (ok) {
                Reglas.registrarAccion("Modificacion de la playlist '" + nombre + "'");
            }
        }

        if (ok) {
            lblMensaje.setText("Cambios guardados.");
            recargar();
            if (seleccionada != null) {
                seleccionar(seleccionada.getIdPlaylist());
            }
        }
    }

    @FXML
    private void eliminar() {
        if (seleccionada == null
                || !(Reglas.esPropietario(seleccionada) || Reglas.puedeEditarContenido())) {
            return;
        }
        if (!Ui.confirmar("Eliminar playlist",
                "Eliminar la playlist '" + seleccionada.getNombre() + "'?")) {
            return;
        }
        int id = seleccionada.getIdPlaylist();
        String nombre = seleccionada.getNombre();
        if (Ui.ejecutar("Eliminar playlist", () -> App.getPlaylists().baja(id))) {
            Reglas.registrarAccion("Baja de la playlist '" + nombre + "'");
            nueva();
            recargar();
        }
    }

    @FXML
    private void agregarCancion() {
        if (seleccionada == null) {
            lblMensaje.setText("Selecciona o guarda una playlist primero.");
            return;
        }
        Cancion cancion = cmbCancion.getValue();
        if (cancion == null) {
            lblMensaje.setText("No hay canciones disponibles.");
            return;
        }
        if (Ui.ejecutar("Agregar cancion",
                () -> App.getPlaylists().agregarCancion(seleccionada.getIdPlaylist(), cancion.getIdCancion()))) {
            Reglas.registrarAccion("Cancion '" + cancion.getTitulo()
                    + "' agregada a la playlist '" + seleccionada.getNombre() + "'");
            mostrar(seleccionada);
        }
    }

    @FXML
    private void quitarCancion() {
        Cancion cancion = lstCanciones.getSelectionModel().getSelectedItem();
        if (seleccionada == null || cancion == null) {
            lblMensaje.setText("Selecciona una cancion de la playlist.");
            return;
        }
        if (Ui.ejecutar("Quitar cancion",
                () -> App.getPlaylists().quitarCancion(seleccionada.getIdPlaylist(), cancion.getIdCancion()))) {
            Reglas.registrarAccion("Cancion '" + cancion.getTitulo()
                    + "' quitada de la playlist '" + seleccionada.getNombre() + "'");
            mostrar(seleccionada);
        }
    }

    @FXML
    private void subir() {
        mover(-1);
    }

    @FXML
    private void bajar() {
        mover(1);
    }

    private void mover(int delta) {
        if (seleccionada == null) {
            return;
        }
        int indice = lstCanciones.getSelectionModel().getSelectedIndex();
        int destino = indice + delta;
        if (indice < 0 || destino < 0 || destino >= seleccionada.getCanciones().size()) {
            return;
        }
        if (Ui.ejecutar("Reordenar playlist",
                () -> App.getPlaylists().moverCancion(seleccionada.getIdPlaylist(), indice, destino))) {
            mostrar(seleccionada);
            lstCanciones.getSelectionModel().select(destino);
        }
    }

    @FXML
    private void reproducir() {
        if (seleccionada == null || seleccionada.getCanciones().isEmpty()) {
            Ui.info("Playlists", "Selecciona una playlist con canciones.");
            return;
        }
        int indice = Math.max(0, lstCanciones.getSelectionModel().getSelectedIndex());
        ReproductorAudioFX.getInstancia().reproducirLista(seleccionada.getCanciones(), indice);
    }
}
