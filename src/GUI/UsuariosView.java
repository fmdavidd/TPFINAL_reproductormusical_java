package GUI;

import java.util.*;
import Enumeraciones.*;
import Seguridad.Seguridad;
import modelo.*;
import javafx.scene.control.*;

public class UsuariosView extends ListaView<Usuario> {
    private final ComboBox<String> estadoCuenta = UI.combo(List.of("Activos", "Inactivos", "Ambos"),
            estado -> estado, "Estado de las cuentas");
    private List<Usuario> inactivos = List.of();
    public UsuariosView(Datos d) {
        super(d,"Buscar usuario por nombre o correo…"); d.sesion.exigirGestionUsuarios();
        estadoCuenta.setId("usuarios-estado");
        estadoCuenta.setValue("Activos");
        filtros.getChildren().add(estadoCuenta);
        estadoCuenta.valueProperty().addListener((o, anterior, nuevo) -> {
            if ("Activos".equals(estadoCuenta.getValue())) mostrar();
            else operar(() -> inactivos = d.usuarios.listarInactivos(), this::mostrar);
        });
        UI.columna(tabla,"Estado",u -> u.isActivo() ? "Activo" : "Inactivo");
        UI.columna(tabla,"ID",Usuario::getIdUsuario); UI.columna(tabla,"Nombre",Usuario::getNombre);
        UI.columna(tabla,"Correo",Usuario::getEmail);
        UI.columna(tabla,"Tipo",u -> u.puedeAdministrar() ? "Administrador" : "Usuario");
        UI.columna(tabla,"Premium",u -> u instanceof UsuarioNormal n ? (n.esPremium() ? "Sí" : "No") : "—");
        UI.columna(tabla,"Nivel",u -> u instanceof UsuarioAdministrador a ? a.getNivelAdmin() : "—");
        UI.columna(tabla,"Registro",Usuario::getFechaRegistro);
        acciones.getChildren().add(UI.boton("Agregar",() -> editar(null)));
        accionCuenta("Editar",true,this::editar);
        accionCuenta("Eliminar",true,u -> {
            if (u.getIdUsuario() == d.actual().getIdUsuario()) { UI.error(new IllegalArgumentException("No podés eliminar tu propia sesión.")); return; }
            if (UI.confirmar("Se dará de baja a «" + u.getNombre() + "».")) cambiar(() -> {
                d.usuarios.baja(u.getIdUsuario());
                inactivos = d.usuarios.listarInactivos();
            });
        });
        accionCuenta("Reactivar",false,u -> {
            if (UI.confirmar("Se volverá a dar de alta a «" + u.getNombre() + "».")) cambiar(() -> {
                d.usuarios.reactivar(u.getIdUsuario());
                inactivos = d.usuarios.listarInactivos();
            });
        });
        configurarFechas("Fecha de registro",Usuario::getFechaRegistro);
        mostrar();
    }
    @Override protected boolean coincide(Usuario u) { return UI.contiene(u.getNombre(),buscar.getText()) || UI.contiene(u.getEmail(),buscar.getText()); }
    @Override protected Collection<Usuario> listar() {
        d.sesion.exigirGestionUsuarios();
        if ("Inactivos".equals(estadoCuenta.getValue())) return inactivos;
        List<Usuario> resultado = new ArrayList<>(d.usuarios.listar());
        if ("Ambos".equals(estadoCuenta.getValue())) resultado.addAll(inactivos);
        return resultado;
    }
    @Override protected void limpiarFiltros() { estadoCuenta.setValue("Activos"); mostrar(); }
    private void accionCuenta(String nombre, boolean activos, java.util.function.Consumer<Usuario> accion) {
        Button boton = UI.boton(nombre, () -> {
            try { accion.accept(seleccionado()); }
            catch (IllegalArgumentException | Excepciones.PermisoDenegadoException e) { UI.error(e); }
        });
        boton.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(() -> {
            Usuario usuario = tabla.getSelectionModel().getSelectedItem();
            return usuario == null || usuario.isActivo() != activos;
        }, tabla.getSelectionModel().selectedItemProperty()));
        acciones.getChildren().add(boton);
    }
    @Override protected void recargar() throws Exception {
        d.usuarios.recargar();
        inactivos = d.usuarios.listarInactivos();
    }
    private void editar(Usuario anterior) {
        Formulario f = new Formulario(anterior == null ? "Agregar usuario" : "Editar usuario",getScene().getWindow());
        TextField nombre = f.texto("Nombre",anterior == null ? "" : anterior.getNombre());
        TextField correo = f.texto("Correo electrónico",anterior == null ? "" : anterior.getEmail());
        PasswordField clave = f.campo(anterior == null ? "Contraseña" : "Nueva contraseña (opcional)",new PasswordField());
        ComboBox<RolUsuario> rol = f.campo("Tipo de usuario",UI.combo(Arrays.asList(RolUsuario.values()),r -> r == RolUsuario.ADMIN ? "Administrador" : "Usuario","Seleccionar tipo"));
        rol.setValue(anterior == null ? RolUsuario.NORMAL : anterior.getRol());
        // UsuarioDAO no cambia el rol: conservar el tipo al editar.
        rol.setDisable(anterior != null);
        ComboBox<NivelAdmin> nivel = f.campo("Nivel administrativo",UI.combo(Arrays.asList(NivelAdmin.values()),Enum::name,"Seleccionar nivel"));
        nivel.setValue(anterior instanceof UsuarioAdministrador a ? a.getNivelAdmin() : NivelAdmin.EDITOR);
        CheckBox premium = f.campo("Cuenta premium",new CheckBox("Premium"));
        premium.setSelected(anterior instanceof UsuarioNormal n && n.esPremium());
        nivel.disableProperty().bind(rol.valueProperty().isNotEqualTo(RolUsuario.ADMIN));
        premium.disableProperty().bind(rol.valueProperty().isEqualTo(RolUsuario.ADMIN));
        f.guardar(() -> {
            String n = UI.requerido(nombre,"Nombre",60), email = UI.email(correo);
            String salt, hash;
            if (anterior != null && clave.getText().isEmpty()) { salt = anterior.getSalt(); hash = anterior.getHashContrasenia(); }
            else { String password = UI.requerido(clave,"Contraseña",255); salt = Seguridad.generarSalt(); hash = Seguridad.hashear(password,salt); }
            Usuario u;
            if (UI.elegido(rol,"un tipo") == RolUsuario.ADMIN) u = new UsuarioAdministrador(n,email,hash,salt,UI.elegido(nivel,"un nivel"));
            else {
                UsuarioNormal normal = new UsuarioNormal(n,email,hash,salt,premium.isSelected());
                if (anterior instanceof UsuarioNormal viejo) { normal.setFavoritos(viejo.getFavoritos()); normal.setFechasFavoritos(viejo.getFechasFavoritos()); }
                u = normal;
            }
            if (anterior != null) { u.setIdUsuario(anterior.getIdUsuario()); u.setFechaRegistro(anterior.getFechaRegistro()); }
            return () -> { if (anterior == null) d.usuarios.alta(u); else d.usuarios.modificar(u); };
        },this::guardado); f.showAndWait();
    }
}
