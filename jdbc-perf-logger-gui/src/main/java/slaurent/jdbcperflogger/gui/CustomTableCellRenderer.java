package slaurent.jdbcperflogger.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import slaurent.jdbcperflogger.StatementType;

public class CustomTableCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        final Component component = super
                .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        final ResultSetDataModel dataModel = (ResultSetDataModel) table.getModel();
        if (LogRepository.STMT_TYPE_COLUMN.equals(dataModel.getColumnName(column))) {
            final StatementType statementType = (StatementType) dataModel.getValueAt(row, column);
            if (statementType != null) {
                switch (statementType) {
                case PREPARED_QUERY_STMT:
                    component.setForeground(Color.GREEN);
                    break;
                // TODO icon for various statement types
                default:
                    component.setForeground(Color.BLACK);
                }
            }
        }

        return component;
    }
}
