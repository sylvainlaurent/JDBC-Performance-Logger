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
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.console.db.LogRepositoryConstants;

import static java.sql.Connection.*;

@SuppressWarnings("serial")
public class CustomTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(@Nullable final JTable table, @Nullable final Object value,
            final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        assert table != null;

        final CustomTableCellRenderer component = (CustomTableCellRenderer) super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

        final int columnModelIndex = table.convertColumnIndexToModel(column);

        final ResultSetDataModel dataModel = (ResultSetDataModel) table.getModel();
        if (LogRepositoryConstants.STMT_TYPE_COLUMN.equals(dataModel.getColumnName(columnModelIndex))) {
            final int modelRowIndex = table.convertRowIndexToModel(row);
            final StatementType statementType = (StatementType) dataModel.getValueAt(modelRowIndex, columnModelIndex);
            assert statementType != null;
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
                case TRANSACTION:
                    component.setForeground(Color.DARK_GRAY);
                    component.setText("TX");
                    break;
            }

            component.setToolTipText(value + " (use STATEMENTTYPE=" + statementType.getId()
                    + " in the \"Advanced filter\")");
        } else if (LogRepositoryConstants.TRANSACTION_ISOLATION_COLUMN.equals(dataModel.getColumnName(columnModelIndex))) {
            final int modelRowIndex = table.convertRowIndexToModel(row);
            final Integer transactionIsolation = (Integer) dataModel.getValueAt(modelRowIndex, columnModelIndex);
            if (transactionIsolation != null ) {
                final Function<String, String> toDescription = text -> text + " (" + transactionIsolation + ")";
                switch (transactionIsolation) {
                    case TRANSACTION_READ_UNCOMMITTED:
                        component.setText("RU");
                        component.setToolTipText(toDescription.apply("Read Uncommitted"));
                        break;
                    case TRANSACTION_READ_COMMITTED:
                        component.setText("RC");
                        component.setToolTipText(toDescription.apply("Read Committed"));
                        break;
                    case TRANSACTION_REPEATABLE_READ:
                        component.setText("RR");
                        component.setToolTipText(toDescription.apply("Repeatable Read"));
                        break;
                    case TRANSACTION_SERIALIZABLE:
                        component.setText("SE");
                        component.setToolTipText(toDescription.apply("Serializable"));
                        break;
                }
            }

        } else if (value != null) {
            component.setToolTipText(value.toString());
        }

        return component;
    }

}
