package Cliente;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Una clase de ayuda compleja que actúa como renderizador y editor de celdas
 * para una JTable, permitiendo mostrar y utilizar un JButton funcional dentro
 * de una columna de la tabla.
 *
 * @author Juan Lucero
 * @version 1.0
 */

public class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
    private JTable table;
    private Action action;
    private JButton renderButton;
    private JButton editButton;
    private String text;
    private boolean isButtonColumnEditor;

    /**
     * Constructor para la columna de botones.
     * @param table La JTable a la que se le añadirá el botón.
     * @param action La acción que se ejecutará al hacer clic en el botón.
     * @param column El índice de la columna donde se renderizará el botón.
     */

    public ButtonColumn(JTable table, Action action, int column) {
        this.table = table;
        this.action = action;

        renderButton = new JButton();
        editButton = new JButton();
        editButton.setFocusPainted(false);
        editButton.addActionListener(this);

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column).setCellRenderer(this);
        columnModel.getColumn(column).setCellEditor(this);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        text = (value == null) ? "" : value.toString();
        renderButton.setText(text);
        return renderButton;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        text = (value == null) ? "" : value.toString();
        editButton.setText(text);
        return editButton;
    }

    @Override
    public Object getCellEditorValue() {
        return text;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int row = table.convertRowIndexToModel(table.getEditingRow());
        fireEditingStopped();
        ActionEvent event = new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "" + row);
        action.actionPerformed(event);
    }
}