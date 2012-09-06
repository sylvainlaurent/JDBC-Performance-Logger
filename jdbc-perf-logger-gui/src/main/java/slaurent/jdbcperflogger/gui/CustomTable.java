package slaurent.jdbcperflogger.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

public class CustomTable extends JTable {
    private static final long serialVersionUID = 1L;

    CustomTable(ResultSetDataModel tm) {
        super(tm);
    }

    // Implement table header tool tips.
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getToolTipText(MouseEvent e) {
                final java.awt.Point p = e.getPoint();
                final int index = columnModel.getColumnIndexAtX(p.x);
                if (index >= 0) {
                    return columnModel.getColumn(index).getHeaderValue().toString();
                } else {
                    return "";
                }
            }
        };
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        final Component component = super.prepareRenderer(renderer, row, column);

        if (this.getSelectedRow() != row) {
            final Integer error = (Integer) ((ResultSetDataModel) getModel()).getValueAt(row,
                    LogRepository.ERROR_COLUMN);
            component.setBackground(error == null || error.intValue() == 0 ? Color.WHITE : Color.RED);
        }
        return component;
    }
}
