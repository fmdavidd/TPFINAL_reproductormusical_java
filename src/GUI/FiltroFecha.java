package GUI;

import java.time.LocalDate;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Rango inclusivo; un extremo vacío no limita la consulta. */
public class FiltroFecha extends VBox {
    private final DatePicker desde = new DatePicker();
    private final DatePicker hasta = new DatePicker();
    private final Label error = new Label();

    public FiltroFecha(String titulo, Runnable alCambiar) {
        super(4);
        desde.setId("fecha-desde"); hasta.setId("fecha-hasta");
        desde.setPromptText("Desde"); hasta.setPromptText("Hasta");
        desde.setPrefWidth(145); hasta.setPrefWidth(145);
        desde.setAccessibleText(titulo + " desde"); hasta.setAccessibleText(titulo + " hasta");
        getChildren().addAll(new Label(titulo),new HBox(8,new Label("Desde"),desde,new Label("Hasta"),hasta),error);
        error.setWrapText(true); error.setStyle("-fx-text-fill: #b42318;");
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());
        desde.valueProperty().addListener((o,a,b) -> actualizar(alCambiar));
        hasta.valueProperty().addListener((o,a,b) -> actualizar(alCambiar));
    }

    private boolean invertido() {
        return desde.getValue() != null && hasta.getValue() != null && desde.getValue().isAfter(hasta.getValue());
    }

    private void actualizar(Runnable alCambiar) {
        error.setText(invertido() ? "La fecha Desde no puede ser posterior a Hasta." : "");
        alCambiar.run();
    }

    public boolean incluye(LocalDate fecha) {
        LocalDate inicio = desde.getValue(), fin = hasta.getValue();
        if (invertido()) return false;
        if (inicio == null && fin == null) return true;
        return fecha != null && (inicio == null || !fecha.isBefore(inicio)) && (fin == null || !fecha.isAfter(fin));
    }

    public void limpiar() { desde.setValue(null); hasta.setValue(null); }
}
