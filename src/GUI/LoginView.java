package GUI;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoginView extends StackPane {
    private final Datos datos;
    private final Label estado = new Label("Ingresá con tu cuenta para acceder a tu biblioteca.");
    public LoginView(Datos datos, Runnable entrar) {
        this.datos = datos;
        Label titulo = new Label("Biblioteca musical"); titulo.getStyleClass().add("titulo");
        TextField email = new TextField(); email.setPromptText("Correo electrónico");
        PasswordField clave = new PasswordField(); clave.setPromptText("Contraseña");
        Button login = UI.boton("Iniciar sesión", () -> {
            try {
                String correo = UI.email(email); String password = UI.requerido(clave, "Contraseña", 255);
                UI.ejecutar(this, estado, () -> {
                    datos.conectar(); datos.sesion.iniciarSesion(correo, password);
                    try { datos.cargarCatalogo(); }
                    catch (Excepciones.ConexionBDException e) { datos.sesion.cerrarSesion(); throw e; }
                }, entrar);
            } catch (IllegalArgumentException e) { estado.setText(e.getMessage()); }
        });
        login.setDefaultButton(true); login.getStyleClass().add("primario"); login.setMaxWidth(Double.MAX_VALUE);
        Button registro = UI.boton("Crear una cuenta", this::registrar);
        estado.setWrapText(true);
        VBox tarjeta = new VBox(16, titulo, new Label("Tu música, organizada en un solo lugar"), new Label("Correo electrónico"), email,
                new Label("Contraseña"), clave, login, registro, estado);
        tarjeta.setMaxWidth(420); tarjeta.setMaxHeight(USE_PREF_SIZE); tarjeta.getStyleClass().add("tarjeta");
        setAlignment(Pos.CENTER); getChildren().add(tarjeta);
    }
    private void registrar() {
        Formulario f = new Formulario("Crear cuenta", getScene().getWindow());
        TextField nombre = f.texto("Nombre", ""), email = f.texto("Correo electrónico", "");
        PasswordField clave = f.campo("Contraseña", new PasswordField());
        PasswordField repetir = f.campo("Repetir contraseña", new PasswordField());
        f.guardar(() -> {
            String n = UI.requerido(nombre, "Nombre", 60), correo = UI.email(email);
            String password = UI.requerido(clave, "Contraseña", 255);
            if (!clave.getText().equals(repetir.getText())) throw new IllegalArgumentException("Las contraseñas no coinciden.");
            return () -> { datos.conectar(); datos.sesion.registrar(n, correo, password); };
        }, () -> estado.setText("Cuenta creada. Ya podés iniciar sesión."));
        f.showAndWait();
    }
}
