package Cliente;

import Cliente.ButtonColumn;
import utils.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;

public class Cliente extends JFrame {
    // --- Componentes Visuales ---
    private JTabbedPane tabMenuPane;
    private JPanel ClientePanel;
    private JTable tableMenu;
    private JScrollPane menuScrollPane;
    private JList<String> lstCarrito;
    private JButton btnRealizarPedido;

    // --- Modelos de Datos ---
    private ArrayList<OrderItem> carritoDeCompras;
    private DefaultListModel<String> carritoListModel;
    private int mesaId; // VARIABLE PARA GUARDAR EL ID DE LA MESA

    // Clase interna para representar un item en el carrito
    private static class OrderItem {
        String nombre;
        int cantidad;
        double precioUnitario;

        OrderItem(String nombre, int cantidad, double precioUnitario) {
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }

        @Override
        public String toString() {
            return String.format("%d x %s - $%.2f", cantidad, nombre, cantidad * precioUnitario);
        }
    }

    // Constructor para uso general
    public Cliente() {
        initComponents();
        cargarMenuEnTabla();
    }

    // NUEVO CONSTRUCTOR que recibe el ID de la mesa
    public Cliente(int idMesa) {
        initComponents();
        this.mesaId = idMesa; // Guardamos el ID de la mesa
        setTitle("Pedido para Mesa #" + idMesa);
        cargarMenuEnTabla();
    }

    // Metodo para inicializar todos los componentes y configuraciones de la ventana
    private void initComponents() {
        setTitle("Menú SIFood");
        setContentPane(ClientePanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        carritoDeCompras = new ArrayList<>();
        carritoListModel = new DefaultListModel<>();
        lstCarrito.setModel(carritoListModel);

        btnRealizarPedido.addActionListener(e -> realizarPedido());
    }

    private void realizarPedido() {
        if (carritoDeCompras.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío.", "Carrito Vacío", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            String sqlPedido = "INSERT INTO pedidos (mesa_id, usuario_id_mesero, turno_id, estado_id, numero_comensales) VALUES (?, ?, ?, ?, ?)";
            long pedidoId;
            try (PreparedStatement pstmtPedido = conn.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
                // ID DE MESA CORRECTO
                pstmtPedido.setInt(1, this.mesaId);
                // Valores de ejemplo para los otros campos
                pstmtPedido.setInt(2, 2); // Mesero 'agomez_mesera'
                pstmtPedido.setInt(3, 1); // Turno 'Almuerzo'
                pstmtPedido.setInt(4, 1); // Estado 'Solicitado'
                pstmtPedido.setInt(5, 1); // Comensales

                pstmtPedido.executeUpdate();

                try (ResultSet generatedKeys = pstmtPedido.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        pedidoId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("No se pudo obtener el ID del pedido.");
                    }
                }
            }

            String sqlDetalle = "INSERT INTO detalle_pedidos (pedido_id, plato_id, cantidad, precio_unitario_congelado) VALUES (?, (SELECT id FROM platos WHERE nombre = ?), ?, ?)";
            try (PreparedStatement pstmtDetalle = conn.prepareStatement(sqlDetalle)) {
                for (OrderItem item : carritoDeCompras) {
                    pstmtDetalle.setLong(1, pedidoId);
                    pstmtDetalle.setString(2, item.nombre);
                    pstmtDetalle.setInt(3, item.cantidad);
                    pstmtDetalle.setDouble(4, item.precioUnitario);
                    pstmtDetalle.addBatch();
                }
                pstmtDetalle.executeBatch();
            }

            conn.commit();
            mostrarFacturaFinal();
            this.dispose(); // Cierra la ventana de pedido después de ser exitoso

        } catch (SQLException e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            JOptionPane.showMessageDialog(this, "Error al guardar el pedido.", "Error de Pedido", JOptionPane.ERROR_MESSAGE);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void mostrarFacturaFinal() {
        StringBuilder factura = new StringBuilder("--- PEDIDO REALIZADO CON ÉXITO ---\n\n");
        double total = 0;
        for (OrderItem item : carritoDeCompras) {
            factura.append(item.toString()).append("\n");
            total += item.cantidad * item.precioUnitario;
        }
        factura.append("--------------------------------\n");
        factura.append(String.format("TOTAL A PAGAR: $%.2f", total));
        JOptionPane.showMessageDialog(this, factura.toString(), "Confirmación de Pedido", JOptionPane.INFORMATION_MESSAGE);
        carritoDeCompras.clear();
        actualizarVistaCarrito();
    }

    private void cargarMenuEnTabla() {
        String[] titulosColumnas = {"Plato", "Descripción", "Precio ($)", "Acción"};

        DefaultTableModel tableModel = new DefaultTableModel(titulosColumnas, 0);

        tableMenu.setModel(tableModel);

        String sql = "SELECT nombre, descripcion, precio FROM platos WHERE disponible = true ORDER BY nombre";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Object[] fila = {
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getDouble("precio"),
                        "Añadir" // Texto del botón
                };
                tableModel.addRow(fila);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar el menú.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        // --- Lógica del Botón "Añadir" ---

        Action accionAñadir = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                int fila = Integer.parseInt(e.getActionCommand());
                String nombrePlato = (String) tableMenu.getValueAt(fila, 0);
                double precioPlato = (Double) tableMenu.getValueAt(fila, 2);

                // Preguntar la cantidad
                String cantidadStr = JOptionPane.showInputDialog(
                        Cliente.this,
                        "¿Cuántas unidades de '" + nombrePlato + "' desea añadir?",
                        "Añadir al Carrito",
                        JOptionPane.QUESTION_MESSAGE
                );

                if (cantidadStr != null && !cantidadStr.isEmpty()) {
                    try {
                        int cantidad = Integer.parseInt(cantidadStr);
                        if (cantidad > 0) {
                            // Añadir al carrito de memoria
                            OrderItem nuevoItem = new OrderItem(nombrePlato, cantidad, precioPlato);
                            carritoDeCompras.add(nuevoItem);

                            // Actualizar el carrito visual
                            actualizarVistaCarrito();
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(Cliente.this, "Por favor, ingrese un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
                    }

                }

            }

        };

        ButtonColumn botonEnTabla = new ButtonColumn(tableMenu, accionAñadir, 3);
    }


    private void actualizarVistaCarrito() {
        // Limpiamos el modelo visual
        carritoListModel.clear();
        double total = 0;

        // Volvemos a llenar el modelo visual con los datos del carrito en memoria
        for (OrderItem item : carritoDeCompras) {
            carritoListModel.addElement(item.toString());
            total += item.cantidad * item.precioUnitario;
        }

        if (!carritoDeCompras.isEmpty()) {
            carritoListModel.addElement("--------------------------------");
            carritoListModel.addElement(String.format("TOTAL: $%.2f", total));
        }

    }

}

