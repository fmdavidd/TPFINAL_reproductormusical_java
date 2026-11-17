package Vista.controladores;

import Vista.App;

import java.util.List;

import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import modelo.Cancion;
import modelo.Playlist;
import modelo.Usuario;
import modelo.UsuarioNormal;

public final class AgregarAPlaylistDialog {

    private AgregarAPlaylistDialog() {
    }

    public static void mostrar(Cancion cancion) {
        if (cancion == null) {
            Ui.info("Agregar a playlist", "Selecciona una cancion primero.");
            return;
        }
        Usuario usuario = Reglas.usuario();
        if (!(usuario instanceof UsuarioNormal)) {
            Ui.info("Agregar a playlist", "Solo los usuarios normales tienen playlists.");
            return;
        }

        List<Playlist> propias = App.getPlaylists().listarPorUsuario(usuario.getIdUsuario());
        if (propias.isEmpty()) {
            Ui.info("Agregar a playlist",
                    "Todavia no tenes playlists. Crea una desde la seccion Playlists.");
            return;
        }

        ComboBox<Playlist> combo = new ComboBox<>();
        combo.getItems().setAll(propias);
        combo.setConverter(Ui.conversor(Playlist::getNombre));
        combo.getSelectionModel().selectFirst();
        combo.setMaxWidth(Double.MAX_VALUE);

        Dialog<Playlist> dialogo = new Dialog<>();
        dialogo.setTitle("Agregar a playlist");
        dialogo.setHeaderText("Elegir playlist para: " + cancion.getTitulo());
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogo.getDialogPane().setContent(new VBox(8, new Label("Playlist:"), combo));
        dialogo.setResultConverter(boton -> boton == ButtonType.OK ? combo.getValue() : null);

        dialogo.showAndWait().ifPresent(playlist -> {
            boolean agregada = Ui.ejecutar("Agregar a playlist",
                    () -> App.getPlaylists().agregarCancion(playlist.getIdPlaylist(), cancion.getIdCancion()));
            if (agregada) {
                Ui.info("Agregar a playlist",
                        "Cancion agregada a '" + playlist.getNombre() + "'.");
            }
        });
    }
}
