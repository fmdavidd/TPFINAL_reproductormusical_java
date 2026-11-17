package GUI;

import java.util.*;
import Enumeraciones.Genero;
import modelo.*;
import javafx.scene.control.*;

public class CancionesView extends ListaView<Cancion> {
    private final FiltroMultiple<Genero> generos;
    private final FiltroMultiple<Artista> artistas;
    private final FiltroMultiple<Album> albumes;
    public CancionesView(Datos d) {
        super(d,"Buscar canción por título…");
        generos = new FiltroMultiple<>("Géneros", "Todos los géneros", Arrays.asList(Genero.values()), UI::genero, this::filtrar);
        generos.setId("filtro-generos"); filtros.getChildren().add(generos);
        artistas = new FiltroMultiple<>("Artistas", "Todos los artistas", d.artistas.listar(), Artista::getNombre, this::filtrar);
        artistas.setId("filtro-artistas"); filtros.getChildren().add(artistas);
        albumes = new FiltroMultiple<>("Álbumes", "Todos los álbumes", Collections.singletonList(null),
                album -> album == null ? "Sin álbum" : album.getTitulo() + " · " + album.getArtista().getNombre(), this::filtrar);
        albumes.setId("filtro-albumes"); filtros.getChildren().add(albumes);
        columnas(tabla);
        if (d.sesion.puedeEditarContenido()) {
            acciones.getChildren().add(UI.boton("Agregar", () -> editar(null)));
            accionFila("Editar",this::editar);
            eliminar(Cancion::getTitulo,c -> () -> d.canciones.baja(c.getIdCancion()));
        } else if (!d.sesion.esAdministrador()) {
            accionFila("Agregar a favoritos", c -> cambiar(() -> {
                if (!d.usuarios.agregarFavorito(d.actual().getIdUsuario(),c.getIdCancion()))
                    throw new Excepciones.ElementoDuplicadoException("Esta canción ya está en tus favoritos.");
            }));
        }
        configurarEstado(Cancion::isActivo, d.canciones::listarInactivos, item -> () -> d.canciones.reactivar(item.getIdCancion()));
        mostrar();
    }
    public static TableView<Cancion> tablaCanciones() { TableView<Cancion> t = UI.tabla(); columnas(t); return t; }
    public static void columnas(TableView<Cancion> t) {
        UI.columna(t,"ID",Cancion::getIdCancion); UI.columna(t,"Título",Cancion::getTitulo);
        UI.columna(t,"Artista", c -> c.getArtista().getNombre());
        UI.columna(t,"Álbum", c -> c.getAlbum() == null ? "Sin álbum" : c.getAlbum().getTitulo());
        UI.columna(t,"Género",c -> UI.genero(c.getGenero())); UI.columna(t,"Duración",Cancion::getDuracionFormateada);
    }
    @Override protected boolean coincide(Cancion c) {
        return UI.contiene(c.getTitulo(),buscar.getText()) && generos.incluye(c.getGenero())
                && artistas.incluye(c.getArtista()) && albumes.incluye(c.getAlbum());
    }
    @Override protected void limpiarFiltros() { generos.limpiar(); artistas.limpiar(); albumes.limpiar(); }
    @Override protected void mostrar() {
        List<Artista> opciones = new ArrayList<>(d.artistas.listar());
        List<Album> discos = new ArrayList<>(); discos.add(null);
        for (Cancion item : registrosPorEstado()) {
            opciones.add(item.getArtista());
            if (item.getAlbum() != null) discos.add(item.getAlbum());
        }
        artistas.setOpciones(opciones); albumes.setOpciones(discos); super.mostrar();
    }
    @Override protected Collection<Cancion> listar() { return d.canciones.listar(); }
    @Override protected void recargar() throws Exception { d.artistas.recargar(); d.canciones.recargar(); }
    private void editar(Cancion anterior) {
        Formulario f = new Formulario(anterior == null ? "Agregar canción" : "Editar canción",getScene().getWindow());
        TextField titulo = f.texto("Título",anterior == null ? "" : anterior.getTitulo());
        TextField duracion = f.texto("Duración (segundos)",anterior == null ? "" : "" + anterior.getDuracionSegundos());
        TextField ruta = f.texto("Ruta de archivo (opcional)",anterior == null ? "" : anterior.getRutaArchivo());
        ComboBox<Genero> g = f.campo("Género",UI.combo(Arrays.asList(Genero.values()),UI::genero,"Seleccionar género"));
        ComboBox<Artista> a = f.campo("Artista",UI.combo(d.artistas.listar(),Artista::getNombre,"Seleccionar artista"));
        ComboBox<Album> album = f.campo("Álbum (opcional)",UI.combo(Collections.emptyList(),Album::getTitulo,"Sin álbum"));
        a.valueProperty().addListener((o,v,n) -> {
            album.setValue(null); album.getItems().clear(); album.getItems().add(null);
            if (n != null) album.getItems().addAll(d.albumes.listarPorArtista(n.getIdArtista()));
        });
        if (anterior != null) { g.setValue(anterior.getGenero()); a.setValue(anterior.getArtista()); album.setValue(anterior.getAlbum()); }
        f.guardar(() -> {
            Cancion c = new Cancion(UI.requerido(titulo,"Título",100),UI.numero(duracion,"Duración",1,86400),
                    UI.opcional(ruta,"Ruta",255),UI.elegido(g,"un género"),UI.elegido(a,"un artista"),album.getValue(),true);
            if (c.getAlbum() != null && c.getAlbum().getArtista().getIdArtista() != c.getArtista().getIdArtista())
                throw new IllegalArgumentException("El álbum debe pertenecer al artista seleccionado.");
            if (anterior != null) c.setIdCancion(anterior.getIdCancion());
            return () -> { if (anterior == null) d.canciones.alta(c); else d.canciones.modificar(c); };
        },this::guardado); f.showAndWait();
    }
}
