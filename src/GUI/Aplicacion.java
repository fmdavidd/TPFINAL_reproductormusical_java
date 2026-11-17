package GUI;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Aplicacion extends Application {
    private Stage ventana;
    private final Datos datos = new Datos();
    @Override public void start(Stage stage) {
        ventana = stage;
        ventana.setTitle("Biblioteca musical");
        ventana.setMinWidth(850); ventana.setMinHeight(600);
        mostrarLogin(); ventana.show();
    }
    public void mostrarLogin() { mostrar(new LoginView(datos, this::mostrarPrincipal)); }
    private void mostrarPrincipal() { mostrar(new MainView(datos, () -> {
        datos.sesion.cerrarSesion(); mostrarLogin();
    })); }
    private void mostrar(javafx.scene.Parent contenido) {
        if (ventana.getScene() == null) {
            Scene scene = new Scene(contenido, 1180, 760);
            ventana.setScene(scene);
        } else ventana.getScene().setRoot(contenido);
    }
}
