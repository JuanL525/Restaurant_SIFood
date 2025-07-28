package Cajero;

import Cliente.Cliente;
import utils.DatabaseConnection;
import Login.Login;
import utils.SesionUsuario;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Representa la ventana principal para el rol de Mesero (Cajero).
 * Muestra un mapa de mesas, su estado y permite iniciar la toma de pedidos.
 */

public class Cajero extends JFrame {
    private JPanel CajeroPanel;
    private JPanel panelInfo;
    private JPanel panelMesas;
    private JButton btnMesa1;
    private JButton btnMesa2;
    private JButton btnMesa3;
    private JButton btnMesa4;
    private JButton btnMesa5;
    private JButton btnMesa6;
    private JLabel lblUsuarioConectado;
    private JButton btnCerrarSesion;
    private JLabel lblMesa1;
    private JLabel lblMesa2;
    private JLabel lblMesa3;
    private JLabel lblMesa4;
    private JLabel lblMesa5;
    private JLabel lblMesa6;

    // Para guardar el estado de las mesas
    private Map<Integer, String> estadosMesas = new HashMap<>();
    // Para mapear IDs a botones
    private Map<Integer, JButton> mapaBotones;

    /**
     * Constructor que crea la ventana del Cajero y muestra el nombre del mesero.
     */

    public Cajero() {
        super("Cajero SIFood - Vista de Mesas");
        setContentPane(CajeroPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 650);
        setLocationRelativeTo(null);

        inicializarMapaBotones();
        configurarBotonesMesa();
        cargarEstadoMesas(); // Carga los estados y pinta los botones

        lblUsuarioConectado.setText("Atendiendo: " + SesionUsuario.getNombreCompleto());

        cargarImagen(lblMesa1, "/mesa1.png", 140, 100);
        cargarImagen(lblMesa2, "/mesa1.png", 140, 100);
        cargarImagen(lblMesa3, "/mesa1.png", 140, 100);
        cargarImagen(lblMesa4, "/mesa1.png", 140, 100);
        cargarImagen(lblMesa5, "/mesa1.png", 140, 100);
        cargarImagen(lblMesa6, "/mesa1.png", 140, 100);


        setVisible(true);

        btnCerrarSesion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                new Login();
            }
        });
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

    // Método para agrupar los botones para fácil acceso
    private void inicializarMapaBotones() {
        mapaBotones = Map.of(
                1, btnMesa1, 2, btnMesa2, 3, btnMesa3,
                4, btnMesa4, 5, btnMesa5, 6, btnMesa6
        );
    }

    // Asigna la acción a cada botón
    private void configurarBotonesMesa() {
        mapaBotones.forEach((idMesa, boton) -> {
            boton.addActionListener(e -> onMesaClick(idMesa));
        });
    }

    // Metodo que determina qué hacer al hacer clic
    private void onMesaClick(int idMesa) {
        String estadoActual = estadosMesas.get(idMesa);

        if ("disponible".equalsIgnoreCase(estadoActual)) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Desea abrir un nuevo pedido para la mesa " + idMesa + "?",
                    "Abrir Pedido", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                actualizarEstadoMesa(idMesa, "ocupada");
                Cliente ventanaDePedido = new Cliente(idMesa);
                ventanaDePedido.setVisible(true);
                cargarEstadoMesas();
            }
        } else if ("ocupada".equalsIgnoreCase(estadoActual)) {
            Object[] options = {"Generar Factura", "Cancelar"};
            int choice = JOptionPane.showOptionDialog(this,
                    "La mesa " + idMesa + " está ocupada. ¿Qué desea hacer?",
                    "Mesa Ocupada",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);

            if (choice == 0) { // Generar Factura
                String propinaStr = JOptionPane.showInputDialog(this, "Ingrese el porcentaje de propina (ej: 10):", "Facturar", JOptionPane.QUESTION_MESSAGE);
                try {
                    double propina = Double.parseDouble(propinaStr);

                    // LLAMAMOS AL PROCEDIMIENTO
                    String sql = "CALL sp_facturar_mesa(?, ?, ?)";
                    try (Connection conn = DatabaseConnection.getConnection();
                         CallableStatement cs = conn.prepareCall(sql)) {

                        cs.setInt(1, idMesa);
                        cs.setDouble(2, propina);
                        cs.registerOutParameter(3, Types.INTEGER); // Registramos el parámetro de salida
                        cs.execute();

                        int pedidoIdFacturado = cs.getInt(3); // Obtenemos el ID del pedido

                        JOptionPane.showMessageDialog(this, "¡Mesa " + idMesa + " facturada con éxito!", "Facturación Completa", JOptionPane.INFORMATION_MESSAGE);

                        // PREGUNTAMOS SI DESEA EXPORTAR
                        int exportar = JOptionPane.showConfirmDialog(this, "¿Desea exportar la factura a un archivo CSV?", "Exportar Factura", JOptionPane.YES_NO_OPTION);
                        if (exportar == JOptionPane.YES_OPTION) {
                            exportarFacturaCSV(pedidoIdFacturado);
                        }

                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Error al facturar la mesa.\n" + ex.getMessage(), "Error de BD", JOptionPane.ERROR_MESSAGE);
                    }
                    cargarEstadoMesas();

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Por favor, ingrese un número válido para la propina.", "Error de Formato", JOptionPane.WARNING_MESSAGE);
                } catch (NullPointerException ex) {
                    // El usuario presionó cancelar en el diálogo de propina
                }
            }
        }
    }

    // METODO PARA EXPORTAR A CSV
    private void exportarFacturaCSV(int pedidoId) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Factura CSV");
        fileChooser.setSelectedFile(new File("factura_pedido_" + pedidoId + ".csv"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivoGuardar = fileChooser.getSelectedFile();
            StringBuilder csvData = new StringBuilder();

            try (Connection conn = DatabaseConnection.getConnection()) {
                // 1. Obtener datos de la cabecera del pedido
                String sqlCabecera = "SELECT p.id, m.numero_mesa, u.nombre_completo, p.fecha_cierre, p.subtotal, p.propina, p.total " +
                        "FROM pedidos p " +
                        "JOIN mesas m ON p.mesa_id = m.id " +
                        "JOIN usuarios u ON p.usuario_id_mesero = u.id " +
                        "WHERE p.id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlCabecera)) {
                    pstmt.setInt(1, pedidoId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        csvData.append("Factura Pedido,").append(rs.getInt("id")).append("\n");
                        csvData.append("Fecha,").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(rs.getTimestamp("fecha_cierre"))).append("\n");
                        csvData.append("Mesa,").append(rs.getInt("numero_mesa")).append("\n");
                        csvData.append("Atendido por,").append(rs.getString("nombre_completo")).append("\n\n");
                    }
                }

                // 2. Obtener detalles del pedido
                csvData.append("--- Detalle ---\n");
                csvData.append("Cantidad,Producto,Precio Unitario,Subtotal\n");
                String sqlDetalle = "SELECT d.cantidad, pl.nombre, d.precio_unitario_congelado, (d.cantidad * d.precio_unitario_congelado) as subtotal_linea " +
                        "FROM detalle_pedidos d JOIN platos pl ON d.plato_id = pl.id " +
                        "WHERE d.pedido_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlDetalle)) {
                    pstmt.setInt(1, pedidoId);
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        csvData.append(rs.getInt("cantidad")).append(",");
                        csvData.append("\"").append(rs.getString("nombre")).append("\","); // Comillas por si el nombre tiene comas
                        csvData.append(rs.getDouble("precio_unitario_congelado")).append(",");
                        csvData.append(rs.getDouble("subtotal_linea")).append("\n");
                    }
                }

                // 3. Escribir los totales finales
                try (PreparedStatement pstmt = conn.prepareStatement(sqlCabecera)) {
                    pstmt.setInt(1, pedidoId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        csvData.append("\n--- Totales ---\n");
                        csvData.append("Subtotal:,$").append(String.format("%.2f", rs.getDouble("subtotal"))).append("\n");
                        csvData.append("Propina:,$").append(String.format("%.2f", rs.getDouble("propina"))).append("\n");
                        csvData.append("Total:,$").append(String.format("%.2f", rs.getDouble("total"))).append("\n");
                    }
                }

                // 4. Escribir al archivo
                try (FileWriter writer = new FileWriter(archivoGuardar)) {
                    writer.write(csvData.toString());
                    JOptionPane.showMessageDialog(this, "Factura exportada con éxito.", "Exportación Completa", JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (SQLException | IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al exportar la factura.", "Error de Exportación", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Actualiza el estado de una mesa específica en la base de datos.
     * @param idMesa El ID de la mesa que se va a actualizar.
     * @param nuevoEstado El nuevo estado a asignar ("ocupada" o "disponible").
     */

    // Actualiza el estado de una mesa en la base de datos
    private void actualizarEstadoMesa(int idMesa, String nuevoEstado) {
        String sql = "UPDATE mesas SET estado = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nuevoEstado);
            pstmt.setInt(2, idMesa);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al actualizar el estado de la mesa.", "Error de BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Consulta la BD y actualiza los colores de los botones
    private void cargarEstadoMesas() {
        String sql = "SELECT id, estado FROM mesas ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                estadosMesas.put(rs.getInt("id"), rs.getString("estado"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar el estado de las mesas.", "Error de BD", JOptionPane.ERROR_MESSAGE);
            return;
        }
        actualizarColoresBotones();
    }

    // Pinta los botones según el mapa de estados
    private void actualizarColoresBotones() {
        mapaBotones.forEach((idMesa, boton) -> {
            String estado = estadosMesas.getOrDefault(idMesa, "desconocido");
            switch (estado) {
                case "disponible":
                    boton.setBackground(new Color(0, 153, 51));
                    boton.setForeground(Color.WHITE);
                    break;
                case "ocupada":
                    boton.setBackground(new Color(204, 0, 0));
                    boton.setForeground(Color.WHITE);
                    break;
                default:
                    boton.setBackground(Color.LIGHT_GRAY);
                    boton.setForeground(Color.BLACK);
                    break;
            }
        });
    }
}