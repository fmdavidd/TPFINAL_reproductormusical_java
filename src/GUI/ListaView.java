package GUI;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Function;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.transformation.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public abstract class ListaView<T> extends VBox {
    protected final Datos d;
    protected final TableView<T> tabla = UI.tabla();
    protected final TextField buscar = new TextField();
    protected final FlowPane filtros = new FlowPane(12,10);
    protected final FlowPane acciones = new FlowPane(10,10);
    protected final Label estado = new Label("Seleccioná una fila para ver las acciones disponibles.");
    private final javafx.collections.ObservableList<T> registros = FXCollections.observableArrayList();
    private final FilteredList<T> filtrados = new FilteredList<>(registros);
    private ComboBox<String> filtroEstado;
    private FiltroFecha filtroFecha;
    private Function<T,LocalDate> fechaRegistro;
    private Predicate<T> activo;
    private LecturaInactivos<T> leerInactivos;
    private Collection<T> inactivos = List.of();

    @FunctionalInterface
    protected interface LecturaInactivos<E> { Collection<E> listar() throws Exception; }
    protected ListaView(Datos datos, String ayuda) {
        d = datos; setSpacing(16); setPadding(new Insets(24));
        buscar.setPromptText(ayuda); buscar.setPrefWidth(320);
        filtros.getChildren().add(buscar);
        filtros.getChildren().add(UI.boton("Limpiar filtros", () -> { buscar.clear(); if (filtroEstado != null) filtroEstado.setValue("Activos"); if (filtroFecha != null) filtroFecha.limpiar(); limpiarFiltros(); filtrar(); }));
        acciones.getChildren().add(UI.boton("Actualizar", () -> operar(() -> { recargar(); cargarInactivos(); }, this::mostrar)));
        SortedList<T> ordenados = new SortedList<>(filtrados); ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados); buscar.textProperty().addListener((o,a,b) -> filtrar());
        estado.setWrapText(true); getChildren().addAll(filtros, acciones, tabla, estado);
    }
    protected void limpiarFiltros() { }
    protected abstract boolean coincide(T item);
    protected abstract Collection<T> listar();
    protected abstract void recargar() throws Exception;
    protected void mostrar() {
        registros.setAll(registrosPorEstado()); filtrar(); estado.setText("Lista actualizada · " + registros.size() + " registros.");
    }
    protected void filtrar() {
        filtrados.setPredicate(item -> coincide(item) && (filtroFecha == null || filtroFecha.incluye(fechaRegistro.apply(item))));
    }
    protected void configurarFechas(String titulo, Function<T,LocalDate> obtenerFecha) {
        fechaRegistro = obtenerFecha;
        filtroFecha = new FiltroFecha(titulo, this::filtrar);
        filtros.getChildren().add(filtroFecha);
    }
    protected void guardado() { mostrar(); estado.setText("Cambios guardados correctamente."); }
    protected void operar(UI.Operacion op, Runnable despues) {
        UI.ejecutar(getScene().getRoot(), estado, op, despues);
    }
    protected void cambiar(UI.Operacion op) {
        operar(() -> { op.ejecutar(); cargarInactivos(); }, () -> { mostrar(); estado.setText("Cambios guardados correctamente."); });
    }

    /** El catálogo activo sigue siendo la lista habitual; las bajas se consultan al solicitarlas. */
    protected void configurarEstado(Predicate<T> estaActivo, LecturaInactivos<T> lectura,
                                    java.util.function.Function<T,UI.Operacion> reactivar) {
        if (!d.sesion.puedeConsultarTodo()) return;
        activo = estaActivo; leerInactivos = lectura;
        filtroEstado = UI.combo(List.of("Activos", "Inactivos", "Ambos"), valor -> valor, "Estado");
        filtroEstado.setId("filtro-estado"); filtroEstado.setValue("Activos");
        filtros.getChildren().add(filtroEstado);
        UI.columna(tabla,"Estado",item -> activo.test(item) ? "Activo" : "Inactivo");
        filtroEstado.valueProperty().addListener((o,anterior,nuevo) -> {
            if ("Activos".equals(nuevo)) mostrar();
            else operar(this::cargarInactivos, this::mostrar);
        });
        if (d.sesion.puedeEditarContenido()) accionFila("Reactivar", item -> {
            if (UI.confirmar("Se volverá a dar de alta el registro seleccionado.")) cambiar(reactivar.apply(item));
        }, item -> !activo.test(item));
    }

    private void cargarInactivos() throws Exception {
        if (filtroEstado != null && !"Activos".equals(filtroEstado.getValue())) inactivos = leerInactivos.listar();
    }

    protected Collection<T> registrosPorEstado() {
        if (filtroEstado == null || "Activos".equals(filtroEstado.getValue())) return listar();
        d.sesion.exigirAdministrador();
        if ("Inactivos".equals(filtroEstado.getValue())) return new ArrayList<>(inactivos);
        List<T> todos = new ArrayList<>(listar()); todos.addAll(inactivos); return todos;
    }
    protected T seleccionado() {
        T item = tabla.getSelectionModel().getSelectedItem();
        if (item == null) throw new IllegalArgumentException("Seleccioná una fila primero."); return item;
    }
    protected void accionFila(String nombre, java.util.function.Consumer<T> accion) {
        accionFila(nombre, accion, item -> activo == null || activo.test(item));
    }
    protected void accionFila(String nombre, java.util.function.Consumer<T> accion, Predicate<T> habilitada) {
        Button b = UI.boton(nombre, () -> {
            try { accion.accept(seleccionado()); } catch (IllegalArgumentException | Excepciones.PermisoDenegadoException e) { UI.error(e); }
        });
        b.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(() -> {
            T item = tabla.getSelectionModel().getSelectedItem();
            return item == null || !habilitada.test(item);
        },tabla.getSelectionModel().selectedItemProperty())); acciones.getChildren().add(b);
    }
    protected void eliminar(java.util.function.Function<T,String> nombre, java.util.function.Function<T,UI.Operacion> op) {
        accionFila("Eliminar", item -> {
            if (UI.confirmar("Se dará de baja «" + nombre.apply(item) + "».")) cambiar(op.apply(item));
        });
    }
}
