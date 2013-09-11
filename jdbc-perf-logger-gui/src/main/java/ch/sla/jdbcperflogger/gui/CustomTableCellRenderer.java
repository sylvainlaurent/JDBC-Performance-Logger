package ch.sla.jdbcperflogger.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import ch.sla.jdbcperflogger.StatementType;

public class CustomTableCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        final Component component = super
                .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        final ResultSetDataModel dataModel = (ResultSetDataModel) table.getModel();
        if (LogRepository.STMT_TYPE_COLUMN.equals(dataModel.getColumnName(column))) {
            final int modelRowIndex = table.convertRowIndexToModel(row);
            final StatementType statementType = (StatementType) dataModel.getValueAt(modelRowIndex, column);
            if (statementType != null) {
                switch (statementType) {
                case BASE_NON_PREPARED_STMT:
                    component.setForeground(Color.ORANGE);
                    break;
                case NON_PREPARED_QUERY_STMT:
                    component.setForeground(Color.RED);
                    break;
                case NON_PREPARED_BATCH_EXECUTION:
                    component.setForeground(Color.MAGENTA);
                    break;
                case PREPARED_BATCH_EXECUTION:
                    component.setForeground(Color.BLUE);
                    break;
                case BASE_PREPARED_STMT:
                    component.setForeground(Color.CYAN);
                    break;
                case PREPARED_QUERY_STMT:
                    component.setForeground(Color.GREEN);
                    break;
                }
            }
        }

        if (component instanceof JComponent && value != null) {
            ((JComponent) component).setToolTipText(value.toString());
        }
        return component;
    }
}
