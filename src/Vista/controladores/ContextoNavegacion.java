package Vista.controladores;

public final class ContextoNavegacion {

    public enum Tipo {
        CANCION,
        ALBUM,
        PLAYLIST,
        ARTISTA
    }

    public static final class Destino {

        private final Tipo tipo;
        private final int id;
        private final String texto;

        private Destino(Tipo tipo, int id, String texto) {
            this.tipo = tipo;
            this.id = id;
            this.texto = texto;
        }

        public Tipo getTipo() {
            return tipo;
        }

        public int getId() {
            return id;
        }

        public String getTexto() {
            return texto;
        }
    }

    private static Destino pendiente;

    private ContextoNavegacion() {
    }

    public static void preparar(Tipo tipo, int id, String texto) {
        pendiente = new Destino(tipo, id, texto == null ? "" : texto);
    }

    public static Destino consumir(Tipo tipo) {
        if (pendiente == null || pendiente.getTipo() != tipo) {
            return null;
        }
        Destino destino = pendiente;
        // se consume una sola vez, asi no queda una seleccion vieja al volver
        pendiente = null;
        return destino;
    }
}
