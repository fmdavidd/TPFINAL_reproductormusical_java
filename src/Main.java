public class Main {
    public static void main(String[] args) {
        try {
            new Consola().ejecutar();
        } catch (Excepciones.ConexionBDException e) {
            System.out.println("No se pudo iniciar: " + e.getMessage());
        }
    }
}
