package Vista.controladores;

import Vista.App;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import Enumeraciones.Genero;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import modelo.Album;
import modelo.Artista;
import modelo.Cancion;

public class ArtistasController {

    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkInactivos;
    @FXML private TableView<Artista> tblArtistas;
    @FXML private TableColumn<Artista, String> colNombre;
    @FXML private TableColumn<Artista, String> colPais;
    @FXML private TableColumn<Artista, String> colGeneros;
    @FXML private TableColumn<Artista, String> colEstado;
    @FXML private ListView<Album> lstAlbumes;

    @FXML private VBox formulario;
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPais;
    @FXML private ListView<Genero> lstGeneros;
    @FXML private Button btnBaja;
    @FXML private Button btnReactivar;
    @FXML private Label lblMensaje;

    private Artista seleccionado;

    @FXML
    private void initialize() {
        tblArtistas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Ui.configurar(colNombre, Artista::getNombre);
        Ui.configurar(colPais, a -> a.getPaisOrigen() != null ? a.getPaisOrigen() : "");
        Ui.configurar(colGeneros, a -> textoGeneros(a.getGeneros()));
        Ui.configurar(colEstado, a -> a.isActivo() ? "Activo" : "Inactivo");

        lstGeneros.getItems().setAll(Genero.values());
        lstGeneros.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lstAlbumes.setCellFactory(lv -> Ui.celda(a ->
                a.getTitulo() + " (" + a.getAnioLanzamiento() + ")"));

        boolean editor = Reglas.puedeEditarContenido();
        formulario.setVisible(editor);
        formulario.setManaged(editor);
        chkInactivos.setVisible(editor);
        chkInactivos.setManaged(editor);

        tblArtistas.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, nuevo) -> mostrar(nuevo));
        tblArtistas.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Artista> fila = new javafx.scene.control.TableRow<>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && fila.getItem() != null) {
                    reproducirArtista(fila.getItem());
                }
            });
            return fila;
        });

        recargar();
        nuevo();
    }

    private String textoGeneros(java.util.Set<Genero> generos) {
        if (generos == null || generos.isEmpty()) {
            return "Sin generos";
        }
        List<String> nombres = new ArrayList<>();
        for (Genero genero : generos) {
            nombres.add(genero.name());
        }
        return String.join(", ", nombres);
    }

    @FXML
    private void recargar() {
        Integer seleccionadoId = seleccionado != null ? seleccionado.getIdArtista() : null;
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().trim() : "";
        List<Artista> artistas = new ArrayList<>(texto.isEmpty()
                ? App.getArtistas().listar()
                : App.getArtistas().buscarPorNombre(texto));

        if (chkInactivos.isSelected()) {
            Ui.ejecutar("Artistas", () -> {
                for (Artista inactivo : App.getArtistas().listarInactivos()) {
                    boolean repetido = false;
                    for (Artista activo : artistas) {
                        if (activo.getIdArtista() == inactivo.getIdArtista()) {
                            repetido = true;
                            break;
                        }
                    }
                    if (!repetido) {
                        artistas.add(inactivo);
                    }
                }
            });
        }

        tblArtistas.getItems().setAll(artistas);
        if (seleccionadoId != null) {
            seleccionar(seleccionadoId);
        }
    }

    private void seleccionar(int id) {
        for (Artista a : tblArtistas.getItems()) {
            if (a.getIdArtista() == id) {
                tblArtistas.getSelectionModel().select(a);
                return;
            }
        }
    }

    private void mostrar(Artista artista) {
        seleccionado = artista;
        if (artista == null) {
            lstAlbumes.getItems().clear();
            return;
        }
        lblFormTitulo.setText("Artista #" + artista.getIdArtista());
        txtNombre.setText(artista.getNombre());
        txtPais.setText(artista.getPaisOrigen());

        lstGeneros.getSelectionModel().clearSelection();
        for (int i = 0; i < lstGeneros.getItems().size(); i++) {
            if (artista.getGeneros().contains(lstGeneros.getItems().get(i))) {
                lstGeneros.getSelectionModel().select(i);
            }
        }

        lstAlbumes.getItems().setAll(App.getAlbumes().listarPorArtista(artista.getIdArtista()));
        boolean activo = artista.isActivo();
        btnBaja.setDisable(!activo);
        btnReactivar.setDisable(activo);
    }

    @FXML
    private void nuevo() {
        seleccionado = null;
        lblFormTitulo.setText("Nuevo artista");
        txtNombre.clear();
        txtPais.clear();
        lstGeneros.getSelectionModel().clearSelection();
        lstAlbumes.getItems().clear();
        btnBaja.setDisable(true);
        btnReactivar.setDisable(true);
        lblMensaje.setText("");
        tblArtistas.getSelectionModel().clearSelection();
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
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String pais = txtPais.getText() != null ? txtPais.getText().trim() : "";
        if (nombre.isEmpty()) {
            lblMensaje.setText("El nombre es obligatorio.");
            return;
        }

        HashSet<Genero> generos = new HashSet<>(lstGeneros.getSelectionModel().getSelectedItems());

        boolean ok;
        if (seleccionado == null) {
            Artista nuevo = new Artista(nombre, pais, true);
            nuevo.setGeneros(generos);
            ok = Ui.ejecutar("Guardar artista", () -> App.getArtistas().alta(nuevo));
            if (ok) {
                Reglas.registrarAccion("Alta del artista '" + nombre + "'");
                seleccionado = nuevo;
            }
        } else {
            seleccionado.setNombre(nombre);
            seleccionado.setPaisOrigen(pais);
            seleccionado.setGeneros(generos);
            ok = Ui.ejecutar("Guardar artista", () -> App.getArtistas().modificar(seleccionado));
            if (ok) {
                Reglas.registrarAccion("Modificacion del artista '" + nombre + "'");
            }
        }

        if (ok) {
            lblMensaje.setText("Cambios guardados.");
            recargar();
            if (seleccionado != null) {
                seleccionar(seleccionado.getIdArtista());
            }
        }
    }

    @FXML
    private void darDeBaja() {
        if (seleccionado == null || !Reglas.puedeEditarContenido()) {
            return;
        }
        if (!Ui.confirmar("Dar de baja",
                "Dar de baja el artista '" + seleccionado.getNombre()
                        + "'? Sus canciones seguiran existiendo.")) {
            return;
        }
        int id = seleccionado.getIdArtista();
        String nombre = seleccionado.getNombre();
        if (Ui.ejecutar("Dar de baja", () -> App.getArtistas().baja(id))) {
            Reglas.registrarAccion("Baja del artista '" + nombre + "'");
            nuevo();
            recargar();
        }
    }

    @FXML
    private void reactivar() {
        if (seleccionado == null || !Reglas.puedeEditarContenido()) {
            return;
        }
        int id = seleccionado.getIdArtista();
        if (Ui.ejecutar("Reactivar", () -> App.getArtistas().reactivar(id))) {
            Reglas.registrarAccion("Reactivacion del artista #" + id);
            recargar();
            seleccionar(id);
        }
    }

    @FXML
    private void reproducir() {
        Artista artista = tblArtistas.getSelectionModel().getSelectedItem();
        if (artista != null) {
            reproducirArtista(artista);
        }
    }

    private void reproducirArtista(Artista artista) {
        List<Cancion> canciones = new ArrayList<>();
        for (Cancion cancion : App.getCanciones().listar()) {
            if (cancion.getArtista() != null
                    && cancion.getArtista().getIdArtista() == artista.getIdArtista()) {
                canciones.add(cancion);
            }
        }
        if (canciones.isEmpty()) {
            Ui.info("Artistas", "El artista no tiene canciones activas.");
            return;
        }
        ReproductorAudioFX.getInstancia().reproducirLista(canciones, 0);
    }
}
