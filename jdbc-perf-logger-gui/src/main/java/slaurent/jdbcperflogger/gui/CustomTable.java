package slaurent.jdbcperflogger.gui;

import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;

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
                // final int realIndex = columnModel.getColumn(index).getModelIndex();
                return columnModel.getColumn(index).getHeaderValue().toString();
            }
        };
    }
}
