package Vista.controladores;

import Vista.App;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import modelo.Usuario;

public class MenuPrincipalController {

    @FXML private StackPane contenido;
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private Button btnFavoritos;
    @FXML private Button btnUsuarios;
    @FXML private Button btnAcciones;

    @FXML
    private void initialize() {
        Usuario usuario = Reglas.usuario();
        if (usuario == null) {
            Navegacion.ir("LoginView", "Reproductor Musical");
            return;
        }

        lblUsuario.setText(usuario.getNombre());
        lblRol.setText(Reglas.descripcionRol());

        ocultar(btnFavoritos, Reglas.esAdmin());
        ocultar(btnUsuarios, !Reglas.puedeVerUsuarios());
        ocultar(btnAcciones, !Reglas.esAdmin());

        inicio();
    }

    private void ocultar(Node nodo, boolean ocultar) {
        nodo.setVisible(!ocultar);
        nodo.setManaged(!ocultar);
    }

    @FXML
    private void inicio() {
        Navegacion.mostrarEn(contenido, "InicioView");
    }

    @FXML
    private void buscar() {
        Navegacion.mostrarEn(contenido, "BuscadorView");
    }

    @FXML
    private void canciones() {
        Navegacion.mostrarEn(contenido, "CancionesView");
    }

    @FXML
    private void artistas() {
        Navegacion.mostrarEn(contenido, "ArtistasView");
    }

    @FXML
    private void albumes() {
        Navegacion.mostrarEn(contenido, "AlbumesView");
    }

    @FXML
    private void playlists() {
        Navegacion.mostrarEn(contenido, "PlaylistsView");
    }

    @FXML
    private void favoritos() {
        Navegacion.mostrarEn(contenido, "FavoritosView");
    }

    @FXML
    private void perfil() {
        Navegacion.mostrarEn(contenido, "PerfilView");
    }

    @FXML
    private void usuarios() {
        Navegacion.mostrarEn(contenido, "UsuariosView");
    }

    @FXML
    private void acciones() {
        Navegacion.mostrarEn(contenido, "RegistroAccionesView");
    }

    @FXML
    private void cerrarSesion() {
        ReproductorAudioFX.getInstancia().dispose();
        App.getSesion().cerrarSesion();
        Navegacion.ir("LoginView", "Reproductor Musical");
    }
}
