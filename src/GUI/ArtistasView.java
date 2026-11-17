package GUI;

import java.util.*;
import Enumeraciones.Genero;
import modelo.Artista;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ArtistasView extends ListaView<Artista> {
    private final FiltroMultiple<Genero> generos;
    private final FiltroMultiple<String> paises;
    public ArtistasView(Datos d) {
        super(d, "Buscar artista por nombre…");
        generos = new FiltroMultiple<>("Géneros", "Todos los géneros", Arrays.asList(Genero.values()), UI::genero, this::filtrar);
        generos.setId("filtro-generos"); filtros.getChildren().add(generos);
        paises = new FiltroMultiple<>("Países", "Todos los países", List.of(), pais -> pais, this::filtrar);
        paises.setId("filtro-paises"); filtros.getChildren().add(paises);
        UI.columna(tabla,"ID",Artista::getIdArtista);
        UI.columna(tabla,"Nombre",Artista::getNombre);
        UI.columna(tabla,"País",Artista::getPaisOrigen);
        UI.columna(tabla,"Géneros", a -> String.join(", ", a.getGeneros().stream().map(UI::genero).sorted().toList()));
        if (d.sesion.puedeEditarContenido()) {
            acciones.getChildren().add(UI.boton("Agregar", () -> editar(null)));
            accionFila("Editar", this::editar);
            eliminar(Artista::getNombre, a -> () -> d.artistas.baja(a.getIdArtista()));
        }
        configurarEstado(Artista::isActivo, d.artistas::listarInactivos, item -> () -> d.artistas.reactivar(item.getIdArtista()));
        mostrar();
    }
    @Override protected boolean coincide(Artista a) {
        return UI.contiene(a.getNombre(), buscar.getText()) && generos.incluyeAlguno(a.getGeneros())
                && paises.incluye(pais(a));
    }
    @Override protected void limpiarFiltros() { generos.limpiar(); paises.limpiar(); }
    private String pais(Artista artista) {
        String pais = artista.getPaisOrigen();
        return pais == null || pais.isBlank() ? "Sin país" : pais.trim();
    }
    @Override protected void mostrar() {
        paises.setOpciones(registrosPorEstado().stream().map(this::pais).toList());
        super.mostrar();
    }
    @Override protected Collection<Artista> listar() { return d.artistas.listar(); }
    @Override protected void recargar() throws Exception { d.artistas.recargar(); }
    private void editar(Artista anterior) {
        Formulario f = new Formulario(anterior == null ? "Agregar artista" : "Editar artista", getScene().getWindow());
        TextField nombre = f.texto("Nombre", anterior == null ? "" : anterior.getNombre());
        TextField pais = f.texto("País de origen", anterior == null ? "" : anterior.getPaisOrigen());
        FlowPane generos = new FlowPane(10,8); generos.setPrefWrapLength(330);
        Map<Genero,CheckBox> opciones = new LinkedHashMap<>();
        for (Genero genero : Genero.values()) {
            CheckBox check = new CheckBox(UI.genero(genero));
            check.setSelected(anterior != null && anterior.getGeneros().contains(genero));
            opciones.put(genero,check); generos.getChildren().add(check);
        }
        f.campo("Géneros", generos);
        f.guardar(() -> {
            Artista a = new Artista(UI.requerido(nombre,"Nombre",80),UI.opcional(pais,"País",50),true);
            opciones.forEach((g,c) -> { if (c.isSelected()) a.agregarGenero(g); });
            if (anterior != null) a.setIdArtista(anterior.getIdArtista());
            return () -> { if (anterior == null) d.artistas.alta(a); else d.artistas.modificar(a); };
        }, this::guardado);
        f.showAndWait();
    }
}
