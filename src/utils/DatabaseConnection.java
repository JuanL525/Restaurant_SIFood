package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Clase de utilidad para gestionar la conexión única (Singleton) a la base de datos PostgreSQL.
 * Se encarga de establecer la conexión y configurar el schema por defecto.
 *
 * @author Juan Lucero
 * @version 1.0
 */

public class DatabaseConnection {

    // Datos de conexión
    private static final String HOST = "ep-plain-bird-ac780msu-pooler.sa-east-1.aws.neon.tech";
    private static final String DATABASE = "neondb";
    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_PT7sFHk4jYMJ";

    // URL
    private static final String URL = String.format("jdbc:postgresql://%s/%s?sslmode=require", HOST, DATABASE);

    private static Connection connection;

    /**
     * Obtiene la instancia de la conexión a la base de datos.
     * Si la conexión no existe o está cerrada, crea una nueva.
     *
     * @return La instancia de la conexión (Connection) o null si falla.
     */

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);

                // Justo después de conectar, establecemos el schema para esta sesión.
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET search_path TO sifood_schema");
                }

                System.out.println("¡Conexión y Schema configurados con éxito!");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Conexión cerrada.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}