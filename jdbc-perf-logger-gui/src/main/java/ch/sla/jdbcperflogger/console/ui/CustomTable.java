/* 
 *  Copyright 2013 Sylvain LAURENT
 *     
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.sla.jdbcperflogger.console.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;

import javax.annotation.Nullable;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import ch.sla.jdbcperflogger.console.db.LogRepositoryConstants;

public class CustomTable extends JTable {
    private static final Color ERROR_COLOR = Color.RED;
    private static final Color DEFAULT_BG_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = Color.ORANGE;
    private static final long serialVersionUID = 1L;
    @Nullable
    private String txtToHighlightUpper;
    @Nullable
    private Long minDurationNanoToHighlight;

    CustomTable(final ResultSetDataModel tm) {
        super(tm);
    }

    // Implement table header tool tips.
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getToolTipText(@Nullable final MouseEvent e) {
                assert e != null;
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
    public Component prepareRenderer(@Nullable final TableCellRenderer renderer, final int row, final int column) {
        assert renderer != null;
        final Component component = super.prepareRenderer(renderer, row, column);

        if (!this.getSelectionModel().isSelectedIndex(row)) {
            final ResultSetDataModel model = (ResultSetDataModel) getModel();
            final int modelIndex = convertRowIndexToModel(row);

            Color bgColor = DEFAULT_BG_COLOR;
            final Integer error = (Integer) model.getValueAt(modelIndex, LogRepositoryConstants.ERROR_COLUMN);
            if (error != null && error.intValue() != 0) {
                bgColor = ERROR_COLOR;
            } else if (txtToHighlightUpper != null) {
                final String sql = (String) model.getValueAt(modelIndex, LogRepositoryConstants.RAW_SQL_COLUMN);
                if (sql != null && sql.toUpperCase().contains(txtToHighlightUpper)) {
                    bgColor = HIGHLIGHT_COLOR;
                }
            } else {
                final Long minDurationNanoToHighlight2 = minDurationNanoToHighlight;
                if (minDurationNanoToHighlight2 != null) {
                    Long duration = (Long) model.getValueAt(modelIndex,
                            LogRepositoryConstants.EXEC_PLUS_FETCH_TIME_COLUMN);
                    if (duration == null) {
                        // in case we are in group by mode
                        final BigDecimal val = (BigDecimal) model.getValueAt(modelIndex,
                                LogRepositoryConstants.TOTAL_EXEC_TIME_COLUMN);
                        if (val != null) {
                            duration = val.longValue();
                        }
                    }
                    if (duration != null && duration.longValue() >= minDurationNanoToHighlight2.longValue()) {
                        bgColor = HIGHLIGHT_COLOR;
                    }
                }
            }
            component.setBackground(bgColor);
        }
        return component;
    }

    public void setTxtToHighlight(@Nullable final String txtToHighlight) {
        if (txtToHighlight != null) {
            txtToHighlightUpper = txtToHighlight.toUpperCase();
        } else {
            txtToHighlightUpper = null;
        }
    }

    public void setMinDurationNanoToHighlight(@Nullable final Long minDurationNanoToHighlight) {
        this.minDurationNanoToHighlight = minDurationNanoToHighlight;
    }
}
