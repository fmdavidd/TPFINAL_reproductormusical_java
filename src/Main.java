import java.util.List;

import DAO.CancionDAO;
import Excepciones.ConexionBDException;
import modelo.Cancion;

public class Main {

    public static void main(String[] args) {
        CancionDAO cancionDAO = new CancionDAO();

        try {
            List<Cancion> canciones = cancionDAO.listarActivos();
            for (Cancion c : canciones) {
                System.out.println(c.getIdCancion() + " - " + c.getTitulo());
            }
            System.out.println("Total: " + canciones.size());
        } catch (ConexionBDException e) {
            System.out.println("Error de base de datos: " + e.getMessage());
        }
    }
}