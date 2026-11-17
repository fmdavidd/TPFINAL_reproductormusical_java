package GUI;

import java.util.*;
import modelo.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;

public class PlaylistsView extends ListaView<Playlist> {
    private final ComboBox<Usuario> propietario;
    private final ComboBox<String> visibilidad = UI.combo(List.of("Todas", "Públicas", "Privadas"),
            opcion -> opcion, "Visibilidad");
    public PlaylistsView(Datos d) {
        super(d,"Buscar playlist por nombre…");
        propietario = UI.combo(List.of(d.actual()),Usuario::getNombre,"Todos los propietarios");
        propietario.setId("filtro-propietario");
        filtros.getChildren().add(propietario); propietario.valueProperty().addListener((o,a,b) -> filtrar());
        visibilidad.setId("filtro-visibilidad"); visibilidad.setValue("Todas");
        filtros.getChildren().add(visibilidad);
        visibilidad.valueProperty().addListener((o,a,b) -> filtrar());
        UI.columna(tabla,"ID",Playlist::getIdPlaylist); UI.columna(tabla,"Nombre",Playlist::getNombre);
        UI.columna(tabla,"Descripción",Playlist::getDescripcion);
        UI.columna(tabla,"Propietario",p -> d.propietario(p.getIdPropietario()));
        UI.columna(tabla,"Visibilidad",p -> p.isPublica() ? "Pública" : "Privada");
        UI.columna(tabla,"Canciones",p -> p.getCanciones().size());
        UI.columna(tabla,"Duración total",Playlist::getDuracionFormateada);
        if (!d.sesion.esAdministrador() || d.sesion.puedeEditarContenido())
            acciones.getChildren().add(UI.boton("Crear playlist",() -> editar(null)));
        accionFila("Ver canciones",this::detalle, p -> true);
        accionEditable("Editar",this::editar);
        accionEditable("Eliminar",p -> {
            if (UI.confirmar("Se dará de baja «" + p.getNombre() + "».")) cambiar(() -> d.playlists.baja(p.getIdPlaylist()));
        });
        configurarEstado(Playlist::isActivo, d.playlists::listarInactivos, p -> () -> d.playlists.reactivar(p.getIdPlaylist()));
        mostrar();
    }
    private void accionEditable(String nombre, java.util.function.Consumer<Playlist> accion) {
        Button b = UI.boton(nombre,() -> accion.accept(seleccionado()));
        b.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(() -> {
            Playlist p = tabla.getSelectionModel().getSelectedItem();
            return p == null || !p.isActivo() || !d.playlists.puedeEditar(p);
        },tabla.getSelectionModel().selectedItemProperty())); acciones.getChildren().add(b);
    }
    private Usuario usuarioPropietario(int id) {
        try { return d.usuarios.buscarPorId(id); }
        catch (Excepciones.ElementoNoEncontradoException e) {
            UsuarioNormal inactivo = new UsuarioNormal(); inactivo.setIdUsuario(id);
            inactivo.setNombre("Usuario inactivo (" + id + ")"); return inactivo;
        }
    }
    @Override protected void mostrar() {
        if (d.sesion.puedeConsultarTodo()) {
            Usuario elegido = propietario.getValue();
            List<Usuario> opciones = registrosPorEstado().stream().map(p -> usuarioPropietario(p.getIdPropietario())).distinct().toList();
            propietario.getItems().setAll(opciones);
            propietario.setValue(opciones.contains(elegido) ? elegido : null);
        }
        super.mostrar();
    }
    @Override protected boolean coincide(Playlist p) {
        return UI.contiene(p.getNombre(),buscar.getText())
                && (propietario.getValue() == null || p.getIdPropietario() == propietario.getValue().getIdUsuario())
                && (!"Públicas".equals(visibilidad.getValue()) || p.isPublica())
                && (!"Privadas".equals(visibilidad.getValue()) || !p.isPublica());
    }
    @Override protected void limpiarFiltros() { propietario.setValue(null); visibilidad.setValue("Todas"); }
    @Override protected Collection<Playlist> listar() { return d.playlists.listar(); }
    @Override protected void recargar() throws Exception { d.playlists.recargar(); }
    private void editar(Playlist anterior) {
        Formulario f = new Formulario(anterior == null ? "Crear playlist" : "Editar playlist",getScene().getWindow());
        TextField nombre = f.texto("Nombre",anterior == null ? "" : anterior.getNombre());
        TextField descripcion = f.texto("Descripción",anterior == null ? "" : anterior.getDescripcion());
        CheckBox publica = f.campo("Visibilidad",new CheckBox("Pública: otros usuarios pueden verla"));
        publica.setSelected(anterior != null && anterior.isPublica());
        f.guardar(() -> {
            Playlist p = new Playlist(UI.requerido(nombre,"Nombre",80),UI.opcional(descripcion,"Descripción",200),publica.isSelected(),
                    anterior == null ? d.actual().getIdUsuario() : anterior.getIdPropietario(),true);
            if (anterior != null) p.setIdPlaylist(anterior.getIdPlaylist());
            return () -> { if (anterior == null) d.playlists.alta(p); else d.playlists.modificar(p); };
        },this::guardado); f.showAndWait();
    }
    private void detalle(Playlist p) {
        Dialog<Void> dialog = new Dialog<>(); dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Playlist · " + p.getNombre()); dialog.setResizable(true);
        dialog.setOnCloseRequest(e -> { if (dialog.getDialogPane().isDisable()) e.consume(); });
        TableView<Cancion> canciones = CancionesView.tablaCanciones();
        canciones.setId("playlist-canciones");
        // El orden de las filas es el de reproducción; no se ordena por encabezados.
        canciones.getColumns().forEach(c -> c.setSortable(false));
        UI.columna(canciones,"Orden",c -> p.getCanciones().indexOf(c)+1);
        canciones.getColumns().get(canciones.getColumns().size()-1).setSortable(false);
        Label mensaje = new Label();
        mensaje.visibleProperty().bind(mensaje.textProperty().isNotEmpty());
        mensaje.managedProperty().bind(mensaje.visibleProperty());
        Label duracion = new Label();
        Runnable actualizar = () -> {
            canciones.getItems().setAll(p.getCanciones());
            duracion.setText("Duración total: " + p.getDuracionFormateada());
            mostrar();
        };
        HBox botones = new HBox(12);
        if (d.playlists.puedeEditar(p)) {
            habilitarReordenamiento(p,canciones,dialog,mensaje,actualizar);
            botones.getChildren().add(UI.boton("Agregar canción",() -> agregarCancion(p,dialog,actualizar)));
            Button quitar = UI.boton("Quitar canción",() -> {
                Cancion c = canciones.getSelectionModel().getSelectedItem();
                if (c != null && UI.confirmar("Se quitará «" + c.getTitulo() + "» de la playlist."))
                    UI.ejecutar(dialog.getDialogPane(),mensaje,() -> d.playlists.quitarCancion(p.getIdPlaylist(),c.getIdCancion()),actualizar);
            });
            quitar.disableProperty().bind(canciones.getSelectionModel().selectedItemProperty().isNull()); botones.getChildren().add(quitar);
        }
        Label info = new Label((p.getDescripcion() == null ? "" : p.getDescripcion()) + "\nPropietario: " + d.propietario(p.getIdPropietario())
                + " · " + (p.isPublica() ? "Pública" : "Privada")); info.setWrapText(true);
        dialog.getDialogPane().setContent(new VBox(14,info,duracion,botones,canciones,mensaje));
        dialog.getDialogPane().setPrefSize(960,600);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Cerrar",ButtonBar.ButtonData.CANCEL_CLOSE));
        actualizar.run(); dialog.showAndWait();
    }
    private void habilitarReordenamiento(Playlist p, TableView<Cancion> tabla, Dialog<Void> dialog,
                                         Label mensaje, Runnable actualizar) {
        tabla.setTooltip(new Tooltip("Arrastrá una canción a otra fila para cambiar su posición."));
        tabla.setRowFactory(t -> {
            TableRow<Cancion> fila = new TableRow<>();
            fila.setOnDragDetected(evento -> {
                if (fila.isEmpty() || !d.playlists.puedeEditar(p)) return;
                ClipboardContent contenido = new ClipboardContent();
                contenido.putString(String.valueOf(fila.getItem().getIdCancion()));
                fila.startDragAndDrop(TransferMode.MOVE).setContent(contenido);
                evento.consume();
            });
            fila.setOnDragOver(evento -> {
                if (evento.getGestureSource() instanceof TableRow<?> origen && origen.getTableView() == tabla
                        && origen != fila && evento.getDragboard().hasString()) {
                    evento.acceptTransferModes(TransferMode.MOVE);
                    fila.setStyle("-fx-border-color: #4385e8; -fx-border-width: 0 0 2 0;");
                }
                evento.consume();
            });
            fila.setOnDragExited(evento -> fila.setStyle(""));
            fila.setOnDragDropped(evento -> {
                fila.setStyle("");
                boolean aceptado = false;
                if (evento.getGestureSource() instanceof TableRow<?> origen && origen.getTableView() == tabla
                        && evento.getDragboard().hasString() && !tabla.getItems().isEmpty()) {
                    // La fila de origen puede reciclarse al desplazar una tabla larga: usar el ID arrastrado.
                    int id = Integer.parseInt(evento.getDragboard().getString());
                    Cancion movida = tabla.getItems().stream().filter(c -> c.getIdCancion() == id).findFirst().orElse(null);
                    if (movida != null) {
                        int destino = fila.isEmpty() ? tabla.getItems().size() - 1 : fila.getIndex();
                        UI.ejecutar(dialog.getDialogPane(),mensaje,
                                () -> d.playlists.moverCancion(p.getIdPlaylist(),id,destino), () -> {
                                    actualizar.run(); tabla.getSelectionModel().select(movida); tabla.refresh();
                                });
                        aceptado = true;
                    }
                }
                evento.setDropCompleted(aceptado); evento.consume();
            });
            fila.setOnDragDone(evento -> fila.setStyle(""));
            return fila;
        });
    }

    private void agregarCancion(Playlist p,Dialog<Void> padre,Runnable actualizado) {
        Formulario f = new Formulario("Agregar canción a «" + p.getNombre() + "»",padre.getDialogPane().getScene().getWindow());
        ComboBox<Cancion> cancion = f.campo("Canción",UI.combo(d.canciones.listar().stream().filter(c -> !p.getCanciones().contains(c)).toList(),
                c -> c.getTitulo() + " · " + c.getArtista().getNombre(),"Seleccionar canción"));
        f.guardar(() -> {
            Cancion c = UI.elegido(cancion,"una canción");
            return () -> d.playlists.agregarCancion(p.getIdPlaylist(),c.getIdCancion());
        },actualizado); f.showAndWait();
    }
}
