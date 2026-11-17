package GUI;

import java.util.*;
import modelo.*;
import javafx.scene.control.*;

public class AlbumesView extends ListaView<Album> {
    private final FiltroMultiple<Artista> artistas;
    private final TextField anioDesde = new TextField();
    private final TextField anioHasta = new TextField();
    private final Label errorAnio = new Label();
    public AlbumesView(Datos d) {
        super(d,"Buscar álbum por título…");
        artistas = new FiltroMultiple<>("Artistas", "Todos los artistas", d.artistas.listar(), Artista::getNombre, this::filtrar);
        artistas.setId("filtro-artistas"); filtros.getChildren().add(artistas);
        anioDesde.setId("anio-desde"); anioHasta.setId("anio-hasta");
        for (TextField campo : List.of(anioDesde,anioHasta)) {
            campo.setPrefWidth(90); campo.setPromptText("Año");
            campo.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("[0-9]{0,4}") ? c : null));
            campo.textProperty().addListener((o,a,b) -> {
                errorAnio.setText(rangoAnioValido() ? "" : "Revisá los años: desde 1 y Desde menor o igual a Hasta.");
                filtrar();
            });
        }
        errorAnio.visibleProperty().bind(errorAnio.textProperty().isNotEmpty());
        errorAnio.managedProperty().bind(errorAnio.visibleProperty());
        errorAnio.setStyle("-fx-text-fill: #b42318;");
        filtros.getChildren().addAll(new Label("Año desde"),anioDesde,new Label("Hasta"),anioHasta,errorAnio);
        UI.columna(tabla,"ID",Album::getIdAlbum); UI.columna(tabla,"Título",Album::getTitulo);
        UI.columna(tabla,"Artista",a -> a.getArtista().getNombre());
        UI.columna(tabla,"Año",Album::getAnioLanzamiento);
        accionFila("Ver canciones", a -> {
            try {
                Dialog<Void> detalle = new Dialog<>(); detalle.initOwner(getScene().getWindow());
                detalle.setTitle(a.getTitulo()); detalle.setResizable(true);
                TableView<Cancion> canciones = CancionesView.tablaCanciones();
                canciones.getItems().setAll(d.albumes.listarCanciones(a.getIdAlbum()));
                detalle.getDialogPane().setContent(canciones); detalle.getDialogPane().setPrefSize(800,450);
                detalle.getDialogPane().getButtonTypes().add(new ButtonType("Cerrar",ButtonBar.ButtonData.CANCEL_CLOSE)); detalle.showAndWait();
            } catch (Excepciones.ElementoNoEncontradoException e) { UI.error(e); }
        });
        if (d.sesion.puedeEditarContenido()) {
            acciones.getChildren().add(UI.boton("Agregar", () -> editar(null)));
            accionFila("Editar", this::editar);
            eliminar(Album::getTitulo,a -> () -> d.albumes.baja(a.getIdAlbum()));
        }
        configurarEstado(Album::isActivo, d.albumes::listarInactivos, item -> () -> d.albumes.reactivar(item.getIdAlbum()));
        mostrar();
    }
    @Override protected boolean coincide(Album a) {
        return UI.contiene(a.getTitulo(),buscar.getText()) && artistas.incluye(a.getArtista())
                && rangoAnioValido()
                && (anioDesde.getText().isEmpty() || a.getAnioLanzamiento() >= Integer.parseInt(anioDesde.getText()))
                && (anioHasta.getText().isEmpty() || (a.getAnioLanzamiento() > 0 && a.getAnioLanzamiento() <= Integer.parseInt(anioHasta.getText())));
    }
    private boolean rangoAnioValido() {
        int inicio = anioDesde.getText().isEmpty() ? 1 : Integer.parseInt(anioDesde.getText());
        int fin = anioHasta.getText().isEmpty() ? 9999 : Integer.parseInt(anioHasta.getText());
        return inicio > 0 && fin > 0 && inicio <= fin;
    }
    @Override protected void limpiarFiltros() { artistas.limpiar(); anioDesde.clear(); anioHasta.clear(); }
    @Override protected void mostrar() {
        List<Artista> opciones = new ArrayList<>(d.artistas.listar());
        for (Album item : registrosPorEstado()) opciones.add(item.getArtista());
        artistas.setOpciones(opciones); super.mostrar();
    }
    @Override protected Collection<Album> listar() { return d.albumes.listar(); }
    @Override protected void recargar() throws Exception { d.artistas.recargar(); d.albumes.recargar(); d.canciones.recargar(); }
    private void editar(Album anterior) {
        Formulario f = new Formulario(anterior == null ? "Agregar álbum" : "Editar álbum", getScene().getWindow());
        TextField titulo = f.texto("Título",anterior == null ? "" : anterior.getTitulo());
        TextField anio = f.texto("Año de lanzamiento",anterior == null ? "" : "" + anterior.getAnioLanzamiento());
        TextField portada = f.texto("Ruta de portada (opcional)",anterior == null ? "" : anterior.getPortada());
        ComboBox<Artista> elegido = f.campo("Artista",UI.combo(d.artistas.listar(),Artista::getNombre,"Seleccionar artista"));
        if (anterior != null) elegido.setValue(anterior.getArtista());
        f.guardar(() -> {
            Album a = new Album(UI.requerido(titulo,"Título",100),UI.numero(anio,"Año",1,java.time.Year.now().getValue()+1),
                    UI.opcional(portada,"Portada",255),UI.elegido(elegido,"un artista"),true);
            if (anterior != null) a.setIdAlbum(anterior.getIdAlbum());
            return () -> { if (anterior == null) d.albumes.alta(a); else d.albumes.modificar(a); };
        },this::guardado); f.showAndWait();
    }
}
