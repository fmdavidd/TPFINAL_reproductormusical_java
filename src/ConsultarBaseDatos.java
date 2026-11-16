import Conexion.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Ejemplo de lectura JDBC. Las tablas y los datos se cargan antes con los scripts SQL.
// Para utilizar el sistema con usuarios, permisos y menus, ejecutar Main.
public class ConsultarBaseDatos {
    public static void main(String[] args) {
        String sql = "SELECT c.id_cancion, c.titulo, c.duracion_segundos, ar.nombre AS artista, "
                + "al.titulo AS album FROM cancion c "
                + "JOIN artista ar ON ar.id_artista = c.id_artista "
                + "LEFT JOIN album al ON al.id_album = c.id_album "
                + "WHERE c.activo = TRUE AND ar.activo = TRUE "
                + "AND (c.id_album IS NULL OR (al.activo = TRUE AND al.id_artista = c.id_artista)) "
                + "ORDER BY c.id_cancion";

        try (Connection conexion = ConexionBD.getConnection();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet resultados = consulta.executeQuery()) {
            System.out.println("Conexion correcta. Canciones cargadas en la base:");
            int cantidad = 0;
            while (resultados.next()) {
                String album = resultados.getString("album");
                if (album == null) album = "Single";
                System.out.println(resultados.getInt("id_cancion") + " - "
                        + resultados.getString("titulo") + " / "
                        + resultados.getString("artista") + " / " + album + " / "
                        + resultados.getInt("duracion_segundos") + " segundos");
                cantidad++;
            }
            System.out.println("Total de canciones disponibles: " + cantidad);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1045) {
                System.out.println("MySQL rechazo las credenciales. Configure MUSICA_DB_USER y MUSICA_DB_PASSWORD.");
            } else if (e.getErrorCode() == 1049 || e.getErrorCode() == 1146) {
                System.out.println("Falta la base o sus tablas. Ejecute los scripts SQL en una base nueva.");
            } else {
                System.out.println("No se pudo consultar MySQL: " + e.getMessage());
            }
        }
    }
}
