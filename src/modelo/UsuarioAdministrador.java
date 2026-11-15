package modelo;

import Enumeraciones.NivelAdmin;
import Enumeraciones.RolUsuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class UsuarioAdministrador extends Usuario {
    private NivelAdmin nivelAdmin;

    private TreeMap<LocalDateTime, String> registroAcciones = new TreeMap<>();

    public UsuarioAdministrador(){
        super();
    }

    public UsuarioAdministrador(String nombre, String email, String hashContrasenia, String salt, NivelAdmin nivelAdmin) {
        super(nombre, email, hashContrasenia, salt);
        this.nivelAdmin = nivelAdmin;
        this.registroAcciones = new TreeMap<LocalDateTime, String>();
    }

    public UsuarioAdministrador(int idUsuario, String nombre, String email, String hashContrasenia, String salt, boolean activo, LocalDate fechaRegistro, NivelAdmin nivelAdmin) {
        super(idUsuario, nombre, email, hashContrasenia, salt, activo, fechaRegistro);
        this.nivelAdmin = nivelAdmin;
        this.registroAcciones = new TreeMap<LocalDateTime, String>();
    }

    public NivelAdmin getNivelAdmin() {
        return nivelAdmin;
    }

    public void setNivelAdmin(NivelAdmin nivelAdmin) {
        this.nivelAdmin = nivelAdmin;
    }

    public TreeMap<LocalDateTime, String> getRegistroAcciones() {
        return new TreeMap<LocalDateTime, String>(registroAcciones);
    }

    public void setRegistroAcciones(TreeMap<LocalDateTime, String> registroAcciones) {
        this.registroAcciones = new TreeMap<>(registroAcciones);
    }

    public void registrarAccion(String accion) {
        LocalDateTime momento = LocalDateTime.now();
        // Evita sobrescribir otra accion si el reloj devuelve el mismo instante.
        while (registroAcciones.containsKey(momento)) {
            momento = momento.plusNanos(1);
        }
        registroAcciones.put(momento, accion);
    }

    public List<String> buscarAccionesDelDia(LocalDate fecha) {
        List<String> acciones = new ArrayList<String>();
        for (Map.Entry<LocalDateTime, String> entrada : registroAcciones.entrySet()) {
            if (entrada.getKey().toLocalDate().equals(fecha)) {
                acciones.add(entrada.getValue());
            }
        }
        return acciones;
    }

    @Override
    public RolUsuario getRol() {
        return RolUsuario.ADMIN;
    }

    @Override
    public boolean puedeAdministrar() {
        return true;
    }
}
