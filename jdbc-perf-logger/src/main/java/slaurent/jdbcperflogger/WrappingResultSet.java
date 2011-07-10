package slaurent.jdbcperflogger;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public class WrappingResultSet implements ResultSet {
    private final ResultSet wrappedResultSet;

    public WrappingResultSet(final ResultSet wrappedResultSet) {
        this.wrappedResultSet = wrappedResultSet;
    }

    public <T> T unwrap(final Class<T> iface) throws SQLException {
        return wrappedResultSet.unwrap(iface);
    }

    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return wrappedResultSet.isWrapperFor(iface);
    }

    public boolean next() throws SQLException {
        return wrappedResultSet.next();
    }

    public void close() throws SQLException {
        wrappedResultSet.close();
    }

    public boolean wasNull() throws SQLException {
        return wrappedResultSet.wasNull();
    }

    public String getString(final int columnIndex) throws SQLException {
        return wrappedResultSet.getString(columnIndex);
    }

    public boolean getBoolean(final int columnIndex) throws SQLException {
        return wrappedResultSet.getBoolean(columnIndex);
    }

    public byte getByte(final int columnIndex) throws SQLException {
        return wrappedResultSet.getByte(columnIndex);
    }

    public short getShort(final int columnIndex) throws SQLException {
        return wrappedResultSet.getShort(columnIndex);
    }

    public int getInt(final int columnIndex) throws SQLException {
        return wrappedResultSet.getInt(columnIndex);
    }

    public long getLong(final int columnIndex) throws SQLException {
        return wrappedResultSet.getLong(columnIndex);
    }

    public float getFloat(final int columnIndex) throws SQLException {
        return wrappedResultSet.getFloat(columnIndex);
    }

    public double getDouble(final int columnIndex) throws SQLException {
        return wrappedResultSet.getDouble(columnIndex);
    }

    @SuppressWarnings("deprecation")
    public BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
        return wrappedResultSet.getBigDecimal(columnIndex, scale);
    }

    public byte[] getBytes(final int columnIndex) throws SQLException {
        return wrappedResultSet.getBytes(columnIndex);
    }

    public Date getDate(final int columnIndex) throws SQLException {
        return wrappedResultSet.getDate(columnIndex);
    }

    public Time getTime(final int columnIndex) throws SQLException {
        return wrappedResultSet.getTime(columnIndex);
    }

    public Timestamp getTimestamp(final int columnIndex) throws SQLException {
        return wrappedResultSet.getTimestamp(columnIndex);
    }

    public InputStream getAsciiStream(final int columnIndex) throws SQLException {
        return wrappedResultSet.getAsciiStream(columnIndex);
    }

    @SuppressWarnings("deprecation")
    public InputStream getUnicodeStream(final int columnIndex) throws SQLException {
        return wrappedResultSet.getUnicodeStream(columnIndex);
    }

    public InputStream getBinaryStream(final int columnIndex) throws SQLException {
        return wrappedResultSet.getBinaryStream(columnIndex);
    }

    public String getString(final String columnLabel) throws SQLException {
        return wrappedResultSet.getString(columnLabel);
    }

    public boolean getBoolean(final String columnLabel) throws SQLException {
        return wrappedResultSet.getBoolean(columnLabel);
    }

    public byte getByte(final String columnLabel) throws SQLException {
        return wrappedResultSet.getByte(columnLabel);
    }

    public short getShort(final String columnLabel) throws SQLException {
        return wrappedResultSet.getShort(columnLabel);
    }

    public int getInt(final String columnLabel) throws SQLException {
        return wrappedResultSet.getInt(columnLabel);
    }

    public long getLong(final String columnLabel) throws SQLException {
        return wrappedResultSet.getLong(columnLabel);
    }

    public float getFloat(final String columnLabel) throws SQLException {
        return wrappedResultSet.getFloat(columnLabel);
    }

    public double getDouble(final String columnLabel) throws SQLException {
        return wrappedResultSet.getDouble(columnLabel);
    }

    @SuppressWarnings("deprecation")
    public BigDecimal getBigDecimal(final String columnLabel, final int scale) throws SQLException {
        return wrappedResultSet.getBigDecimal(columnLabel, scale);
    }

    public byte[] getBytes(final String columnLabel) throws SQLException {
        return wrappedResultSet.getBytes(columnLabel);
    }

    public Date getDate(final String columnLabel) throws SQLException {
        return wrappedResultSet.getDate(columnLabel);
    }

    public Time getTime(final String columnLabel) throws SQLException {
        return wrappedResultSet.getTime(columnLabel);
    }

    public Timestamp getTimestamp(final String columnLabel) throws SQLException {
        return wrappedResultSet.getTimestamp(columnLabel);
    }

    public InputStream getAsciiStream(final String columnLabel) throws SQLException {
        return wrappedResultSet.getAsciiStream(columnLabel);
    }

    @SuppressWarnings("deprecation")
    public InputStream getUnicodeStream(final String columnLabel) throws SQLException {
        return wrappedResultSet.getUnicodeStream(columnLabel);
    }

    public InputStream getBinaryStream(final String columnLabel) throws SQLException {
        return wrappedResultSet.getBinaryStream(columnLabel);
    }

    public SQLWarning getWarnings() throws SQLException {
        return wrappedResultSet.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        wrappedResultSet.clearWarnings();
    }

    public String getCursorName() throws SQLException {
        return wrappedResultSet.getCursorName();
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return wrappedResultSet.getMetaData();
    }

    public Object getObject(final int columnIndex) throws SQLException {
        return wrappedResultSet.getObject(columnIndex);
    }

    public Object getObject(final String columnLabel) throws SQLException {
        return wrappedResultSet.getObject(columnLabel);
    }

    public int findColumn(final String columnLabel) throws SQLException {
        return wrappedResultSet.findColumn(columnLabel);
    }

    public Reader getCharacterStream(final int columnIndex) throws SQLException {
        return wrappedResultSet.getCharacterStream(columnIndex);
    }

    public Reader getCharacterStream(final String columnLabel) throws SQLException {
        return wrappedResultSet.getCharacterStream(columnLabel);
    }

    public BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
        return wrappedResultSet.getBigDecimal(columnIndex);
    }

    public BigDecimal getBigDecimal(final String columnLabel) throws SQLException {
        return wrappedResultSet.getBigDecimal(columnLabel);
    }

    public boolean isBeforeFirst() throws SQLException {
        return wrappedResultSet.isBeforeFirst();
    }

    public boolean isAfterLast() throws SQLException {
        return wrappedResultSet.isAfterLast();
    }

    public boolean isFirst() throws SQLException {
        return wrappedResultSet.isFirst();
    }

    public boolean isLast() throws SQLException {
        return wrappedResultSet.isLast();
    }

    public void beforeFirst() throws SQLException {
        wrappedResultSet.beforeFirst();
    }

    public void afterLast() throws SQLException {
        wrappedResultSet.afterLast();
    }

    public boolean first() throws SQLException {
        return wrappedResultSet.first();
    }

    public boolean last() throws SQLException {
        return wrappedResultSet.last();
    }

    public int getRow() throws SQLException {
        return wrappedResultSet.getRow();
    }

    public boolean absolute(final int row) throws SQLException {
        return wrappedResultSet.absolute(row);
    }

    public boolean relative(final int rows) throws SQLException {
        return wrappedResultSet.relative(rows);
    }

    public boolean previous() throws SQLException {
        return wrappedResultSet.previous();
    }

    public void setFetchDirection(final int direction) throws SQLException {
        wrappedResultSet.setFetchDirection(direction);
    }

    public int getFetchDirection() throws SQLException {
        return wrappedResultSet.getFetchDirection();
    }

    public void setFetchSize(final int rows) throws SQLException {
        wrappedResultSet.setFetchSize(rows);
    }

    public int getFetchSize() throws SQLException {
        return wrappedResultSet.getFetchSize();
    }

    public int getType() throws SQLException {
        return wrappedResultSet.getType();
    }

    public int getConcurrency() throws SQLException {
        return wrappedResultSet.getConcurrency();
    }

    public boolean rowUpdated() throws SQLException {
        return wrappedResultSet.rowUpdated();
    }

    public boolean rowInserted() throws SQLException {
        return wrappedResultSet.rowInserted();
    }

    public boolean rowDeleted() throws SQLException {
        return wrappedResultSet.rowDeleted();
    }

    public void updateNull(final int columnIndex) throws SQLException {
        wrappedResultSet.updateNull(columnIndex);
    }

    public void updateBoolean(final int columnIndex, final boolean x) throws SQLException {
        wrappedResultSet.updateBoolean(columnIndex, x);
    }

    public void updateByte(final int columnIndex, final byte x) throws SQLException {
        wrappedResultSet.updateByte(columnIndex, x);
    }

    public void updateShort(final int columnIndex, final short x) throws SQLException {
        wrappedResultSet.updateShort(columnIndex, x);
    }

    public void updateInt(final int columnIndex, final int x) throws SQLException {
        wrappedResultSet.updateInt(columnIndex, x);
    }

    public void updateLong(final int columnIndex, final long x) throws SQLException {
        wrappedResultSet.updateLong(columnIndex, x);
    }

    public void updateFloat(final int columnIndex, final float x) throws SQLException {
        wrappedResultSet.updateFloat(columnIndex, x);
    }

    public void updateDouble(final int columnIndex, final double x) throws SQLException {
        wrappedResultSet.updateDouble(columnIndex, x);
    }

    public void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {
        wrappedResultSet.updateBigDecimal(columnIndex, x);
    }

    public void updateString(final int columnIndex, final String x) throws SQLException {
        wrappedResultSet.updateString(columnIndex, x);
    }

    public void updateBytes(final int columnIndex, final byte[] x) throws SQLException {
        wrappedResultSet.updateBytes(columnIndex, x);
    }

    public void updateDate(final int columnIndex, final Date x) throws SQLException {
        wrappedResultSet.updateDate(columnIndex, x);
    }

    public void updateTime(final int columnIndex, final Time x) throws SQLException {
        wrappedResultSet.updateTime(columnIndex, x);
    }

    public void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {
        wrappedResultSet.updateTimestamp(columnIndex, x);
    }

    public void updateAsciiStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        wrappedResultSet.updateAsciiStream(columnIndex, x, length);
    }

    public void updateBinaryStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        wrappedResultSet.updateBinaryStream(columnIndex, x, length);
    }

    public void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {
        wrappedResultSet.updateCharacterStream(columnIndex, x, length);
    }

    public void updateObject(final int columnIndex, final Object x, final int scaleOrLength) throws SQLException {
        wrappedResultSet.updateObject(columnIndex, x, scaleOrLength);
    }

    public void updateObject(final int columnIndex, final Object x) throws SQLException {
        wrappedResultSet.updateObject(columnIndex, x);
    }

    public void updateNull(final String columnLabel) throws SQLException {
        wrappedResultSet.updateNull(columnLabel);
    }

    public void updateBoolean(final String columnLabel, final boolean x) throws SQLException {
        wrappedResultSet.updateBoolean(columnLabel, x);
    }

    public void updateByte(final String columnLabel, final byte x) throws SQLException {
        wrappedResultSet.updateByte(columnLabel, x);
    }

    public void updateShort(final String columnLabel, final short x) throws SQLException {
        wrappedResultSet.updateShort(columnLabel, x);
    }

    public void updateInt(final String columnLabel, final int x) throws SQLException {
        wrappedResultSet.updateInt(columnLabel, x);
    }

    public void updateLong(final String columnLabel, final long x) throws SQLException {
        wrappedResultSet.updateLong(columnLabel, x);
    }

    public void updateFloat(final String columnLabel, final float x) throws SQLException {
        wrappedResultSet.updateFloat(columnLabel, x);
    }

    public void updateDouble(final String columnLabel, final double x) throws SQLException {
        wrappedResultSet.updateDouble(columnLabel, x);
    }

    public void updateBigDecimal(final String columnLabel, final BigDecimal x) throws SQLException {
        wrappedResultSet.updateBigDecimal(columnLabel, x);
    }

    public void updateString(final String columnLabel, final String x) throws SQLException {
        wrappedResultSet.updateString(columnLabel, x);
    }

    public void updateBytes(final String columnLabel, final byte[] x) throws SQLException {
        wrappedResultSet.updateBytes(columnLabel, x);
    }

    public void updateDate(final String columnLabel, final Date x) throws SQLException {
        wrappedResultSet.updateDate(columnLabel, x);
    }

    public void updateTime(final String columnLabel, final Time x) throws SQLException {
        wrappedResultSet.updateTime(columnLabel, x);
    }

    public void updateTimestamp(final String columnLabel, final Timestamp x) throws SQLException {
        wrappedResultSet.updateTimestamp(columnLabel, x);
    }

    public void updateAsciiStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
        wrappedResultSet.updateAsciiStream(columnLabel, x, length);
    }

    public void updateBinaryStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
        wrappedResultSet.updateBinaryStream(columnLabel, x, length);
    }

    public void updateCharacterStream(final String columnLabel, final Reader reader, final int length)
            throws SQLException {
        wrappedResultSet.updateCharacterStream(columnLabel, reader, length);
    }

    public void updateObject(final String columnLabel, final Object x, final int scaleOrLength) throws SQLException {
        wrappedResultSet.updateObject(columnLabel, x, scaleOrLength);
    }

    public void updateObject(final String columnLabel, final Object x) throws SQLException {
        wrappedResultSet.updateObject(columnLabel, x);
    }

    public void insertRow() throws SQLException {
        wrappedResultSet.insertRow();
    }

    public void updateRow() throws SQLException {
        wrappedResultSet.updateRow();
    }

    public void deleteRow() throws SQLException {
        wrappedResultSet.deleteRow();
    }

    public void refreshRow() throws SQLException {
        wrappedResultSet.refreshRow();
    }

    public void cancelRowUpdates() throws SQLException {
        wrappedResultSet.cancelRowUpdates();
    }

    public void moveToInsertRow() throws SQLException {
        wrappedResultSet.moveToInsertRow();
    }

    public void moveToCurrentRow() throws SQLException {
        wrappedResultSet.moveToCurrentRow();
    }

    public Statement getStatement() throws SQLException {
        return wrappedResultSet.getStatement();
    }

    public Object getObject(final int columnIndex, final Map<String, Class<?>> map) throws SQLException {
        return wrappedResultSet.getObject(columnIndex, map);
    }

    public Ref getRef(final int columnIndex) throws SQLException {
        return wrappedResultSet.getRef(columnIndex);
    }

    public Blob getBlob(final int columnIndex) throws SQLException {
        return wrappedResultSet.getBlob(columnIndex);
    }

    public Clob getClob(final int columnIndex) throws SQLException {
        return wrappedResultSet.getClob(columnIndex);
    }

    public Array getArray(final int columnIndex) throws SQLException {
        return wrappedResultSet.getArray(columnIndex);
    }

    public Object getObject(final String columnLabel, final Map<String, Class<?>> map) throws SQLException {
        return wrappedResultSet.getObject(columnLabel, map);
    }

    public Ref getRef(final String columnLabel) throws SQLException {
        return wrappedResultSet.getRef(columnLabel);
    }

    public Blob getBlob(final String columnLabel) throws SQLException {
        return wrappedResultSet.getBlob(columnLabel);
    }

    public Clob getClob(final String columnLabel) throws SQLException {
        return wrappedResultSet.getClob(columnLabel);
    }

    public Array getArray(final String columnLabel) throws SQLException {
        return wrappedResultSet.getArray(columnLabel);
    }

    public Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
        return wrappedResultSet.getDate(columnIndex, cal);
    }

    public Date getDate(final String columnLabel, final Calendar cal) throws SQLException {
        return wrappedResultSet.getDate(columnLabel, cal);
    }

    public Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
        return wrappedResultSet.getTime(columnIndex, cal);
    }

    public Time getTime(final String columnLabel, final Calendar cal) throws SQLException {
        return wrappedResultSet.getTime(columnLabel, cal);
    }

    public Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
        return wrappedResultSet.getTimestamp(columnIndex, cal);
    }

    public Timestamp getTimestamp(final String columnLabel, final Calendar cal) throws SQLException {
        return wrappedResultSet.getTimestamp(columnLabel, cal);
    }

    public URL getURL(final int columnIndex) throws SQLException {
        return wrappedResultSet.getURL(columnIndex);
    }

    public URL getURL(final String columnLabel) throws SQLException {
        return wrappedResultSet.getURL(columnLabel);
    }

    public void updateRef(final int columnIndex, final Ref x) throws SQLException {
        wrappedResultSet.updateRef(columnIndex, x);
    }

    public void updateRef(final String columnLabel, final Ref x) throws SQLException {
        wrappedResultSet.updateRef(columnLabel, x);
    }

    public void updateBlob(final int columnIndex, final Blob x) throws SQLException {
        wrappedResultSet.updateBlob(columnIndex, x);
    }

    public void updateBlob(final String columnLabel, final Blob x) throws SQLException {
        wrappedResultSet.updateBlob(columnLabel, x);
    }

    public void updateClob(final int columnIndex, final Clob x) throws SQLException {
        wrappedResultSet.updateClob(columnIndex, x);
    }

    public void updateClob(final String columnLabel, final Clob x) throws SQLException {
        wrappedResultSet.updateClob(columnLabel, x);
    }

    public void updateArray(final int columnIndex, final Array x) throws SQLException {
        wrappedResultSet.updateArray(columnIndex, x);
    }

    public void updateArray(final String columnLabel, final Array x) throws SQLException {
        wrappedResultSet.updateArray(columnLabel, x);
    }

    public RowId getRowId(final int columnIndex) throws SQLException {
        return wrappedResultSet.getRowId(columnIndex);
    }

    public RowId getRowId(final String columnLabel) throws SQLException {
        return wrappedResultSet.getRowId(columnLabel);
    }

    public void updateRowId(final int columnIndex, final RowId x) throws SQLException {
        wrappedResultSet.updateRowId(columnIndex, x);
    }

    public void updateRowId(final String columnLabel, final RowId x) throws SQLException {
        wrappedResultSet.updateRowId(columnLabel, x);
    }

    public int getHoldability() throws SQLException {
        return wrappedResultSet.getHoldability();
    }

    public boolean isClosed() throws SQLException {
        return wrappedResultSet.isClosed();
    }

    public void updateNString(final int columnIndex, final String nString) throws SQLException {
        wrappedResultSet.updateNString(columnIndex, nString);
    }

    public void updateNString(final String columnLabel, final String nString) throws SQLException {
        wrappedResultSet.updateNString(columnLabel, nString);
    }

    public void updateNClob(final int columnIndex, final NClob nClob) throws SQLException {
        wrappedResultSet.updateNClob(columnIndex, nClob);
    }

    public void updateNClob(final String columnLabel, final NClob nClob) throws SQLException {
        wrappedResultSet.updateNClob(columnLabel, nClob);
    }

    public NClob getNClob(final int columnIndex) throws SQLException {
        return wrappedResultSet.getNClob(columnIndex);
    }

    public NClob getNClob(final String columnLabel) throws SQLException {
        return wrappedResultSet.getNClob(columnLabel);
    }

    public SQLXML getSQLXML(final int columnIndex) throws SQLException {
        return wrappedResultSet.getSQLXML(columnIndex);
    }

    public SQLXML getSQLXML(final String columnLabel) throws SQLException {
        return wrappedResultSet.getSQLXML(columnLabel);
    }

    public void updateSQLXML(final int columnIndex, final SQLXML xmlObject) throws SQLException {
        wrappedResultSet.updateSQLXML(columnIndex, xmlObject);
    }

    public void updateSQLXML(final String columnLabel, final SQLXML xmlObject) throws SQLException {
        wrappedResultSet.updateSQLXML(columnLabel, xmlObject);
    }

    public String getNString(final int columnIndex) throws SQLException {
        return wrappedResultSet.getNString(columnIndex);
    }

    public String getNString(final String columnLabel) throws SQLException {
        return wrappedResultSet.getNString(columnLabel);
    }

    public Reader getNCharacterStream(final int columnIndex) throws SQLException {
        return wrappedResultSet.getNCharacterStream(columnIndex);
    }

    public Reader getNCharacterStream(final String columnLabel) throws SQLException {
        return wrappedResultSet.getNCharacterStream(columnLabel);
    }

    public void updateNCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
        wrappedResultSet.updateNCharacterStream(columnIndex, x, length);
    }

    public void updateNCharacterStream(final String columnLabel, final Reader reader, final long length)
            throws SQLException {
        wrappedResultSet.updateNCharacterStream(columnLabel, reader, length);
    }

    public void updateAsciiStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
        wrappedResultSet.updateAsciiStream(columnIndex, x, length);
    }

    public void updateBinaryStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
        wrappedResultSet.updateBinaryStream(columnIndex, x, length);
    }

    public void updateCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
        wrappedResultSet.updateCharacterStream(columnIndex, x, length);
    }

    public void updateAsciiStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
        wrappedResultSet.updateAsciiStream(columnLabel, x, length);
    }

    public void updateBinaryStream(final String columnLabel, final InputStream x, final long length)
            throws SQLException {
        wrappedResultSet.updateBinaryStream(columnLabel, x, length);
    }

    public void updateCharacterStream(final String columnLabel, final Reader reader, final long length)
            throws SQLException {
        wrappedResultSet.updateCharacterStream(columnLabel, reader, length);
    }

    public void updateBlob(final int columnIndex, final InputStream inputStream, final long length) throws SQLException {
        wrappedResultSet.updateBlob(columnIndex, inputStream, length);
    }

    public void updateBlob(final String columnLabel, final InputStream inputStream, final long length)
            throws SQLException {
        wrappedResultSet.updateBlob(columnLabel, inputStream, length);
    }

    public void updateClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        wrappedResultSet.updateClob(columnIndex, reader, length);
    }

    public void updateClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        wrappedResultSet.updateClob(columnLabel, reader, length);
    }

    public void updateNClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        wrappedResultSet.updateNClob(columnIndex, reader, length);
    }

    public void updateNClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        wrappedResultSet.updateNClob(columnLabel, reader, length);
    }

    public void updateNCharacterStream(final int columnIndex, final Reader x) throws SQLException {
        wrappedResultSet.updateNCharacterStream(columnIndex, x);
    }

    public void updateNCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        wrappedResultSet.updateNCharacterStream(columnLabel, reader);
    }

    public void updateAsciiStream(final int columnIndex, final InputStream x) throws SQLException {
        wrappedResultSet.updateAsciiStream(columnIndex, x);
    }

    public void updateBinaryStream(final int columnIndex, final InputStream x) throws SQLException {
        wrappedResultSet.updateBinaryStream(columnIndex, x);
    }

    public void updateCharacterStream(final int columnIndex, final Reader x) throws SQLException {
        wrappedResultSet.updateCharacterStream(columnIndex, x);
    }

    public void updateAsciiStream(final String columnLabel, final InputStream x) throws SQLException {
        wrappedResultSet.updateAsciiStream(columnLabel, x);
    }

    public void updateBinaryStream(final String columnLabel, final InputStream x) throws SQLException {
        wrappedResultSet.updateBinaryStream(columnLabel, x);
    }

    public void updateCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        wrappedResultSet.updateCharacterStream(columnLabel, reader);
    }

    public void updateBlob(final int columnIndex, final InputStream inputStream) throws SQLException {
        wrappedResultSet.updateBlob(columnIndex, inputStream);
    }

    public void updateBlob(final String columnLabel, final InputStream inputStream) throws SQLException {
        wrappedResultSet.updateBlob(columnLabel, inputStream);
    }

    public void updateClob(final int columnIndex, final Reader reader) throws SQLException {
        wrappedResultSet.updateClob(columnIndex, reader);
    }

    public void updateClob(final String columnLabel, final Reader reader) throws SQLException {
        wrappedResultSet.updateClob(columnLabel, reader);
    }

    public void updateNClob(final int columnIndex, final Reader reader) throws SQLException {
        wrappedResultSet.updateNClob(columnIndex, reader);
    }

    public void updateNClob(final String columnLabel, final Reader reader) throws SQLException {
        wrappedResultSet.updateNClob(columnLabel, reader);
    }
}
