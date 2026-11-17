package GUI;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

/** Conserva el diálogo abierto cuando falla la validación o MySQL. */
public class Formulario extends Dialog<Void> {
    private final GridPane campos = new GridPane();
    private final Label estado = new Label();
    private int fila;
    private final ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
    public Formulario(String titulo, Window propietario) {
        setTitle(titulo); initOwner(propietario); setResizable(true);
        campos.setHgap(16); campos.setVgap(12); campos.setPadding(new Insets(16));
        ColumnConstraints entrada = new ColumnConstraints(); entrada.setHgrow(Priority.ALWAYS);
        campos.getColumnConstraints().addAll(new ColumnConstraints(), entrada);
        getDialogPane().setContent(new VBox(10, campos, estado));
        getDialogPane().setPrefWidth(570);
        setOnCloseRequest(e -> { if (getDialogPane().isDisable()) e.consume(); });
        getDialogPane().getButtonTypes().addAll(guardar, new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
    }
    public <T extends Node> T campo(String etiqueta, T control) {
        Label label = new Label(etiqueta); label.setLabelFor(control);
        campos.addRow(fila++, label, control); GridPane.setHgrow(control, Priority.ALWAYS); return control;
    }
    public TextField texto(String etiqueta, String valor) {
        return campo(etiqueta, new TextField(valor == null ? "" : valor));
    }
    public void guardar(java.util.function.Supplier<UI.Operacion> validar, Runnable terminado) {
        getDialogPane().lookupButton(guardar).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            try {
                UI.Operacion op = validar.get();
                UI.ejecutar(getDialogPane(), estado, op, () -> { close(); terminado.run(); });
            } catch (IllegalArgumentException ex) { estado.setText(ex.getMessage()); }
        });
    }
}
