package GUI;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MainView extends BorderPane {
    private final Label titulo = new Label();
    public MainView(Datos d, Runnable salir) {
        titulo.getStyleClass().add("titulo");
        Region espacio = new Region(); HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(20, titulo, espacio,
                new Label(d.actual().getNombre() + (d.sesion.esAdministrador() ? " · " + ((modelo.UsuarioAdministrador)d.actual()).getNivelAdmin() : " · Usuario")),
                UI.boton("Cerrar sesión", () -> {
                    if (UI.confirmar("Se cerrará tu sesión y volverás a la pantalla de acceso.")) salir.run();
                }));
        cabecera.setPadding(new Insets(20)); cabecera.getStyleClass().add("cabecera"); setTop(cabecera);
        VBox menu = new VBox(12); menu.setPadding(new Insets(24,16,24,16)); menu.setPrefWidth(190); menu.getStyleClass().add("menu");
        menu.getChildren().add(new Label("BIBLIOTECA"));
        agregar(menu,"Canciones", () -> new CancionesView(d));
        agregar(menu,"Artistas", () -> new ArtistasView(d));
        agregar(menu,"Álbumes", () -> new AlbumesView(d));
        agregar(menu,"Playlists", () -> new PlaylistsView(d));
        if (!d.sesion.esAdministrador()) agregar(menu,"Favoritos", () -> new FavoritosView(d));
        if (d.sesion.puedeGestionarUsuarios()) {
            agregar(menu,"Usuarios", () -> new UsuariosView(d));
            agregar(menu,"Historial administrativo", () -> new HistorialView(d));
        }
        setLeft(menu); mostrar("Canciones", new CancionesView(d));
    }
    private void agregar(VBox menu, String nombre, java.util.function.Supplier<Node> vista) {
        Button boton = UI.boton(nombre, () -> {
            try { mostrar(nombre, vista.get()); }
            catch (Excepciones.PermisoDenegadoException e) { UI.error(e); }
        });
        boton.setMaxWidth(Double.MAX_VALUE); menu.getChildren().add(boton);
    }
    private void mostrar(String nombre, Node vista) { titulo.setText(nombre); setCenter(vista); }
}
