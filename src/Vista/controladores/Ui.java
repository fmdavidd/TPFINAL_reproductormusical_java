package Vista.controladores;

import Vista.App;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import Excepciones.ConexionBDException;
import Excepciones.CredencialesInvalidasException;
import Excepciones.ElementoDuplicadoException;
import Excepciones.ElementoNoEncontradoException;
import Excepciones.PermisoDenegadoException;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.util.StringConverter;
import modelo.Cancion;

public final class Ui {

    private static final Set<String> AVISOS_MOSTRADOS = new HashSet<>();

    private Ui() {
    }

    @FunctionalInterface
    public interface Accion {
        void run() throws ConexionBDException, ElementoDuplicadoException,
                ElementoNoEncontradoException, CredencialesInvalidasException,
                PermisoDenegadoException;
    }

    // Ejecuta una operacion de negocio y muestra el error si algo falla.
    public static boolean ejecutar(String titulo, Accion accion) {
        try {
            accion.run();
            return true;
        } catch (ConexionBDException | ElementoDuplicadoException
                | ElementoNoEncontradoException | CredencialesInvalidasException
                | PermisoDenegadoException e) {
            error(titulo, e);
            return false;
        }
    }

    public static String mensaje(Throwable e) {
        String texto = e.getMessage();
        if (texto == null || texto.isBlank()) {
            texto = e.getClass().getSimpleName();
        }
        return texto;
    }

    public static void error(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje == null ? "" : mensaje, ButtonType.OK);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }

    public static void error(String titulo, Throwable e) {
        error(titulo, mensaje(e));
    }

    // Muestra el error una sola vez por clave. Se usa en celdas de tabla, donde
    // el mismo fallo se repite en cada render.
    public static void errorUnaVez(String clave, String titulo, String mensaje) {
        if (AVISOS_MOSTRADOS.add(clave)) {
            error(titulo, mensaje);
        }
    }

    public static void info(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje == null ? "" : mensaje, ButtonType.OK);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }

    public static boolean confirmar(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        return alerta.showAndWait().filter(boton -> boton == ButtonType.OK).isPresent();
    }

    public static String duracion(int segundos) {
        int total = Math.max(0, segundos);
        int horas = total / 3600;
        int minutos = (total % 3600) / 60;
        int segundosRestantes = total % 60;
        if (horas > 0) {
            return String.format("%d:%02d:%02d", horas, minutos, segundosRestantes);
        }
        return String.format("%02d:%02d", minutos, segundosRestantes);
    }

    public static <S> void configurar(TableColumn<S, String> columna, Function<S, String> valor) {
        columna.setCellValueFactory(celda -> new SimpleStringProperty(valor.apply(celda.getValue())));
    }

    public static <T> StringConverter<T> conversor(Function<T, String> texto) {
        return conversor(texto, "");
    }

    public static <T> StringConverter<T> conversor(Function<T, String> texto, String textoNulo) {
        return new StringConverter<T>() {
            @Override
            public String toString(T item) {
                return item == null ? textoNulo : texto.apply(item);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    public static <T> ListCell<T> celda(Function<T, String> texto) {
        return new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : texto.apply(item));
            }
        };
    }

    public static String textoCancion(Cancion cancion) {
        String artista = cancion.getArtista() != null ? cancion.getArtista().getNombre() : "?";
        return cancion.getTitulo() + " - " + artista + " (" + duracion(cancion.getDuracionSegundos()) + ")";
    }

    // Busca el archivo de audio de forma absoluta, relativa al directorio de
    // trabajo o dentro de la carpeta musica.
    public static Path resolverArchivo(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        Path path = Paths.get(ruta);
        if (path.isAbsolute() && Files.isRegularFile(path)) {
            return path;
        }
        Path enDirectorio = Paths.get(System.getProperty("user.dir")).resolve(ruta).normalize();
        if (Files.isRegularFile(enDirectorio)) {
            return enDirectorio;
        }
        Path enSrc = Paths.get(System.getProperty("user.dir")).resolve("src").resolve(ruta).normalize();
        if (Files.isRegularFile(enSrc)) {
            return enSrc;
        }
        Path enMusica = Paths.get(System.getProperty("user.dir")).resolve("musica")
                .resolve(path.getFileName()).normalize();
        if (Files.isRegularFile(enMusica)) {
            return enMusica;
        }
        return null;
    }
}
