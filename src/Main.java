import Excepciones.ConexionBDException;
import Excepciones.CredencialesInvalidasException;
import Service.CancionService;
import Service.SesionService;
import Service.UsuarioService;
import modelo.Cancion;
import modelo.Usuario;

public class Main {

    public static void main(String[] args) {
        try {
            UsuarioService usuarioService = new UsuarioService();
            CancionService cancionService = new CancionService();
            SesionService sesionService = new SesionService(usuarioService);

            // Prueba 1: login correcto (en el script: juan@mail.com / 1234)
            Usuario u = sesionService.iniciarSesion("juan@mail.com", "1234");
            System.out.println("Login OK: " + u.getNombre() + " (" + u.getRol() + ")"
                    + " - administrador: " + sesionService.esAdministrador());
            sesionService.cerrarSesion();

            // Prueba 2: login de administrador (admin@musica.com / admin123)
            sesionService.iniciarSesion("admin@musica.com", "admin123");
            System.out.println("Login admin OK - administrador: " + sesionService.esAdministrador());
            sesionService.cerrarSesion();

            // Prueba 3: contrasenia incorrecta
            try {
                sesionService.iniciarSesion("juan@mail.com", "mal");
            } catch (CredencialesInvalidasException e) {
                System.out.println("Contrasenia incorrecta -> " + e.getMessage());
            }

            // Prueba 4: listar canciones desde la coleccion en memoria
            for (Cancion c : cancionService.listar()) {
                System.out.println(c.getIdCancion() + " - " + c.getTitulo());
            }
            System.out.println("Total: " + cancionService.listar().size());

        } catch (ConexionBDException e) {
            System.out.println("Error de base de datos: " + e.getMessage());
        } catch (CredencialesInvalidasException e) {
            System.out.println("Login fallido: " + e.getMessage());
        }
    }
}
