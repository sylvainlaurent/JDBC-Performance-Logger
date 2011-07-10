package slaurent.jdbcperflogger;

import java.util.Calendar;
import java.util.Date;

public class SqlTypedValueWithCalendar extends SqlTypedValue {
    final Calendar calendar;

    SqlTypedValueWithCalendar(final Date value, final Calendar calendar, final int sqlType) {
        super(value, sqlType);
        this.calendar = calendar;
    }

}
