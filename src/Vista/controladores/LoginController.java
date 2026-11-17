package Vista.controladores;

import Vista.App;

import Excepciones.CredencialesInvalidasException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtContrasenia;
    @FXML private Label lblError;

    @FXML
    private void iniciarSesion() {
        String email = txtEmail.getText().trim();
        String contrasenia = txtContrasenia.getText();

        if (email.isEmpty() || contrasenia.isEmpty()) {
            lblError.setText("Completa email y contrasenia");
            return;
        }

        try {
            Vista.App.getSesion().iniciarSesion(email, contrasenia);
            lblError.setText("");
            Navegacion.ir("MenuPrincipalView", "Reproductor Musical");
        } catch (CredencialesInvalidasException e) {
            lblError.setText(e.getMessage());
            Ui.error("Iniciar sesion", e);
            txtContrasenia.clear();
        }
    }

    @FXML
    private void irARegistro() {
        Navegacion.ir("RegistroView", "Crear cuenta");
    }
}
