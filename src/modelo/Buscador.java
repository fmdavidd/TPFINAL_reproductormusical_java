package modelo;

import java.util.ArrayList;
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
        String buscado = consulta == null ? "" : consulta.trim().toLowerCase();
        List<T> resultado = new ArrayList<T>();
        for (T elemento : elementos) {
            if (!coincideTexto(elemento, buscado)) {
                continue;
            }
            if (genero != null && !coincideGenero(elemento, genero)) {
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

    private boolean coincideGenero(T elemento, Genero genero) {
        Set<Genero> valores = generos == null ? null : generos.apply(elemento);
        return valores != null && valores.contains(genero);
    }
}
