package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import Enumeraciones.Genero;

public class Buscador<T> {

    private final List<T> elementos;
    private final Function<T, String> texto;
    private final Function<T, Set<Genero>> generos;

    public Buscador(List<T> elementos, Function<T, String> texto, Function<T, Set<Genero>> generos) {
        this.elementos = elementos == null ? new ArrayList<T>() : new ArrayList<T>(elementos);
        this.texto = texto;
        this.generos = generos;
    }

    public List<T> buscar(String consulta, Genero genero) {
        return buscarPorGeneros(consulta, genero == null ? Collections.emptySet() : Collections.singleton(genero));
    }

    public List<T> buscarPorGeneros(String consulta, Set<Genero> seleccionados) {
        String buscado = consulta == null ? "" : consulta.trim().toLowerCase();
        List<T> resultado = new ArrayList<T>();
        for (T elemento : elementos) {
            if (!coincideTexto(elemento, buscado)) {
                continue;
            }
            if (seleccionados != null && !seleccionados.isEmpty() && !coincideGenero(elemento, seleccionados)) {
                continue;
            }
            resultado.add(elemento);
        }
        return resultado;
    }

    public List<T> getElementos() {
        return new ArrayList<T>(elementos);
    }

    private boolean coincideTexto(T elemento, String buscado) {
        if (buscado.isEmpty()) {
            return true;
        }
        String valor = texto == null ? null : texto.apply(elemento);
        return valor != null && valor.toLowerCase().contains(buscado);
    }

    private boolean coincideGenero(T elemento, Set<Genero> seleccionados) {
        Set<Genero> valores = generos == null ? null : generos.apply(elemento);
        // alcanza con que coincida uno de los generos elegidos
        return valores != null && !Collections.disjoint(valores, seleccionados);
    }
}
