package Conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {
    private static String configurar(String clave, String defecto) {
        String valor = System.getProperty(clave);
        if (valor == null) valor = System.getenv(clave);
        return valor == null ? defecto : valor;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                configurar("MUSICA_DB_URL", "jdbc:mysql://localhost:3306/ReproductorMusicalBD?connectTimeout=5000&socketTimeout=10000"),
                configurar("MUSICA_DB_USER", "root"), configurar("MUSICA_DB_PASSWORD", ""));
    }
}
