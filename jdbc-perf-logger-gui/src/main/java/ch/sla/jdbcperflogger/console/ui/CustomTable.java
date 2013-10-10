package ch.sla.jdbcperflogger.console.ui;

import static ch.sla.jdbcperflogger.console.db.LogRepository.ERROR_COLUMN;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import ch.sla.jdbcperflogger.console.db.LogRepository;

public class CustomTable extends JTable {
    private static final Color ERROR_COLOR = Color.RED;
    private static final Color DEFAULT_BG_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = Color.ORANGE;
    private static final long serialVersionUID = 1L;
    private String txtToHighlightUpper;
    private Long minDurationNanoToHighlight;

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
            final ResultSetDataModel model = (ResultSetDataModel) getModel();
            final int modelIndex = convertRowIndexToModel(row);

            Color bgColor = DEFAULT_BG_COLOR;
            final Integer error = (Integer) model.getValueAt(modelIndex, ERROR_COLUMN);
            if (error != null && error.intValue() != 0) {
                bgColor = ERROR_COLOR;
            } else if (txtToHighlightUpper != null) {
                final String sql = (String) model.getValueAt(modelIndex, LogRepository.RAW_SQL_COLUMN);
                if (sql != null && sql.toUpperCase().contains(txtToHighlightUpper)) {
                    bgColor = HIGHLIGHT_COLOR;
                }
            } else if (minDurationNanoToHighlight != null) {
                Long duration = (Long) model.getValueAt(modelIndex, LogRepository.EXEC_PLUS_FETCH_TIME_COLUMN);
                if (duration == null) {
                    // in case we are in group by mode
                    final BigDecimal val = (BigDecimal) model.getValueAt(modelIndex,
                            LogRepository.TOTAL_EXEC_TIME_COLUMN);
                    if (val != null) {
                        duration = val.longValue();
                    }
                }
                if (duration != null && duration.longValue() >= minDurationNanoToHighlight.longValue()) {
                    bgColor = HIGHLIGHT_COLOR;
                }
            }
            component.setBackground(bgColor);
        }
        return component;
    }

    public void setTxtToHighlight(String txtToHighlight) {
        if (txtToHighlight != null) {
            txtToHighlightUpper = txtToHighlight.toUpperCase();
        } else {
            txtToHighlightUpper = null;
        }
    }

    public void setMinDurationNanoToHighlight(Long minDurationNanoToHighlight) {
        this.minDurationNanoToHighlight = minDurationNanoToHighlight;
    }
}
