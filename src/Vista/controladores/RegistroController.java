package Vista.controladores;

import Vista.App;

import Excepciones.ConexionBDException;
import Excepciones.CredencialesInvalidasException;
import Excepciones.ElementoDuplicadoException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegistroController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtContrasenia;
    @FXML private PasswordField txtRepetir;
    @FXML private Label lblError;

    @FXML
    private void registrarse() {
        String nombre = txtNombre.getText().trim();
        String email = txtEmail.getText().trim();
        String contrasenia = txtContrasenia.getText();
        String repetir = txtRepetir.getText();

        if (nombre.isEmpty() || email.isEmpty() || contrasenia.isEmpty()) {
            lblError.setText("Completa todos los campos.");
            return;
        }
        if (!contrasenia.equals(repetir)) {
            lblError.setText("Las contrasenias no coinciden.");
            return;
        }

        try {
            App.getSesion().registrar(nombre, email, contrasenia);
            App.getSesion().iniciarSesion(email, contrasenia);
            lblError.setText("");
            Navegacion.ir("MenuPrincipalView", "Reproductor Musical");
        } catch (ConexionBDException | ElementoDuplicadoException
                | CredencialesInvalidasException e) {
            lblError.setText(Ui.mensaje(e));
            Ui.error("Registro", e);
            txtContrasenia.clear();
            txtRepetir.clear();
        }
    }

    @FXML
    private void volver() {
        Navegacion.ir("LoginView", "Reproductor Musical");
    }
}
