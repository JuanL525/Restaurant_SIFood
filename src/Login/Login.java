package Login;

import Cajero.Cajero;
import Administrador.Administrador;
import utils.DatabaseConnection; // ¡Muy importante importar nuestra clase de conexión!

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Login extends JFrame {
    private JPanel panel1;
    private JTextField txtUser;
    private JButton btnIngresar;
    private JPasswordField txtClave;

    public Login() {
        super("Inicio de Sesión SIFood");
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 150);
        setLocationRelativeTo(null);
        setVisible(true);

        btnIngresar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ahora llamamos a la nueva versión del método
                validarCredencialesConDB();
            }
        });
    }

    /**
     * Este nuevo método se conecta a la base de datos para validar las credenciales.
     */
    private void validarCredencialesConDB() {
        String user = txtUser.getText();
        String clave = new String(txtClave.getPassword());

        if (user.isEmpty() || clave.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese usuario y contraseña", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
            return; // Salimos del método si los campos están vacíos
        }


        String sql = "SELECT nombre_completo, rol_app FROM usuarios WHERE nombre_usuario = ? AND clave_hash = crypt(?, clave_hash) AND activo = true";

        // Usamos try-with-resources para manejar la conexión de forma segura
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user);
            pstmt.setString(2, clave); // Compara con la columna clave_hash

            try (ResultSet rs = pstmt.executeQuery()) {
                // Si rs.next() es verdadero, encontramos un usuario válido y activo
                if (rs.next()) {
                    String nombreCompleto = rs.getString("nombre_completo");
                    String rol = rs.getString("rol_app");

                    JOptionPane.showMessageDialog(this, "¡Bienvenido(a), " + nombreCompleto + "!");

                    // Usamos un switch para abrir la ventana correcta según el rol
                    switch (rol) {
                        case "Administrador":
                            new Administrador();
                            break;
                        case "Mesero":
                            // Aquí le pasamos el nombre del mesero a la siguiente ventana
                            new Cajero(nombreCompleto);
                            break;
                        case "Cocinero":
                            // new Cocina(); // Si tuvieras una ventana para la cocina
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