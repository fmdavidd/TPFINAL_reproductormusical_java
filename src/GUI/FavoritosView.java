package GUI;

import java.util.*;
import modelo.*;
import javafx.scene.control.*;

public class FavoritosView extends ListaView<Cancion> {
    public FavoritosView(Datos d) {
        super(d,"Buscar en mis favoritos…");
        if (!(d.actual() instanceof UsuarioNormal)) throw new Excepciones.PermisoDenegadoException("Los favoritos corresponden a usuarios normales.");
        CancionesView.columnas(tabla);
        UI.columna(tabla,"Agregada el",c -> ((UsuarioNormal)d.actual()).getFechaFavorito(c.getIdCancion()));
        configurarFechas("Fecha de incorporación a favoritos",c -> ((UsuarioNormal)d.actual()).getFechaFavorito(c.getIdCancion()));
        acciones.getChildren().add(UI.boton("Agregar canción",this::agregar));
        accionFila("Quitar de favoritos", c -> {
            if (UI.confirmar("Se quitará «" + c.getTitulo() + "» de tus favoritos.")) cambiar(() -> {
                if (!d.usuarios.quitarFavorito(d.actual().getIdUsuario(),c.getIdCancion()))
                    throw new Excepciones.ElementoNoEncontradoException("La canción ya no está en tus favoritos. Actualizá la lista.");
            });
        }); mostrar();
    }
    @Override protected boolean coincide(Cancion c) { return UI.contiene(c.getTitulo(),buscar.getText()); }
    @Override protected Collection<Cancion> listar() {
        UsuarioNormal u = (UsuarioNormal)d.actual();
        return d.canciones.listar().stream().filter(c -> u.esFavorita(c.getIdCancion())).toList();
    }
    @Override protected void recargar() throws Exception
    {
        d.usuarios.recargar();
        d.canciones.recargar();
    }

    private void agregar() {
        Formulario f = new Formulario("Agregar a favoritos",getScene().getWindow());
        UsuarioNormal u = (UsuarioNormal)d.actual();
        ComboBox<Cancion> cancion = f.campo("Canción",UI.combo(d.canciones.listar().stream()
                .filter(c -> !u.esFavorita(c.getIdCancion())).toList(),c -> c.getTitulo() + " · " + c.getArtista().getNombre(),"Seleccionar canción"));
        f.guardar(() -> {
            Cancion c = UI.elegido(cancion,"una canción");
            return () -> {
                if (!d.usuarios.agregarFavorito(d.actual().getIdUsuario(),c.getIdCancion()))
                    throw new Excepciones.ElementoDuplicadoException("La canción ya está en tus favoritos.");
            };
        },this::guardado); f.showAndWait();
    }
}
