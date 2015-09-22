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

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.console.db.LogRepositoryConstants;

import org.eclipse.jdt.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;

public class CustomTable extends JTable {
    private static final long serialVersionUID = 1L;

    private static final Color ERROR_COLOR = Color.RED;
    private static final Color DEFAULT_BG_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = Color.ORANGE;
    private static final Color COMMIT_COLOR = new Color(204, 255, 102);
    private static final Color ROLLBACK_COLOR = Color.PINK;

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
            final StatementType statementType = (StatementType) model.getValueAt(modelIndex,
                    LogRepositoryConstants.STMT_TYPE_COLUMN);
            final String sql = (String) model.getValueAt(modelIndex, LogRepositoryConstants.RAW_SQL_COLUMN);

            if (statementType == StatementType.TRANSACTION && sql != null) {
                if (sql.contains("COMMIT")) {
                    bgColor = COMMIT_COLOR;
                } else if (sql.contains("ROLLBACK")) {
                    bgColor = ROLLBACK_COLOR;
                }
            }
            final Integer error = (Integer) model.getValueAt(modelIndex, LogRepositoryConstants.ERROR_COLUMN);
            if (error != null && error.intValue() != 0) {
                bgColor = ERROR_COLOR;
            } else if (txtToHighlightUpper != null) {
                if (sql != null && sql.toUpperCase().contains(txtToHighlightUpper)) {
                    bgColor = HIGHLIGHT_COLOR;
                }
            } else {
                final Long minDurationNanoToHighlight2 = minDurationNanoToHighlight;
                if (minDurationNanoToHighlight2 != null) {
                    Long duration = (Long) model.getValueAt(modelIndex,
                            LogRepositoryConstants.EXEC_PLUS_RSET_USAGE_TIME);
                    if (duration == null) {
                        // in case we are in group by mode
                        final BigDecimal val = (BigDecimal) model.getValueAt(modelIndex,
                                LogRepositoryConstants.TOTAL_EXEC_PLUS_RSET_USAGE_TIME_COLUMN);
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
