package GUI;

import java.util.Collection;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

public final class UI {
    private UI() { }
    @FunctionalInterface public interface Operacion { void ejecutar() throws Exception; }
    public static Button boton(String texto, Runnable accion) {
        Button b = new Button(texto); b.setOnAction(e -> accion.run()); return b;
    }
    public static void ejecutar(Node vista, Label estado, Operacion operacion, Runnable terminado) {
        vista.setDisable(true); estado.setText("Procesando…");
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception { operacion.ejecutar(); return null; }
        };
        task.setOnSucceeded(e -> {
            vista.setDisable(false); estado.setText("Operación completada.");
            try { terminado.run(); }
            catch (Excepciones.PermisoDenegadoException ex) { estado.setText(ex.getMessage()); error(ex); }
        });
        task.setOnFailed(e -> {
            vista.setDisable(false); estado.setText("No se pudo completar la operación."); error(task.getException());
        });
        Thread hilo = new Thread(task, "operacion-musical"); hilo.setDaemon(true); hilo.start();
    }
    public static void error(Throwable e) {
        String mensaje = e.getMessage();
        if (mensaje == null || mensaje.isBlank()) mensaje = "Error inesperado: " + e.getClass().getSimpleName();
        Alert a = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        a.setTitle("No se pudo completar"); a.setHeaderText("Revisá los datos e intentá nuevamente"); a.showAndWait();
    }
    public static boolean confirmar(String texto) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, texto, ButtonType.YES, ButtonType.CANCEL);
        a.setTitle("Confirmar operación"); a.setHeaderText("¿Querés continuar?");
        ((Button)a.getDialogPane().lookupButton(ButtonType.YES)).setText("Sí, continuar");
        ((Button)a.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.YES;
    }
    public static <T,V> void columna(TableView<T> tabla, String titulo, Function<T,V> valor) {
        TableColumn<T,V> c = new TableColumn<>(titulo);
        c.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(valor.apply(row.getValue())));
        tabla.getColumns().add(c);
    }
    public static <T> TableView<T> tabla() {
        TableView<T> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPlaceholder(new Label("No hay registros. Probá cambiar los filtros o agregar uno."));
        VBox.setVgrow(t, Priority.ALWAYS); return t;
    }
    public static <T> ComboBox<T> combo(Collection<T> items, Function<T,String> nombre, String vacio) {
        ComboBox<T> combo = new ComboBox<>(); combo.getItems().addAll(items);
        combo.setPromptText(vacio); combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(T item) { return item == null ? vacio : nombre.apply(item); }
            @Override public T fromString(String texto) { return null; }
        });
        return combo;
    }
    public static String requerido(TextField campo, String nombre, int max) {
        String s = campo.getText().trim();
        if (s.isEmpty()) throw new IllegalArgumentException(nombre + ": completá este campo.");
        if (s.length() > max) throw new IllegalArgumentException(nombre + ": máximo " + max + " caracteres.");
        // Los espacios pueden formar parte de una contraseña existente.
        return campo instanceof PasswordField ? campo.getText() : s;
    }
    public static String opcional(TextField campo, String nombre, int max) {
        if (campo.getText().length() > max) throw new IllegalArgumentException(nombre + ": máximo " + max + " caracteres.");
        return campo.getText().trim();
    }
    public static String email(TextField campo) {
        String s = requerido(campo, "Correo electrónico", 100);
        if (!s.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Ingresá un correo electrónico válido.");
        return s;
    }
    public static <T> T elegido(ComboBox<T> combo, String campo) {
        if (combo.getValue() == null || !combo.getItems().contains(combo.getValue()))
            throw new IllegalArgumentException("Seleccioná " + campo + " de la lista disponible.");
        return combo.getValue();
    }
    public static int numero(TextField campo, String nombre, int min, int max) {
        try { int n = Integer.parseInt(campo.getText().trim()); if (n >= min && n <= max) return n; }
        catch (NumberFormatException e) { /* Se informa el rango a continuación. */ }
        throw new IllegalArgumentException(nombre + ": ingresá un entero entre " + min + " y " + max + ".");
    }
    public static boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase(java.util.Locale.ROOT).contains(texto.toLowerCase(java.util.Locale.ROOT));
    }
    public static String genero(Enumeraciones.Genero genero) {
        return switch (genero) {
            case CLASICA -> "Clásica";
            case ELECTRONICA -> "Electrónica";
            case HIP_HOP -> "Hip hop";
            case REGGAETON -> "Reguetón";
            default -> genero.name().substring(0,1) + genero.name().substring(1).toLowerCase(java.util.Locale.ROOT);
        };
    }
}
