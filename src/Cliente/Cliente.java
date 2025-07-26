package Cliente;

import Login.Login;
import utils.DatabaseConnection;
import utils.SesionUsuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
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
    private JLabel lblCarrito;


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

    private void cargarImagenCarrito() {
        // Asegúrate de que el nombre del archivo coincida con el que tienes en 'resources'
        String nombreImagen = "/carrito.png";

        try {
            URL imageUrl = getClass().getResource(nombreImagen);
            if (imageUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imageUrl);
                // Escala la imagen al tamaño del JLabel (ajusta 128x128 si es necesario)
                Image scaledImage = originalIcon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH);
                lblCarrito.setIcon(new ImageIcon(scaledImage));
            } else {
                System.err.println("No se pudo encontrar la imagen: " + nombreImagen);
                lblCarrito.setText("Imagen no encontrada");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblCarrito.setText("Error al cargar imagen");
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
        setSize(600, 450);
        setLocationRelativeTo(null);

        cargarImagenCarrito();

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
        System.out.println(">>> Creando pedido para el mesero con ID: " + SesionUsuario.getId());

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            String sqlPedido = "INSERT INTO pedidos (mesa_id, usuario_id_mesero, turno_id, estado_id, numero_comensales) VALUES (?, ?, ?, ?, ?)";
            long pedidoId;
            try (PreparedStatement pstmtPedido = conn.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
                // ID DE MESA CORRECTO
                pstmtPedido.setInt(1, this.mesaId);
                // Valores de ejemplo para los otros campos
                pstmtPedido.setInt(2, SesionUsuario.getId()); // Obtiene el ID del mesero
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
        // AÑADIMOS LA COLUMNA "IMAGEN" AL PRINCIPIO
        String[] titulosColumnas = {"Imagen", "Plato", "Descripción", "Precio ($)", "Acción"};

        // CREAMOS UN MODELO DE TABLA ESPECIAL QUE ENTIENDE DE IMÁGENES
        DefaultTableModel tableModel = new DefaultTableModel(titulosColumnas, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Le decimos a la tabla que la primera columna (índice 0) es de tipo ImageIcon
                if (columnIndex == 0) {
                    return ImageIcon.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };

        tableMenu.setModel(tableModel);
        // AÑADIMOS ALTURA A LAS FILAS PARA QUE LAS IMÁGENES SE VEAN BIEN
        tableMenu.setRowHeight(120);
        // Ajustamos el ancho de la columna de imagen
        TableColumn imageColumn = tableMenu.getColumnModel().getColumn(0);
        imageColumn.setPreferredWidth(130);


        // MODIFICAMOS LA CONSULTA PARA TRAER EL NOMBRE DE LA IMAGEN
        String sql = "SELECT nombre, descripcion, precio, imagen_nombre FROM platos WHERE disponible = true ORDER BY nombre";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // Obtenemos el nombre del archivo de la imagen
                String nombreImagen = rs.getString("imagen_nombre");
                // Cargamos la imagen como un ImageIcon
                ImageIcon iconoPlato = cargarImagenPlato(nombreImagen, 100, 100);

                // Creamos la fila, AÑADIENDO EL ICONO AL PRINCIPIO
                Object[] fila = {
                        iconoPlato,
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getDouble("precio"),
                        "Añadir"
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
                String nombrePlato = (String) tableMenu.getValueAt(fila, 1);
                double precioPlato = (Double) tableMenu.getValueAt(fila, 3);

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

        ButtonColumn botonEnTabla = new ButtonColumn(tableMenu, accionAñadir, 4);
    }

    private ImageIcon cargarImagenPlato(String nombreArchivo, int ancho, int alto) {
        if (nombreArchivo == null || nombreArchivo.isEmpty()) {
            return null; // O podrías devolver un icono de "imagen no disponible"
        }

        try {
            // Busca la imagen en la carpeta 'resources/platos/'
            URL imageUrl = getClass().getResource("/platos/" + nombreArchivo);
            if (imageUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imageUrl);
                Image scaledImage = originalIcon.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Devuelve null si no se encuentra la imagen
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

