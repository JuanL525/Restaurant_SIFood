package Administrador;

import utils.DatabaseConnection;
import Login.*;

import javax.swing.*;
import java.awt.*; // Importamos Image
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL; // Importamos URL
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Administrador extends JFrame {
    private JTabbedPane AdminTab;
    private JPanel AdminPanel;
    private JPanel Registrar_Empleado;
    private JTextField txtNombre;
    private JTextField txtUserName;
    private JPasswordField txtClave;
    private JTextField txtRol;
    private JTextField txtEstado;
    private JButton btnRegistrar;
    private JPanel VerEmpleados;
    private JTextArea txtAreaEmpleados;
    private JLabel lblPerfil;
    private JLabel lblAdd;
    private JButton btnCerrarSesion;
    private JScrollPane scrollPaneEmpleados;

    public Administrador() {
        super("Panel de Administrador");
        setContentPane(AdminPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Usamos DISPOSE para no cerrar toda la app
        setSize(650, 290);
        setLocationRelativeTo(null);

        // Hacemos que el JTextArea no sea editable y tenga un texto inicial
        txtAreaEmpleados.setEditable(false);
        txtAreaEmpleados.setText("Cargando empleados...");

        cargarImagenPerfil();
        cargarImagenPerfil1();

        // --- Lógica del Botón Registrar ---
        btnRegistrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registrarNuevoEmpleado();
            }
        });

        cargarEmpleados();
        setVisible(true);

        btnCerrarSesion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                new Login();
            }
        });
    }



    private void cargarImagenPerfil() {
        // Asegúrate de que el nombre del archivo coincida con el que tienes en 'resources'
        String nombreImagen = "/admin_icon.png";

        try {
            URL imageUrl = getClass().getResource(nombreImagen);
            if (imageUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imageUrl);
                // Escala la imagen al tamaño del JLabel (ajusta 128x128 si es necesario)
                Image scaledImage = originalIcon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH);
                lblPerfil.setIcon(new ImageIcon(scaledImage));
            } else {
                System.err.println("No se pudo encontrar la imagen: " + nombreImagen);
                lblPerfil.setText("Imagen no encontrada");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblPerfil.setText("Error al cargar imagen");
        }
    }

    private void cargarImagenPerfil1() {
        // Asegúrate de que el nombre del archivo coincida con el que tienes en 'resources'
        String nombreImagen = "/addUser.png";

        try {
            URL imageUrl = getClass().getResource(nombreImagen);
            if (imageUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imageUrl);
                // Escala la imagen al tamaño del JLabel (ajusta 128x128 si es necesario)
                Image scaledImage = originalIcon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH);
                lblAdd.setIcon(new ImageIcon(scaledImage));
            } else {
                System.err.println("No se pudo encontrar la imagen: " + nombreImagen);
                lblAdd.setText("Imagen no encontrada");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblAdd.setText("Error al cargar imagen");
        }
    }

    /**
     * Lee todos los usuarios de la base de datos y los muestra en el JTextArea.
     */
    private void cargarEmpleados() {
        StringBuilder empleadosTexto = new StringBuilder("--- Lista de Empleados ---\n\n");
        String sql = "SELECT id, nombre_completo, nombre_usuario, rol_app, activo FROM usuarios ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                empleadosTexto.append("ID: ").append(rs.getInt("id"));
                empleadosTexto.append(" | Nombre: ").append(rs.getString("nombre_completo"));
                empleadosTexto.append(" | Usuario: ").append(rs.getString("nombre_usuario"));
                empleadosTexto.append(" | Rol: ").append(rs.getString("rol_app"));
                empleadosTexto.append(" | Activo: ").append(rs.getBoolean("activo")).append("\n");
            }
            txtAreaEmpleados.setText(empleadosTexto.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            txtAreaEmpleados.setText("Error al cargar los empleados.");
            JOptionPane.showMessageDialog(this, "Error al cargar datos de empleados.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Toma los datos del formulario e inserta un nuevo usuario en la base de datos.
     */
    private void registrarNuevoEmpleado() {
        String nombre = txtNombre.getText();
        String userName = txtUserName.getText();
        String clave = new String(txtClave.getPassword());
        String rol = txtRol.getText();
        boolean estado = Boolean.parseBoolean(txtEstado.getText());

        // Validamos que los campos no estén vacíos
        if (nombre.isEmpty() || userName.isEmpty() || clave.isEmpty() || rol.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String claveHash = clave;

        String sql = "INSERT INTO usuarios (nombre_completo, nombre_usuario, clave_hash, rol_app, activo) VALUES (?, ?, crypt(?, gen_salt('bf')), ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, userName);
            pstmt.setString(3, claveHash); // Guardamos el hash, no la clave original
            pstmt.setString(4, rol);
            pstmt.setBoolean(5, estado);

            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                JOptionPane.showMessageDialog(this, "¡Empleado registrado con éxito!", "Registro Exitoso", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormulario();
                cargarEmpleados(); // Recargamos la lista para mostrar al nuevo empleado
            }

        } catch (SQLException e) {
            e.printStackTrace();
            // Manejar errores comunes como un nombre de usuario duplicado
            JOptionPane.showMessageDialog(this, "Error al registrar el empleado.\nCausa probable: El nombre de usuario ya existe.", "Error de Registro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        txtUserName.setText("");
        txtClave.setText("");
        txtRol.setText("");
        txtEstado.setText("");
    }
}