package Vista.controladores;

import Vista.App;

import java.util.ArrayList;
import java.util.List;

import Enumeraciones.NivelAdmin;
import Seguridad.Seguridad;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import modelo.Usuario;
import modelo.UsuarioAdministrador;
import modelo.UsuarioNormal;

public class UsuariosController {

    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkInactivos;
    @FXML private TableView<Usuario> tblUsuarios;
    @FXML private TableColumn<Usuario, String> colId;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colEmail;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colFecha;
    @FXML private TableColumn<Usuario, String> colEstado;

    @FXML private VBox formulario;
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private ComboBox<NivelAdmin> cmbNivel;
    @FXML private CheckBox chkPremium;
    @FXML private Label lblPassInfo;
    @FXML private PasswordField txtPass;
    @FXML private Button btnGuardar;
    @FXML private Button btnBaja;
    @FXML private Button btnReactivar;
    @FXML private Label lblMensaje;
    @FXML private Label lblInfo;

    private Usuario seleccionado;

    @FXML
    private void initialize() {
        tblUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        if (!Reglas.puedeGestionarUsuarios()) {
            lblInfo.setText("Solo el SUPERADMIN puede gestionar usuarios.");
            formulario.setDisable(true);
            chkInactivos.setDisable(true);
            return;
        }

        Ui.configurar(colId, u -> String.valueOf(u.getIdUsuario()));
        Ui.configurar(colNombre, Usuario::getNombre);
        Ui.configurar(colEmail, Usuario::getEmail);
        Ui.configurar(colRol, u -> u instanceof UsuarioAdministrador
                ? u.getRol().name() + " / " + ((UsuarioAdministrador) u).getNivelAdmin().name()
                : u.getRol().name());
        Ui.configurar(colFecha, u -> u.getFechaRegistro() != null
                ? u.getFechaRegistro().toString() : "");
        Ui.configurar(colEstado, u -> u.isActivo() ? "Activo" : "Inactivo");

        cmbTipo.getItems().setAll("Usuario normal", "Administrador");
        cmbTipo.valueProperty().addListener((obs, anterior, nuevo) -> aplicarTipo());

        cmbNivel.getItems().setAll(NivelAdmin.values());
        cmbNivel.setConverter(Ui.conversor(NivelAdmin::name));
        cmbNivel.getSelectionModel().select(NivelAdmin.EDITOR);

        tblUsuarios.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, nuevo) -> mostrar(nuevo));

        recargar();
        nuevo();
    }

    private void aplicarTipo() {
        boolean admin = "Administrador".equals(cmbTipo.getValue());
        cmbNivel.setDisable(!admin || esSeleccionadoMismoUsuario());
        chkPremium.setDisable(admin);
        if (!admin) {
            cmbNivel.getSelectionModel().clearSelection();
        } else if (cmbNivel.getValue() == null) {
            cmbNivel.getSelectionModel().select(NivelAdmin.EDITOR);
        }
    }

    private boolean esSeleccionadoMismoUsuario() {
        return seleccionado != null && Reglas.usuario() != null
                && seleccionado.getIdUsuario() == Reglas.usuario().getIdUsuario();
    }

    @FXML
    private void recargar() {
        if (!Reglas.puedeGestionarUsuarios()) {
            return;
        }
        Integer seleccionadoId = seleccionado != null ? seleccionado.getIdUsuario() : null;
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().trim().toLowerCase() : "";
        List<Usuario> usuarios = new ArrayList<>();
        for (Usuario u : App.getUsuarios().listar()) {
            if (coincide(u, texto)) {
                usuarios.add(u);
            }
        }

        if (chkInactivos.isSelected()) {
            Ui.ejecutar("Usuarios", () -> {
                for (Usuario inactivo : App.getUsuarios().listarInactivos()) {
                    boolean repetido = false;
                    for (Usuario activo : usuarios) {
                        if (activo.getIdUsuario() == inactivo.getIdUsuario()) {
                            repetido = true;
                            break;
                        }
                    }
                    if (!repetido && coincide(inactivo, texto)) {
                        usuarios.add(inactivo);
                    }
                }
            });
        }

        tblUsuarios.getItems().setAll(usuarios);
        if (seleccionadoId != null) {
            seleccionar(seleccionadoId);
        }
    }

    private boolean coincide(Usuario u, String texto) {
        return texto.isEmpty()
                || u.getNombre().toLowerCase().contains(texto)
                || u.getEmail().toLowerCase().contains(texto);
    }

    private void seleccionar(int id) {
        for (Usuario u : tblUsuarios.getItems()) {
            if (u.getIdUsuario() == id) {
                tblUsuarios.getSelectionModel().select(u);
                return;
            }
        }
    }

    private void mostrar(Usuario usuario) {
        seleccionado = usuario;
        if (usuario == null) {
            return;
        }
        lblFormTitulo.setText("Usuario #" + usuario.getIdUsuario());
        txtNombre.setText(usuario.getNombre());
        txtEmail.setText(usuario.getEmail());
        txtPass.clear();
        lblPassInfo.setText("Nueva contrasenia (opcional)");

        boolean admin = usuario instanceof UsuarioAdministrador;
        cmbTipo.setValue(admin ? "Administrador" : "Usuario normal");
        cmbTipo.setDisable(true);
        cmbNivel.setDisable(!admin || esSeleccionadoMismoUsuario());
        chkPremium.setDisable(admin);
        chkPremium.setSelected(usuario instanceof UsuarioNormal
                && ((UsuarioNormal) usuario).esPremium());
        if (admin) {
            cmbNivel.setValue(((UsuarioAdministrador) usuario).getNivelAdmin());
        } else {
            cmbNivel.getSelectionModel().clearSelection();
        }

        boolean activo = usuario.isActivo();
        btnBaja.setDisable(!activo);
        btnReactivar.setDisable(activo);
        btnGuardar.setDisable(!activo);
        lblInfo.setText(activo ? "" : "Usuario dado de baja: reactivalo para poder editarlo.");
    }

    @FXML
    private void nuevo() {
        seleccionado = null;
        lblFormTitulo.setText("Nuevo usuario");
        txtNombre.clear();
        txtEmail.clear();
        txtPass.clear();
        lblPassInfo.setText("Contrasenia");
        cmbTipo.setDisable(false);
        cmbTipo.setValue("Usuario normal");
        cmbNivel.getSelectionModel().clearSelection();
        chkPremium.setSelected(false);
        chkPremium.setDisable(false);
        btnGuardar.setDisable(false);
        btnBaja.setDisable(true);
        btnReactivar.setDisable(true);
        lblMensaje.setText("");
        lblInfo.setText("");
        tblUsuarios.getSelectionModel().clearSelection();
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
        if (!Reglas.puedeGestionarUsuarios()) {
            return;
        }
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String pass = txtPass.getText() != null ? txtPass.getText() : "";

        if (nombre.isEmpty() || email.isEmpty()) {
            lblMensaje.setText("Nombre y email son obligatorios.");
            return;
        }

        boolean admin = "Administrador".equals(cmbTipo.getValue());

        if (seleccionado == null) {
            if (pass.isEmpty()) {
                lblMensaje.setText("La contrasenia es obligatoria para un usuario nuevo.");
                return;
            }
            String salt = Seguridad.generarSalt();
            String hash = Seguridad.hashear(pass, salt);

            Usuario nuevo;
            if (admin) {
                NivelAdmin nivel = cmbNivel.getValue() != null ? cmbNivel.getValue() : NivelAdmin.EDITOR;
                nuevo = new UsuarioAdministrador(nombre, email, hash, salt, nivel);
            } else {
                nuevo = new UsuarioNormal(nombre, email, hash, salt, chkPremium.isSelected());
            }

            if (Ui.ejecutar("Crear usuario", () -> App.getUsuarios().alta(nuevo))) {
                Reglas.registrarAccion("Alta del usuario '" + nombre + "' (" + email + ")");
                seleccionado = nuevo;
                lblMensaje.setText("Usuario creado.");
                recargar();
                seleccionar(nuevo.getIdUsuario());
            }
        } else {
            seleccionado.setNombre(nombre);
            seleccionado.setEmail(email);
            if (!pass.isEmpty()) {
                String salt = Seguridad.generarSalt();
                seleccionado.setSalt(salt);
                seleccionado.setHashContrasenia(Seguridad.hashear(pass, salt));
            }
            if (seleccionado instanceof UsuarioNormal) {
                ((UsuarioNormal) seleccionado).setPremium(chkPremium.isSelected());
            } else if (!esSeleccionadoMismoUsuario() && cmbNivel.getValue() != null) {
                ((UsuarioAdministrador) seleccionado).setNivelAdmin(cmbNivel.getValue());
            }

            if (Ui.ejecutar("Guardar usuario", () -> App.getUsuarios().modificar(seleccionado))) {
                Reglas.registrarAccion("Modificacion del usuario '" + nombre + "'");
                if (esSeleccionadoMismoUsuario()) {
                    App.getSesion().refrescar(seleccionado);
                }
                txtPass.clear();
                lblMensaje.setText("Cambios guardados.");
                recargar();
                seleccionar(seleccionado.getIdUsuario());
            }
        }
    }

    @FXML
    private void darDeBaja() {
        if (seleccionado == null || !Reglas.puedeGestionarUsuarios()) {
            return;
        }
        if (esSeleccionadoMismoUsuario()) {
            Ui.info("Usuarios", "No podes dar de baja tu propia cuenta desde aca.");
            return;
        }
        if (!Ui.confirmar("Dar de baja", "Dar de baja el usuario '" + seleccionado.getNombre() + "'?")) {
            return;
        }
        int id = seleccionado.getIdUsuario();
        String nombre = seleccionado.getNombre();
        if (Ui.ejecutar("Dar de baja", () -> App.getUsuarios().baja(id))) {
            Reglas.registrarAccion("Baja del usuario '" + nombre + "'");
            nuevo();
            recargar();
        }
    }

    @FXML
    private void reactivar() {
        if (seleccionado == null || !Reglas.puedeGestionarUsuarios()) {
            return;
        }
        int id = seleccionado.getIdUsuario();
        if (Ui.ejecutar("Reactivar", () -> App.getUsuarios().reactivar(id))) {
            Reglas.registrarAccion("Reactivacion del usuario #" + id);
            recargar();
            seleccionar(id);
        }
    }
}
