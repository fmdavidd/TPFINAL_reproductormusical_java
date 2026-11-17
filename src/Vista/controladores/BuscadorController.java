package Vista.controladores;

import Vista.App;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Enumeraciones.Genero;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import modelo.Album;
import modelo.Artista;
import modelo.Buscador;
import modelo.Cancion;
import modelo.Playlist;

public class BuscadorController {

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private TableView<Object> tblResultados;
    @FXML private TableColumn<Object, String> colTipo;
    @FXML private TableColumn<Object, String> colTitulo;
    @FXML private TableColumn<Object, String> colDetalle;
    @FXML private TableColumn<Object, String> colGeneros;
    @FXML private Label lblResultados;
    @FXML private Button btnAbrir;

    private Map<Integer, List<Cancion>> cancionesPorAlbum = new LinkedHashMap<Integer, List<Cancion>>();

    @FXML
    private void initialize() {
        tblResultados.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Ui.configurar(colTipo, this::tipoDe);
        Ui.configurar(colTitulo, this::tituloDe);
        Ui.configurar(colDetalle, this::detalleDe);
        Ui.configurar(colGeneros, this::generosDe);

        cmbGenero.getItems().setAll("Todos");
        for (Genero genero : Genero.values()) {
            cmbGenero.getItems().add(genero.name());
        }
        cmbGenero.getSelectionModel().selectFirst();

        tblResultados.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, nuevo) -> btnAbrir.setDisable(nuevo == null));
        tblResultados.setRowFactory(tv -> {
            TableRow<Object> fila = new TableRow<Object>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && fila.getItem() != null) {
                    abrirSeleccionado();
                }
            });
            return fila;
        });

        txtBuscar.setOnAction(evento -> buscar());
        btnAbrir.setDisable(true);
        recargar();
    }

    @FXML
    private void buscar() {
        recargar();
    }

    @FXML
    private void limpiarBusqueda() {
        txtBuscar.clear();
        cmbGenero.getSelectionModel().selectFirst();
        recargar();
    }

    @FXML
    private void abrirSeleccionado() {
        Object seleccionado = tblResultados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Ui.info("Buscar", "Selecciona un resultado primero.");
            return;
        }

        if (seleccionado instanceof Cancion) {
            Cancion cancion = (Cancion) seleccionado;
            abrir(ContextoNavegacion.Tipo.CANCION, cancion.getIdCancion(), cancion.getTitulo(),
                    "CancionesView");
            return;
        }
        if (seleccionado instanceof Album) {
            Album album = (Album) seleccionado;
            abrir(ContextoNavegacion.Tipo.ALBUM, album.getIdAlbum(), album.getTitulo(),
                    "AlbumesView");
            return;
        }
        if (seleccionado instanceof Playlist) {
            Playlist playlist = (Playlist) seleccionado;
            abrir(ContextoNavegacion.Tipo.PLAYLIST, playlist.getIdPlaylist(), playlist.getNombre(),
                    "PlaylistsView");
            return;
        }
        if (seleccionado instanceof Artista) {
            Artista artista = (Artista) seleccionado;
            abrir(ContextoNavegacion.Tipo.ARTISTA, artista.getIdArtista(), artista.getNombre(),
                    "ArtistasView");
        }
    }

    private void abrir(ContextoNavegacion.Tipo tipo, int id, String texto, String vista) {
        ContextoNavegacion.preparar(tipo, id, texto);
        Navegacion.mostrarEnActual(vista);
    }

    private void recargar() {
        actualizarIndices();
        String consulta = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        Genero genero = generoSeleccionado();

        List<Object> resultados = new ArrayList<Object>();
        resultados.addAll(buscadorCanciones().buscar(consulta, genero));
        resultados.addAll(buscadorAlbumes().buscar(consulta, genero));
        resultados.addAll(buscadorPlaylists().buscar(consulta, genero));
        resultados.addAll(buscadorArtistas().buscar(consulta, genero));

        tblResultados.getItems().setAll(resultados);
        lblResultados.setText(resultados.size() + (resultados.size() == 1
                ? " resultado" : " resultados"));
    }

    private Genero generoSeleccionado() {
        String valor = cmbGenero.getValue();
        if (valor == null || "Todos".equals(valor)) {
            return null;
        }
        return Genero.valueOf(valor);
    }

    private Buscador<Cancion> buscadorCanciones() {
        return new Buscador<Cancion>(App.getCanciones().listar(),
                c -> c.getTitulo() + " " + nombreArtista(c.getArtista()) + " " + tituloAlbum(c.getAlbum()),
                this::generosDeCancion);
    }

    private Buscador<Album> buscadorAlbumes() {
        return new Buscador<Album>(App.getAlbumes().listar(),
                a -> a.getTitulo() + " " + nombreArtista(a.getArtista()),
                this::generosDeAlbum);
    }

    private Buscador<Playlist> buscadorPlaylists() {
        List<Playlist> visibles = new ArrayList<Playlist>();
        for (Playlist playlist : App.getPlaylists().listar()) {
            if (Reglas.puedeVerPlaylist(playlist)) {
                visibles.add(playlist);
            }
        }
        return new Buscador<Playlist>(visibles,
                p -> p.getNombre() + " " + p.getDescripcion() + " "
                        + Reglas.nombreUsuario(p.getIdPropietario()),
                this::generosDePlaylist);
    }

    private Buscador<Artista> buscadorArtistas() {
        return new Buscador<Artista>(App.getArtistas().listar(),
                a -> a.getNombre() + " " + (a.getPaisOrigen() == null ? "" : a.getPaisOrigen()),
                this::generosDeArtista);
    }

    private void actualizarIndices() {
        Map<Integer, List<Cancion>> indices = new LinkedHashMap<Integer, List<Cancion>>();
        for (Cancion cancion : App.getCanciones().listar()) {
            if (cancion.getAlbum() == null) {
                continue;
            }
            int idAlbum = cancion.getAlbum().getIdAlbum();
            List<Cancion> canciones = indices.get(idAlbum);
            if (canciones == null) {
                canciones = new ArrayList<Cancion>();
                indices.put(idAlbum, canciones);
            }
            canciones.add(cancion);
        }
        cancionesPorAlbum = indices;
    }

    private Set<Genero> generosDeCancion(Cancion cancion) {
        if (cancion.getGenero() == null) {
            return Collections.emptySet();
        }
        return Collections.<Genero>singleton(cancion.getGenero());
    }

    private Set<Genero> generosDeAlbum(Album album) {
        Set<Genero> generos = new HashSet<Genero>();
        if (album.getArtista() != null) {
            generos.addAll(album.getArtista().getGeneros());
        }
        for (Cancion cancion : cancionesPorAlbum.getOrDefault(album.getIdAlbum(),
                Collections.<Cancion>emptyList())) {
            if (cancion.getGenero() != null) {
                generos.add(cancion.getGenero());
            }
        }
        return generos;
    }

    private Set<Genero> generosDePlaylist(Playlist playlist) {
        Set<Genero> generos = new HashSet<Genero>();
        for (Cancion cancion : playlist.getCanciones()) {
            if (cancion.getGenero() != null) {
                generos.add(cancion.getGenero());
            }
        }
        return generos;
    }

    private Set<Genero> generosDeArtista(Artista artista) {
        if (artista.getGeneros() == null || artista.getGeneros().isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<Genero>(artista.getGeneros());
    }

    private String tipoDe(Object elemento) {
        if (elemento instanceof Cancion) {
            return "Cancion";
        }
        if (elemento instanceof Album) {
            return "Album";
        }
        if (elemento instanceof Playlist) {
            return "Playlist";
        }
        if (elemento instanceof Artista) {
            return "Artista";
        }
        return "";
    }

    private String tituloDe(Object elemento) {
        if (elemento instanceof Cancion) {
            return ((Cancion) elemento).getTitulo();
        }
        if (elemento instanceof Album) {
            return ((Album) elemento).getTitulo();
        }
        if (elemento instanceof Playlist) {
            return ((Playlist) elemento).getNombre();
        }
        if (elemento instanceof Artista) {
            return ((Artista) elemento).getNombre();
        }
        return "";
    }

    private String detalleDe(Object elemento) {
        if (elemento instanceof Cancion) {
            Cancion cancion = (Cancion) elemento;
            return nombreArtista(cancion.getArtista()) + " - " + tituloAlbum(cancion.getAlbum())
                    + " - " + Ui.duracion(cancion.getDuracionSegundos());
        }
        if (elemento instanceof Album) {
            Album album = (Album) elemento;
            int total = cancionesPorAlbum.getOrDefault(album.getIdAlbum(),
                    Collections.<Cancion>emptyList()).size();
            return nombreArtista(album.getArtista()) + " - " + album.getAnioLanzamiento()
                    + " - " + total + (total == 1 ? " cancion" : " canciones");
        }
        if (elemento instanceof Playlist) {
            Playlist playlist = (Playlist) elemento;
            int total = playlist.getCanciones().size();
            return Reglas.nombreUsuario(playlist.getIdPropietario()) + " - " + total
                    + (total == 1 ? " cancion" : " canciones")
                    + " - " + (playlist.isPublica() ? "Publica" : "Privada");
        }
        if (elemento instanceof Artista) {
            Artista artista = (Artista) elemento;
            int total = App.getAlbumes().listarPorArtista(artista.getIdArtista()).size();
            String pais = artista.getPaisOrigen() == null ? "" : artista.getPaisOrigen();
            return pais + " - " + total + (total == 1 ? " album" : " albumes");
        }
        return "";
    }

    private String generosDe(Object elemento) {
        if (elemento instanceof Cancion) {
            return textoGeneros(generosDeCancion((Cancion) elemento));
        }
        if (elemento instanceof Album) {
            return textoGeneros(generosDeAlbum((Album) elemento));
        }
        if (elemento instanceof Playlist) {
            return textoGeneros(generosDePlaylist((Playlist) elemento));
        }
        if (elemento instanceof Artista) {
            return textoGeneros(generosDeArtista((Artista) elemento));
        }
        return "";
    }

    private String textoGeneros(Set<Genero> generos) {
        if (generos == null || generos.isEmpty()) {
            return "Sin generos";
        }
        List<String> nombres = new ArrayList<String>();
        for (Genero genero : generos) {
            nombres.add(genero.name());
        }
        Collections.sort(nombres);
        return String.join(", ", nombres);
    }

    private String nombreArtista(Artista artista) {
        return artista == null ? "Sin artista" : artista.getNombre();
    }

    private String tituloAlbum(Album album) {
        return album == null ? "Sin album" : album.getTitulo();
    }
}
