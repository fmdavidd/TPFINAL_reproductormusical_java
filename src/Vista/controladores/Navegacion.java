package Vista.controladores;

import Vista.App;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

public final class Navegacion {

    private static Pane contenedorActual;

    private Navegacion() {
    }

    public static Parent cargar(String nombreVista) throws IOException {
        FXMLLoader loader = new FXMLLoader(Navegacion.class.getResource("/Vista/vistas/" + nombreVista + ".fxml"));
        return loader.load();
    }

    // Carga una seccion dentro del panel central del menu principal.
    public static void mostrarEn(Pane contenedor, String nombreVista) {
        contenedorActual = contenedor;
        try {
            contenedor.getChildren().setAll(cargar(nombreVista));
        } catch (IOException e) {
            Ui.error("No se pudo abrir la vista", e.getMessage());
        }
    }

    // Cambia la seccion dentro del menu principal que esta abierto.
    public static void mostrarEnActual(String nombreVista) {
        if (contenedorActual == null) {
            Ui.error("No se pudo abrir la vista", "No hay un menu principal activo");
            return;
        }
        mostrarEn(contenedorActual, nombreVista);
    }

    // Cambia toda la ventana (login, registro, menu).
    public static void ir(String nombreVista, String titulo) {
        try {
            App.cambiarVista(nombreVista, titulo);
        } catch (IOException e) {
            Ui.error("No se pudo abrir la vista", e.getMessage());
        }
    }
}
