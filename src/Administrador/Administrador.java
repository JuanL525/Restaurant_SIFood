package Administrador;

import utils.DatabaseConnection;
import Login.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.*;


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
    private JTextArea txtAreaIngredientes;
    private JTextField txtStockNuevo;
    private JTextField txtIngredienteID;
    private JPanel StockPanel;
    private JButton btnActualizarStock;
    private JTextArea txtAreaProdutosMV;
    private JTextArea txtAreaEmpleadoMes;
    private JLabel lblStock;
    private JLabel lblPanel;
    private JScrollPane scrollPaneEmpleados;

    public Administrador() {
        super("Panel de Administrador");
        setContentPane(AdminPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // DISPOSE para no cerrar toda la app
        setSize(650, 300);
        setLocationRelativeTo(null);

        // Hacemos que el JTextArea no sea editable y tenga un texto inicial
        txtAreaEmpleados.setEditable(false);
        txtAreaEmpleados.setText("Cargando empleados...");

        cargarImagen(lblAdd, "/addUser.png", 100, 100);
        cargarImagen(lblPerfil, "/admin_icon.png", 100, 100);
        cargarImagen(lblStock, "/stock.png", 90,90);
        cargarImagen(lblPanel,"/panel.png", 180,160);

        // Lógica del Botón Registrar
        btnRegistrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registrarNuevoEmpleado();
            }
        });

        cargarEmpleados();
        cargarIngredientes();
        cargarPlatosMasVendidos();
        cargarEmpleadoDelMes();


        setVisible(true);

        btnCerrarSesion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                new Login();
            }
        });

        btnActualizarStock.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarStock();
            }
        });
    }

    private void cargarPlatosMasVendidos() {
        StringBuilder texto = new StringBuilder("--- Platos Más Vendidos ---\n\n");
        // Usamos la vista existente
        String sql = "SELECT nombre, categoria, total_vendido FROM vista_gerente_platos_mas_pedidos LIMIT 5";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int i = 1;
            while (rs.next()) {
                texto.append(String.format("%d. %s (%s) - %d unidades\n",
                        i++,
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getInt("total_vendido")));
            }
            txtAreaProdutosMV.setText(texto.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            txtAreaProdutosMV.setText("Error al cargar los datos.");
        }
    }


    private void cargarEmpleadoDelMes() {
        String sql = "SELECT nombre_completo, ventas_totales, pedidos_atendidos FROM vista_gerente_ventas_por_mesero LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                String nombre = rs.getString("nombre_completo");
                double ventas = rs.getDouble("ventas_totales");
                int pedidos = rs.getInt("pedidos_atendidos");

                String texto = String.format("🏆 ¡Empleado del Mes! 🏆\n\n" +
                                "Nombre: %s\n" +
                                "Ventas Totales: $%.2f\n" +
                                "Pedidos Atendidos: %d",
                        nombre, ventas, pedidos);
                txtAreaEmpleadoMes.setText(texto);
            } else {
                txtAreaEmpleadoMes.setText("Aún no hay datos de ventas para determinar un ganador.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            txtAreaEmpleadoMes.setText("Error al cargar los datos.");
        }
    }


    private void cargarIngredientes() {
        StringBuilder ingredientesTexto = new StringBuilder("--- Inventario de Ingredientes ---\n\n");
        String sql = "SELECT id, nombre, stock_disponible, unidad_medida FROM ingredientes ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                ingredientesTexto.append("ID: ").append(rs.getInt("id"));
                ingredientesTexto.append(" | Nombre: ").append(rs.getString("nombre"));
                ingredientesTexto.append(" | Stock: ").append(rs.getDouble("stock_disponible"));
                ingredientesTexto.append(" ").append(rs.getString("unidad_medida")).append("\n");
            }
            txtAreaIngredientes.setText(ingredientesTexto.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            txtAreaIngredientes.setText("Error al cargar el inventario.");
        }
    }

    private void actualizarStock() {
        try {
            int id = Integer.parseInt(txtIngredienteID.getText());
            double nuevoStock = Double.parseDouble(txtStockNuevo.getText());

            // Usamos CallableStatement para llamar a procedimientos almacenados
            String sql = "CALL sp_actualizar_stock_ingrediente(?, ?)";

            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(sql)) {

                cs.setInt(1, id);
                cs.setDouble(2, nuevoStock);
                cs.execute();

                JOptionPane.showMessageDialog(this, "¡Stock actualizado con éxito!", "Éxito", JOptionPane.INFORMATION_MESSAGE);

                // Limpiamos los campos y refrescamos la lista
                txtIngredienteID.setText("");
                txtStockNuevo.setText("");
                cargarIngredientes();

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al actualizar en la BD.\n" + e.getMessage(), "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un ID y un Stock válidos.", "Error de Formato", JOptionPane.WARNING_MESSAGE);
        }
    }


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

        // Validación
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
            // Manejar errores
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