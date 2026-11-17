package Vista.controladores;

import Vista.App;

import Excepciones.ElementoNoEncontradoException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import modelo.UsuarioAdministrador;

public class RegistroAccionesController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private Label lblResumen;
    @FXML private CheckBox chkFiltrar;
    @FXML private DatePicker dpFecha;
    @FXML private TableView<Map.Entry<LocalDateTime, String>> tblAcciones;
    @FXML private TableColumn<Map.Entry<LocalDateTime, String>, String> colFecha;
    @FXML private TableColumn<Map.Entry<LocalDateTime, String>, String> colHora;
    @FXML private TableColumn<Map.Entry<LocalDateTime, String>, String> colAccion;

    @FXML
    private void initialize() {
        Ui.configurar(colFecha, entrada -> entrada.getKey().format(FORMATO_FECHA));
        Ui.configurar(colHora, entrada -> entrada.getKey().format(FORMATO_HORA));
        Ui.configurar(colAccion, Map.Entry::getValue);

        dpFecha.setValue(LocalDate.now());
        chkFiltrar.setSelected(false);
        chkFiltrar.selectedProperty().addListener((obs, anterior, nuevo) -> recargar());
        dpFecha.valueProperty().addListener((obs, anterior, nuevo) -> recargar());
        recargar();
    }

    @FXML
    private void recargar() {
        UsuarioAdministrador admin = adminActual();
        if (admin == null) {
            lblResumen.setText("Solo un administrador puede ver su registro de acciones.");
            tblAcciones.getItems().clear();
            return;
        }

        List<Map.Entry<LocalDateTime, String>> entradas =
                new ArrayList<>(admin.getRegistroAcciones().entrySet());
        entradas.sort((a, b) -> b.getKey().compareTo(a.getKey()));

        if (chkFiltrar.isSelected()) {
            LocalDate fecha = dpFecha.getValue() != null ? dpFecha.getValue() : LocalDate.now();
            List<Map.Entry<LocalDateTime, String>> filtradas = new ArrayList<>();
            for (Map.Entry<LocalDateTime, String> entrada : entradas) {
                if (entrada.getKey().toLocalDate().equals(fecha)) {
                    filtradas.add(entrada);
                }
            }
            entradas = filtradas;
            lblResumen.setText(entradas.size() + " acciones el " + fecha.format(FORMATO_FECHA)
                    + " - admin " + admin.getNombre());
        } else {
            lblResumen.setText(entradas.size() + " acciones registradas - admin " + admin.getNombre());
        }

        tblAcciones.getItems().setAll(entradas);
    }

    private UsuarioAdministrador adminActual() {
        if (!(Reglas.usuario() instanceof UsuarioAdministrador)) {
            return null;
        }
        int id = Reglas.usuario().getIdUsuario();
        try {
            return (UsuarioAdministrador) App.getUsuarios().buscarPorId(id);
        } catch (ElementoNoEncontradoException e) {
            Ui.error("Registro de acciones", e);
            return null;
        }
    }

    @FXML
    private void hoy() {
        dpFecha.setValue(LocalDate.now());
        chkFiltrar.setSelected(true);
        recargar();
    }

    @FXML
    private void verTodas() {
        chkFiltrar.setSelected(false);
        recargar();
    }
}
