package GUI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import modelo.RegistroAdministrativo;

/** Consulta del registro completo, incluso de administradores actualmente inactivos. */
public class HistorialView extends ListaView<RegistroAdministrativo> {
    private List<RegistroAdministrativo> historial = List.of();

    public HistorialView(Datos d) {
        super(d, "Buscar por acción, administrador, correo o ID…");
        d.sesion.exigirGestionUsuarios();
        UI.columna(tabla,"ID",RegistroAdministrativo::getIdRegistro);
        TableColumn<RegistroAdministrativo,LocalDateTime> fecha = new TableColumn<>("Fecha y hora");
        fecha.setCellValueFactory(fila -> new ReadOnlyObjectWrapper<>(fila.getValue().getFechaHora()));
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        fecha.setCellFactory(columna -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime valor, boolean vacia) {
                super.updateItem(valor, vacia);
                setText(vacia || valor == null ? null : formato.format(valor));
            }
        });
        tabla.getColumns().add(fecha);
        UI.columna(tabla,"ID administrador",RegistroAdministrativo::getIdAdmin);
        UI.columna(tabla,"Administrador",r -> r.getNombreAdmin() == null ? "Administrador no disponible" : r.getNombreAdmin());
        UI.columna(tabla,"Correo",RegistroAdministrativo::getEmailAdmin);
        UI.columna(tabla,"Acción",RegistroAdministrativo::getAccion);
        configurarFechas("Fecha de la acción", r -> r.getFechaHora().toLocalDate());
        // Cargar fuera del hilo gráfico; Actualizar vuelve a consultar la base.
        UI.ejecutar(this, estado, this::recargar, this::mostrar);
    }

    @Override protected boolean coincide(RegistroAdministrativo r) {
        String texto = buscar.getText();
        return UI.contiene(r.getAccion(),texto) || UI.contiene(r.getNombreAdmin(),texto)
                || UI.contiene(r.getEmailAdmin(),texto) || UI.contiene(String.valueOf(r.getIdAdmin()),texto)
                || UI.contiene(String.valueOf(r.getIdRegistro()),texto);
    }

    @Override protected Collection<RegistroAdministrativo> listar() {
        d.sesion.exigirGestionUsuarios();
        return historial;
    }

    @Override protected void recargar() throws Exception {
        historial = d.usuarios.listarHistorialAdministrativo();
    }
}
