package modelo;

import java.time.LocalDateTime;

/** Una acción persistida, sin datos de autenticación ni posibilidad de editarla. */
public class RegistroAdministrativo {
    private final int idRegistro;
    private final int idAdmin;
    private final String nombreAdmin;
    private final String emailAdmin;
    private final LocalDateTime fechaHora;
    private final String accion;

    public RegistroAdministrativo(int idRegistro, int idAdmin, String nombreAdmin, String emailAdmin,
                                  LocalDateTime fechaHora, String accion) {
        this.idRegistro = idRegistro;
        this.idAdmin = idAdmin;
        this.nombreAdmin = nombreAdmin;
        this.emailAdmin = emailAdmin;
        this.fechaHora = fechaHora;
        this.accion = accion;
    }

    public int getIdRegistro() { return idRegistro; }
    public int getIdAdmin() { return idAdmin; }
    public String getNombreAdmin() { return nombreAdmin; }
    public String getEmailAdmin() { return emailAdmin; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getAccion() { return accion; }
}
