package Login;

import Cajero.Cajero;
import Administrador.Administrador;
import utils.DatabaseConnection;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import utils.*;

/**
 * Representa la ventana de inicio de sesión y es el punto de entrada principal de la aplicación.
 * Se encarga de autenticar a los usuarios contra la base de datos y redirigirlos
 * al panel correspondiente a su rol.
 *
 * @author Juan Lucero
 * @version 1.0
 */

public class Login extends JFrame {
    private JPanel panel1;
    private JTextField txtUser;
    private JButton btnIngresar;
    private JPasswordField txtClave;
    private JLabel lblImagen;
    private JLabel lblIcon;

    /**
     * Constructor que inicializa la interfaz gráfica de la ventana de Login,
     * configura sus propiedades y asigna los listeners a los botones.
     */

    public Login() {
        super("Inicio de Sesión SIFood");
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setVisible(true);

        cargarImagen(lblIcon, "/icon.png", 95, 80);
        cargarImagen(lblImagen, "/restaurantLogin.jpg", 190, 370);

        btnIngresar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Llamamos a la nueva versión del método
                validarCredencialesConDB();
            }
        });
    }

    /**
     * Carga y escala una imagen decorativa para la interfaz de login.
     */

    private void cargarImagen(JLabel labelDestino, String nombreImagen, int ancho, int alto) {
        try {

            URL imageUrl = getClass().getResource(nombreImagen);
            if (imageUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imageUrl);

                Image scaledImage = originalIcon.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);

                labelDestino.setIcon(new ImageIcon(scaledImage));
            } else {
                System.err.println("No se pudo encontrar la imagen: " + nombreImagen);
                labelDestino.setText("Imagen no encontrada");
            }
        } catch (Exception e) {
            e.printStackTrace();
            labelDestino.setText("Error al cargar imagen");
        }
    }


    /**
     * Valida las credenciales introducidas por el usuario contra la base de datos.
     * Utiliza la clase SesionUsuario para guardar los datos del usuario si el login es exitoso
     * y abre la ventana correspondiente a su rol.
     */

    private void validarCredencialesConDB() {
        String user = txtUser.getText();
        String clave = new String(txtClave.getPassword());

        if (user.isEmpty() || clave.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese usuario y contraseña", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
            return; // Salimos del metodo si los campos estan vacíos
        }


        String sql = "SELECT id, nombre_completo, rol_app FROM usuarios WHERE nombre_usuario = ? AND clave_hash = crypt(?, clave_hash) AND activo = true";

        // Usamos try-with-resources para manejar la conexión de forma segura
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user);
            pstmt.setString(2, clave); // Compara con la columna clave_hash

            try (ResultSet rs = pstmt.executeQuery()) {
                // Si rs.next() es verdadero, encontramos un usuario válido y activo
                if (rs.next()) {
                    int idUsuario = rs.getInt("id");
                    String nombreCompleto = rs.getString("nombre_completo");
                    String rol = rs.getString("rol_app");

                    SesionUsuario.iniciarSesion(idUsuario, nombreCompleto, user, rol);

                    JOptionPane.showMessageDialog(this, "¡Bienvenido(a), " + nombreCompleto + "!");

                    // Usamos un switch para abrir la ventana correspondiente según el rol
                    switch (rol) {
                        case "Administrador":
                            new Administrador();
                            break;
                        case "Mesero":
                            // Aquí pasamos el nombre del mesero a la siguiente ventana
                            new Cajero();
                            break;
                        default:
                            JOptionPane.showMessageDialog(this, "Rol no reconocido.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                    }

                    this.dispose(); // Cierra la ventana de login

                } else {
                    // Si no hay resultados, las credenciales son incorrectas o el usuario está inactivo
                    JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.", "Error de Login", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error de conexión con la base de datos.", "Error Crítico", JOptionPane.ERROR_MESSAGE);
        }
    }
}