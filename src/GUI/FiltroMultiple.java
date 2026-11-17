package GUI;

import java.util.*;
import java.util.function.Function;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/** Filtro con casillas: sin selección incluye todo; varias opciones se combinan con O. */
public class FiltroMultiple<T> extends MenuButton {
    private final Set<T> seleccionados = new LinkedHashSet<>();
    private final Map<T, CheckBox> casillas = new LinkedHashMap<>();
    private final String titulo;
    private final String todos;
    private final Function<T, String> nombre;
    private final Runnable alCambiar;

    public FiltroMultiple(String titulo, String todos, Collection<T> opciones,
                          Function<T, String> nombre, Runnable alCambiar) {
        this.titulo = titulo;
        this.todos = todos;
        this.nombre = nombre;
        this.alCambiar = alCambiar;
        setPrefWidth(190);
        setOpciones(opciones);
    }

    public void setOpciones(Collection<T> opciones) {
        List<T> ordenadas = opciones.stream().distinct()
                .sorted(Comparator.comparing(nombre, String.CASE_INSENSITIVE_ORDER)).toList();
        Set<T> vigentes = new LinkedHashSet<>();
        for (T opcion : ordenadas) if (seleccionados.contains(opcion)) vigentes.add(opcion);
        boolean cambio = !seleccionados.equals(vigentes);
        seleccionados.clear(); seleccionados.addAll(vigentes);
        casillas.clear();
        VBox lista = new VBox(8);
        lista.setStyle("-fx-padding: 8;");
        for (T opcion : ordenadas) {
            CheckBox casilla = new CheckBox(nombre.apply(opcion));
            casilla.setSelected(seleccionados.contains(opcion));
            casilla.selectedProperty().addListener((o, anterior, marcada) -> {
                if (marcada) seleccionados.add(opcion); else seleccionados.remove(opcion);
                actualizarTexto(); alCambiar.run();
            });
            casillas.put(opcion, casilla); lista.getChildren().add(casilla);
        }
        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true); scroll.setPrefViewportWidth(250);
        scroll.setPrefViewportHeight(Math.min(260, Math.max(45, ordenadas.size() * 30)));
        getItems().setAll(new CustomMenuItem(scroll, false));
        setDisable(ordenadas.isEmpty()); actualizarTexto();
        if (cambio) alCambiar.run();
    }

    public boolean incluye(T opcion) {
        return seleccionados.isEmpty() || seleccionados.contains(opcion);
    }

    public boolean incluyeAlguno(Collection<T> opciones) {
        return seleccionados.isEmpty() || (opciones != null && opciones.stream().anyMatch(seleccionados::contains));
    }

    public void limpiar() { casillas.values().forEach(c -> c.setSelected(false)); }

    private void actualizarTexto() {
        String detalle = String.join(", ", seleccionados.stream().map(nombre).toList());
        setText(seleccionados.isEmpty() ? todos
                : seleccionados.size() == 1 ? detalle : titulo + ": " + seleccionados.size());
        setTooltip(new Tooltip(seleccionados.isEmpty() ? "Sin selección: se muestran todos." : detalle));
        setAccessibleText(titulo + ": " + (seleccionados.isEmpty() ? "todos" : detalle));
    }
}
