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

import java.awt.Component;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.eclipse.jdt.annotation.Nullable;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import ch.sla.jdbcperflogger.console.db.LogRepositoryConstants;

public class StatementTimestampTableCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat tstampFormat = new SimpleDateFormat(/* "yyyy-MM-dd "+ */"HH:mm:ss.SSS");
    private long deltaTimestampBaseMillis;

    @Override
    public Component getTableCellRendererComponent(@Nullable final JTable table, @Nullable final Object value,
            final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        assert table != null;

        final StatementTimestampTableCellRenderer component = (StatementTimestampTableCellRenderer) super
                .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        final ResultSetDataModel dataModel = (ResultSetDataModel) table.getModel();
        if (value != null && LogRepositoryConstants.TSTAMP_COLUMN.equals(dataModel.getColumnName(column))) {
            final Timestamp tstamp = (Timestamp) value;
            String str = tstampFormat.format(tstamp);
            if (deltaTimestampBaseMillis != 0) {
                str += " (";
                final long delta = tstamp.getTime() - deltaTimestampBaseMillis;
                if (delta > 0) {
                    str += "+";
                }
                str += delta + "ms)";
            }
            component.setText(str);
        }
        if (value != null) {
            component.setToolTipText(value.toString());
        }

        return component;
    }

    public void setDeltaTimestampBaseMillis(final long deltaTimestampBaseMillis) {
        this.deltaTimestampBaseMillis = deltaTimestampBaseMillis;
    }

}
