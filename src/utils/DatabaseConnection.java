package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement; // ¡Este import es importante!

public class DatabaseConnection {

    // Tus datos de conexión
    private static final String HOST = "ep-plain-bird-ac780msu-pooler.sa-east-1.aws.neon.tech";
    private static final String DATABASE = "neondb";
    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_PT7sFHk4jYMJ";

    // Volvemos a la URL simple, sin el parámetro del schema
    private static final String URL = String.format("jdbc:postgresql://%s/%s?sslmode=require", HOST, DATABASE);

    private static Connection connection;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);

                // --- ESTA ES LA PARTE QUE FALTA EN TU CÓDIGO ---
                // Justo después de conectar, establecemos el schema para esta sesión.
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET search_path TO sifood_schema");
                }
                // --- FIN DE LA PARTE IMPORTANTE ---

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