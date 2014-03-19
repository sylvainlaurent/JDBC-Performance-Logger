package ch.sla.jdbcperflogger.console.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SortOrder;
import javax.swing.table.TableRowSorter;

import ch.sla.jdbcperflogger.console.db.LogRepositoryConstants;

/**
 * Custom {@link TableRowSorter} that allows to sort on statement ID when the user wants to sort on the timestamp. This
 * allows to have a correct order even if 2 statements occurred at the same millisecond.
 * 
 * @author slaurent
 * 
 */
public class CustomTableRowSorter extends TableRowSorter<ResultSetDataModel> {
    public CustomTableRowSorter(final ResultSetDataModel datamodel) {
        super(datamodel);
    }

    @Override
    public void toggleSortOrder(final int column) {
        if (LogRepositoryConstants.TSTAMP_COLUMN.equals(getModel().getColumnName(column))) {
            // sort on ID instead
            setMaxSortKeys(2);
            final List<SortKey> keys = new ArrayList<>(getSortKeys());
            if (keys.size() <= 1) {
                keys.clear();
                keys.add(new SortKey(column, SortOrder.DESCENDING));
                keys.add(new SortKey(0, SortOrder.DESCENDING));
            } else {
                final SortOrder previousOrder = keys.get(0).getSortOrder();
                final SortOrder newOrder = previousOrder == SortOrder.ASCENDING ? SortOrder.DESCENDING
                        : SortOrder.ASCENDING;
                keys.set(0, new SortKey(column, newOrder));
                keys.set(1, new SortKey(0, newOrder));
            }
            setSortKeys(keys);
        } else {
            setMaxSortKeys(1);
            super.toggleSortOrder(column);
        }

    }

}
