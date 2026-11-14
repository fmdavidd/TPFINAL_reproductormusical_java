package Excepciones;

public class ConexionBDException extends Exception {

    public ConexionBDException(String mensaje) {
        super(mensaje);
    }

    // Lo usan los DAOs: agrega el mensaje del error original de SQL
    public ConexionBDException(String mensaje, Throwable causa) {
        super(mensaje + " - " + causa.getMessage());
    }
}
