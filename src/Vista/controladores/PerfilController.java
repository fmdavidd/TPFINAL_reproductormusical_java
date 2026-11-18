package Vista.controladores;

import Vista.App;

import Seguridad.Seguridad;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import modelo.Usuario;
import modelo.UsuarioNormal;

public class PerfilController {

    @FXML private Label lblInfo;
    @FXML private Label lblResumen;
    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtActual;
    @FXML private PasswordField txtNueva;
    @FXML private PasswordField txtRepetir;
    @FXML private Label lblMensaje;
    @FXML private Label lblId;
    @FXML private Label lblRol;
    @FXML private Label lblFecha;

    @FXML
    private void initialize() {
        Usuario usuario = Reglas.usuario();
        if (usuario == null) {
            return;
        }

        lblInfo.setText("Sesion iniciada como " + usuario.getEmail());
        lblId.setText("ID: " + usuario.getIdUsuario());
        lblRol.setText("Rol: " + Reglas.descripcionRol());
        lblFecha.setText("Alta: " + usuario.getFechaRegistro());

        txtNombre.setText(usuario.getNombre());
        txtEmail.setText(usuario.getEmail());

        if (usuario instanceof UsuarioNormal) {
            UsuarioNormal normal = (UsuarioNormal) usuario;
            int playlists = App.getPlaylists().listarPorUsuario(usuario.getIdUsuario()).size();
            String cupo = normal.esPremium()
                    ? playlists + " (ilimitadas)"
                    : playlists + "/" + Reglas.LIMITE_PLAYLISTS_SIN_PREMIUM;
            lblResumen.setText("Favoritos: " + normal.getFavoritos().size()
                    + "  |  Playlists: " + cupo);
        } else {
            lblResumen.setText("Cuenta administrativa");
        }
    }

    @FXML
    private void guardar() {
        Usuario usuario = Reglas.usuario();
        if (usuario == null) {
            return;
        }

        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        if (nombre.isEmpty() || email.isEmpty()) {
            lblMensaje.setText("Nombre y email son obligatorios.");
            return;
        }

        String actual = txtActual.getText() != null ? txtActual.getText() : "";
        String nueva = txtNueva.getText() != null ? txtNueva.getText() : "";
        String repetir = txtRepetir.getText() != null ? txtRepetir.getText() : "";

        boolean cambiaContrasenia = !nueva.isEmpty() || !repetir.isEmpty();
        if (cambiaContrasenia) {
            if (!usuario.verificarContrasenia(actual)) {
                lblMensaje.setText("La contrasenia actual no es correcta.");
                return;
            }
            if (nueva.isEmpty() || !nueva.equals(repetir)) {
                lblMensaje.setText("Las contrasenias nuevas no coinciden.");
                return;
            }
        }

        usuario.setNombre(nombre);
        usuario.setEmail(email);
        if (cambiaContrasenia) {
            String salt = Seguridad.generarSalt();
            usuario.setSalt(salt);
            usuario.setHashContrasenia(Seguridad.hashear(nueva, salt));
        }

        if (Ui.ejecutar("Mi perfil", () -> App.getUsuarios().modificar(usuario))) {
            App.getSesion().refrescar(usuario);
            txtActual.clear();
            txtNueva.clear();
            txtRepetir.clear();
            lblMensaje.setText("Datos actualizados.");
            lblInfo.setText("Sesion iniciada como " + usuario.getEmail());
        }
    }

    @FXML
    private void darDeBaja() {
        Usuario usuario = Reglas.usuario();
        if (usuario == null) {
            return;
        }
        Dialog<ButtonType> confirmacion = new Dialog<>();
        confirmacion.setTitle("Dar de baja mi cuenta");
        confirmacion.setHeaderText("Se cerrara la sesion y no podras volver a ingresar.");
        PasswordField contrasenia = new PasswordField();
        contrasenia.setPromptText("Contrasenia actual");
        Label error = new Label();
        error.setWrapText(true);
        confirmacion.getDialogPane().setContent(new VBox(8,
                new Label("Ingresa tu contrasenia para confirmar la baja."), contrasenia, error));
        ButtonType darDeBaja = new ButtonType("Dar de baja", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        confirmacion.getDialogPane().getButtonTypes().setAll(darDeBaja, ButtonType.CANCEL);
        var botonConfirmar = confirmacion.getDialogPane().lookupButton(darDeBaja);
        botonConfirmar.disableProperty().bind(contrasenia.textProperty().isEmpty());
        botonConfirmar.addEventFilter(ActionEvent.ACTION, evento -> {
            if (!usuario.verificarContrasenia(contrasenia.getText())) {
                evento.consume();
                error.setText("La contrasenia no es correcta.");
                contrasenia.clear();
                contrasenia.requestFocus();
            }
        });
        confirmacion.setOnShown(evento -> contrasenia.requestFocus());
        boolean confirmada = confirmacion.showAndWait().filter(darDeBaja::equals).isPresent();
        contrasenia.clear();
        if (!confirmada) {
            return;
        }
        int id = usuario.getIdUsuario();
        if (Ui.ejecutar("Baja de cuenta", () -> App.getUsuarios().baja(id))) {
            ReproductorAudioFX.getInstancia().dispose();
            App.getSesion().cerrarSesion();
            Navegacion.ir("LoginView", "Reproductor Musical");
        }
    }
}
