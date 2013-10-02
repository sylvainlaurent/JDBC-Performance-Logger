package ch.sla.jdbcperflogger.console.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.table.AbstractTableModel;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.console.db.LogRepository;

class ResultSetDataModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private List<String> columnNames = new ArrayList<String>();
    private List<Class<?>> columnTypes = new ArrayList<Class<?>>();
    private List<Object[]> rows = new ArrayList<Object[]>();
    private boolean firstColumnIsID;

    /**
     * Must be called in EDT
     * 
     * @param rows
     * @param columnNames
     * @param columnTypes
     */
    void setNewData(List<Object[]> rows, List<String> columnNames, List<Class<?>> columnTypes) {
        final boolean columnsChanged = (this.columnNames == null) || (!this.columnNames.equals(columnNames));
        if (columnsChanged) {
            // fireTableStructureChanged twice to force the clearing of the current selection
            ResultSetDataModel.this.fireTableStructureChanged();
        }

        this.rows = rows;
        this.columnNames = columnNames;
        firstColumnIsID = "ID".equals(columnNames.get(0));
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
        if (firstColumnIsID) {
            return columnNames.size() - 1;
        } else {
            return columnNames.size();
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object o = rows.get(rowIndex)[firstColumnIsID ? columnIndex + 1 : columnIndex];
        final String col = getColumnName(columnIndex);
        if (LogRepository.STMT_TYPE_COLUMN.equals(col)) {
            o = StatementType.fromId(((Byte) o).byteValue());
        } else if (o != null && col.endsWith("TIME")) {
            // all time retrieved from the DB is in ns, we just convert it for display to have the best perf
            o = TimeUnit.NANOSECONDS.toMillis(((Number) o).longValue());
        }
        return o;

    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames.get(firstColumnIsID ? columnIndex + 1 : columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes.get(firstColumnIsID ? columnIndex + 1 : columnIndex);
    }

    public Long getIdAtRow(int rowIndex) {
        return (Long) rows.get(rowIndex)[0];
    }

    public Object getValueAt(int rowIndex, String columnName) {
        final int columnIndex = columnNames.indexOf(columnName);
        if (columnIndex < 0) {
            return null;
        }
        final Object o = rows.get(rowIndex)[columnIndex];

        return o;
    }
}