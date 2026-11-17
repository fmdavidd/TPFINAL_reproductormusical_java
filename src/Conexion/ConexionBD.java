package Conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String URL = "jdbc:mysql://localhost:3306/ReproductorMusicalBD";
    private static final String USUARIO = "user2";
    private static final String PASSWORD = "password2";

    public static Connection getConnection() throws SQLException {
        java.util.Properties opciones = new java.util.Properties();
        opciones.setProperty("user", USUARIO);
        opciones.setProperty("password", PASSWORD);
        opciones.setProperty("connectTimeout", "5000");
        opciones.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(URL, opciones);
    }

}
