package Cajero;

import Cliente.Cliente;
import utils.DatabaseConnection;
import Login.Login; // ¡IMPORTANTE! Importar la clase Login

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

    // Variable para guardar el estado de las mesas
    private Map<Integer, String> estadosMesas = new HashMap<>();
    // Variable para mapear IDs a botones
    private Map<Integer, JButton> mapaBotones;

    public Cajero(String nombreMesero) {
        super("Cajero SIFood - Vista de Mesas");
        setContentPane(CajeroPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        inicializarMapaBotones();
        configurarBotonesMesa();
        cargarEstadoMesas(); // Carga los estados y pinta los botones

        lblUsuarioConectado.setText("Atendiendo: " + nombreMesero);

        setVisible(true);

        btnCerrarSesion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1. Cierra la ventana actual del Cajero
                dispose();

                // 2. Crea y muestra una nueva ventana de Login
                new Login();
            }
        });
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

    // El metodo "inteligente" que decide qué hacer al hacer clic
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
                cargarEstadoMesas(); // Refresca la vista de mesas
            }
        } else if ("ocupada".equalsIgnoreCase(estadoActual)) {
            Object[] options = {"Liberar Mesa", "Cancelar"};
            int choice = JOptionPane.showOptionDialog(this,
                    "La mesa " + idMesa + " está ocupada. ¿Qué desea hacer?",
                    "Mesa Ocupada", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);

            switch (choice) {
                case 0: // Generar Factura

                    JOptionPane.showMessageDialog(this, "Liberando y cerrando mesa " + idMesa + "...");
                    actualizarEstadoMesa(idMesa, "disponible");
                    cargarEstadoMesas(); // Refresca la vista
                    break;
                case 1: // Cancelar
                    break;
            }
        }
    }

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