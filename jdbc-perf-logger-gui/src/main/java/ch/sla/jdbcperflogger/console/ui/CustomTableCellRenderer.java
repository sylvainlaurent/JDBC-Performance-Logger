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

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.console.db.LogRepository;

public class CustomTableCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
        final CustomTableCellRenderer component = (CustomTableCellRenderer) super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

        final ResultSetDataModel dataModel = (ResultSetDataModel) table.getModel();
        if (LogRepository.STMT_TYPE_COLUMN.equals(dataModel.getColumnName(column))) {
            final int modelRowIndex = table.convertRowIndexToModel(row);
            final StatementType statementType = (StatementType) dataModel.getValueAt(modelRowIndex, column);
            if (statementType != null) {
                switch (statementType) {
                case BASE_NON_PREPARED_STMT:
                    component.setForeground(Color.ORANGE);
                    component.setText("S");
                    break;
                case NON_PREPARED_QUERY_STMT:
                    component.setForeground(Color.RED);
                    component.setText("Q");
                    break;
                case NON_PREPARED_BATCH_EXECUTION:
                    component.setForeground(Color.MAGENTA);
                    component.setText("B");
                    break;
                case PREPARED_BATCH_EXECUTION:
                    component.setForeground(Color.BLUE);
                    component.setText("PB");
                    break;
                case BASE_PREPARED_STMT:
                    component.setForeground(Color.CYAN);
                    component.setText("PS");
                    break;
                case PREPARED_QUERY_STMT:
                    component.setForeground(Color.GREEN);
                    component.setText("PQ");
                    break;
                }
            }
        }
        if (value != null) {
            component.setToolTipText(value.toString());
        }

        return component;
    }
}
