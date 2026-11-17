package Vista;

import java.io.IOException;

import Excepciones.ConexionBDException;
import Service.AlbumService;
import Service.ArtistaService;
import Service.CancionService;
import Service.PlaylistService;
import Service.SesionService;
import Service.UsuarioService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class App extends Application {

    // Mismos services que crea Main.java, ahora compartidos por todas las vistas
    private static UsuarioService usuarios;
    private static ArtistaService artistas;
    private static CancionService canciones;
    private static AlbumService albumes;
    private static PlaylistService playlists;
    private static SesionService sesion;

    private static Stage stage;

    @Override
    public void start(Stage primaryStage) {
        try {
            usuarios = new UsuarioService();
            sesion = new SesionService(usuarios);
            artistas = new ArtistaService();
            canciones = new CancionService();
            albumes = new AlbumService(canciones);
            playlists = new PlaylistService(canciones);
        } catch (ConexionBDException e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo conectar a MySQL: " + e.getMessage()).showAndWait();
            Platform.exit();
            return;
        }

        stage = primaryStage;
        try {
            cambiarVista("LoginView", "Reproductor Musical");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo cargar la vista: " + e.getMessage()).showAndWait();
            Platform.exit();
            return;
        }
        stage.show();
    }

    /**
     * Cambia la vista de la ventana. Busca "nombreVista.fxml" en la carpeta Vista/vistas.
     * Ejemplo: App.cambiarVista("LoginView", "Iniciar sesion");
     */
    public static void cambiarVista(String nombreVista, String titulo) throws IOException {
        Parent root = new FXMLLoader(App.class.getResource("/Vista/vistas/" + nombreVista + ".fxml")).load();
        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 1120, 720));
        } else {
            stage.getScene().setRoot(root);
        }
        stage.setTitle(titulo);
    }

    public static UsuarioService getUsuarios() { return usuarios; }
    public static ArtistaService getArtistas() { return artistas; }
    public static CancionService getCanciones() { return canciones; }
    public static AlbumService getAlbumes() { return albumes; }
    public static PlaylistService getPlaylists() { return playlists; }
    public static SesionService getSesion() { return sesion; }
}
