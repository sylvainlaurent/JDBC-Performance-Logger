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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.console.db.LogRepositoryConstants;

class ResultSetDataModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private List<String> columnNames = new ArrayList<>();
    private List<Class<?>> columnTypes = new ArrayList<>();
    private List<Object[]> rows = new ArrayList<>();

    /**
     * Must be called in EDT
     *
     * @param rows
     * @param columnNames
     * @param columnTypes
     */
    void setNewData(final List<Object[]> rows, final List<String> columnNames, final List<Class<?>> columnTypes) {
        final boolean columnsChanged = !this.columnNames.equals(columnNames);
        if (columnsChanged) {
            // fireTableStructureChanged twice to force the clearing of the current selection
            ResultSetDataModel.this.fireTableStructureChanged();
        }

        this.rows = rows;
        this.columnNames = columnNames;
        this.columnTypes = columnTypes;

        if (columnsChanged) {
            ResultSetDataModel.this.fireTableStructureChanged();
        } else {
            ResultSetDataModel.this.fireTableDataChanged();
        }
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    @Nullable
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        Object o = rows.get(rowIndex)[columnIndex];
        if (o == null) {
            return null;
        }
        final String col = getColumnName(columnIndex);
        if (LogRepositoryConstants.STMT_TYPE_COLUMN.equals(col)) {
            o = StatementType.fromId(((Byte) o).byteValue());
        } else if (col.endsWith("TIME")) {
            // all time retrieved from the DB is in ns, we just convert it for display to have the best perf
            o = TimeUnit.NANOSECONDS.toMillis(((Number) o).longValue());
        }
        return o;

    }

    @Nullable
    public Object getRawValueAt(final int rowIndex, final int columnIndex) {
        return rows.get(rowIndex)[columnIndex];
    }

    @Override
    public String getColumnName(final int columnIndex) {
        return columnNames.get(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return columnTypes.get(columnIndex);
    }

    public long getIdAtRow(final int rowIndex) {
        return (Long) rows.get(rowIndex)[0];
    }

    @Nullable
    public Object getValueAt(final int rowIndex, final String columnName) {
        final int columnIndex = columnNames.indexOf(columnName);
        if (columnIndex < 0) {
            return null;
        }
        Object o = rows.get(rowIndex)[columnIndex];
        if (LogRepositoryConstants.STMT_TYPE_COLUMN.equals(columnName)) {
            o = StatementType.fromId(((Byte) o).byteValue());
        }
        return o;
    }
}