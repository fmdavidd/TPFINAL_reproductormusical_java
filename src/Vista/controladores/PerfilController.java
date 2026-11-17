package Vista.controladores;

import Vista.App;

import Seguridad.Seguridad;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import modelo.Usuario;
import modelo.UsuarioNormal;

public class PerfilController {

    @FXML private Label lblInfo;
    @FXML private Label lblResumen;
    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private CheckBox chkPremium;
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

        chkPremium.setVisible(usuario instanceof UsuarioNormal);
        chkPremium.setManaged(usuario instanceof UsuarioNormal);
        if (usuario instanceof UsuarioNormal) {
            chkPremium.setSelected(((UsuarioNormal) usuario).esPremium());
            lblResumen.setText("Favoritos: " + ((UsuarioNormal) usuario).getFavoritos().size()
                    + "  |  Playlists: "
                    + App.getPlaylists().listarPorUsuario(usuario.getIdUsuario()).size());
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
        if (usuario instanceof UsuarioNormal) {
            ((UsuarioNormal) usuario).setPremium(chkPremium.isSelected());
        }
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
        if (!Ui.confirmar("Dar de baja mi cuenta",
                "Estas seguro? Se cerrara la sesion y no podras volver a ingresar.")) {
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
