package slaurent.jdbcperflogger;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
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

public class WrappingStatement {
    private final Statement wrappedStatement;
    private final PreparedStatement wrappedPreparedStatement;
    private final CallableStatement wrappedCallableStatement;

    public WrappingStatement(final Statement wrappedStatement) {
        this.wrappedStatement = wrappedStatement;
        if (wrappedStatement instanceof PreparedStatement) {
            wrappedPreparedStatement = (PreparedStatement) wrappedStatement;
        } else {
            wrappedPreparedStatement = null;
        }
        if (wrappedStatement instanceof CallableStatement) {
            wrappedCallableStatement = (CallableStatement) wrappedStatement;
        } else {
            wrappedCallableStatement = null;
        }
    }

    // ----------- DELEGATED METHODS -----------

    public ResultSet executeQuery(final String sql) throws SQLException {
        return wrappedStatement.executeQuery(sql);
    }

    public <T> T unwrap(final Class<T> iface) throws SQLException {
        return wrappedStatement.unwrap(iface);
    }

    public int executeUpdate(final String sql) throws SQLException {
        return wrappedStatement.executeUpdate(sql);
    }

    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return wrappedStatement.isWrapperFor(iface);
    }

    public void close() throws SQLException {
        wrappedStatement.close();
    }

    public int getMaxFieldSize() throws SQLException {
        return wrappedStatement.getMaxFieldSize();
    }

    public void setMaxFieldSize(final int max) throws SQLException {
        wrappedStatement.setMaxFieldSize(max);
    }

    public int getMaxRows() throws SQLException {
        return wrappedStatement.getMaxRows();
    }

    public void setMaxRows(final int max) throws SQLException {
        wrappedStatement.setMaxRows(max);
    }

    public void setEscapeProcessing(final boolean enable) throws SQLException {
        wrappedStatement.setEscapeProcessing(enable);
    }

    public int getQueryTimeout() throws SQLException {
        return wrappedStatement.getQueryTimeout();
    }

    public void setQueryTimeout(final int seconds) throws SQLException {
        wrappedStatement.setQueryTimeout(seconds);
    }

    public void cancel() throws SQLException {
        wrappedStatement.cancel();
    }

    public SQLWarning getWarnings() throws SQLException {
        return wrappedStatement.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        wrappedStatement.clearWarnings();
    }

    public void setCursorName(final String name) throws SQLException {
        wrappedStatement.setCursorName(name);
    }

    public boolean execute(final String sql) throws SQLException {
        return wrappedStatement.execute(sql);
    }

    public ResultSet getResultSet() throws SQLException {
        return wrappedStatement.getResultSet();
    }

    public int getUpdateCount() throws SQLException {
        return wrappedStatement.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        return wrappedStatement.getMoreResults();
    }

    public void setFetchDirection(final int direction) throws SQLException {
        wrappedStatement.setFetchDirection(direction);
    }

    public int getFetchDirection() throws SQLException {
        return wrappedStatement.getFetchDirection();
    }

    public void setFetchSize(final int rows) throws SQLException {
        wrappedStatement.setFetchSize(rows);
    }

    public int getFetchSize() throws SQLException {
        return wrappedStatement.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return wrappedStatement.getResultSetConcurrency();
    }

    public int getResultSetType() throws SQLException {
        return wrappedStatement.getResultSetType();
    }

    public void addBatch(final String sql) throws SQLException {
        wrappedStatement.addBatch(sql);
    }

    public void clearBatch() throws SQLException {
        wrappedStatement.clearBatch();
    }

    public int[] executeBatch() throws SQLException {
        return executeBatch_internal();
    }

    protected final int[] executeBatch_internal() throws SQLException {
        return wrappedStatement.executeBatch();
    }

    public Connection getConnection() throws SQLException {
        return wrappedStatement.getConnection();
    }

    public boolean getMoreResults(final int current) throws SQLException {
        return wrappedStatement.getMoreResults(current);
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return wrappedStatement.getGeneratedKeys();
    }

    public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
        return wrappedStatement.executeUpdate(sql, autoGeneratedKeys);
    }

    public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
        return wrappedStatement.executeUpdate(sql, columnIndexes);
    }

    public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
        return wrappedStatement.executeUpdate(sql, columnNames);
    }

    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
        return wrappedStatement.execute(sql, autoGeneratedKeys);
    }

    public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
        return wrappedStatement.execute(sql, columnIndexes);
    }

    public boolean execute(final String sql, final String[] columnNames) throws SQLException {
        return wrappedStatement.execute(sql, columnNames);
    }

    public int getResultSetHoldability() throws SQLException {
        return wrappedStatement.getResultSetHoldability();
    }

    public boolean isClosed() throws SQLException {
        return wrappedStatement.isClosed();
    }

    public void setPoolable(final boolean poolable) throws SQLException {
        wrappedStatement.setPoolable(poolable);
    }

    public boolean isPoolable() throws SQLException {
        return wrappedStatement.isPoolable();
    }

    // ------------- DELEGATES for PreparedStatement -----------

    public ResultSet executeQuery() throws SQLException {
        return wrappedPreparedStatement.executeQuery();
    }

    public int executeUpdate() throws SQLException {
        return wrappedPreparedStatement.executeUpdate();
    }

    public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
        wrappedPreparedStatement.setNull(parameterIndex, sqlType);
    }

    public void setBoolean(final int parameterIndex, final boolean x) throws SQLException {
        wrappedPreparedStatement.setBoolean(parameterIndex, x);
    }

    public void setByte(final int parameterIndex, final byte x) throws SQLException {
        wrappedPreparedStatement.setByte(parameterIndex, x);
    }

    public void setShort(final int parameterIndex, final short x) throws SQLException {
        wrappedPreparedStatement.setShort(parameterIndex, x);
    }

    public void setInt(final int parameterIndex, final int x) throws SQLException {
        wrappedPreparedStatement.setInt(parameterIndex, x);
    }

    public void setLong(final int parameterIndex, final long x) throws SQLException {
        wrappedPreparedStatement.setLong(parameterIndex, x);
    }

    public void setFloat(final int parameterIndex, final float x) throws SQLException {
        wrappedPreparedStatement.setFloat(parameterIndex, x);
    }

    public void setDouble(final int parameterIndex, final double x) throws SQLException {
        wrappedPreparedStatement.setDouble(parameterIndex, x);
    }

    public void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
        wrappedPreparedStatement.setBigDecimal(parameterIndex, x);
    }

    public void setString(final int parameterIndex, final String x) throws SQLException {
        wrappedPreparedStatement.setString(parameterIndex, x);
    }

    public void setBytes(final int parameterIndex, final byte[] x) throws SQLException {
        wrappedPreparedStatement.setBytes(parameterIndex, x);
    }

    public void setDate(final int parameterIndex, final Date x) throws SQLException {
        wrappedPreparedStatement.setDate(parameterIndex, x);
    }

    public void setTime(final int parameterIndex, final Time x) throws SQLException {
        wrappedPreparedStatement.setTime(parameterIndex, x);
    }

    public void setTimestamp(final int parameterIndex, final Timestamp x) throws SQLException {
        wrappedPreparedStatement.setTimestamp(parameterIndex, x);
    }

    public void setAsciiStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        wrappedPreparedStatement.setAsciiStream(parameterIndex, x, length);
    }

    @SuppressWarnings("deprecation")
    public void setUnicodeStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        wrappedPreparedStatement.setUnicodeStream(parameterIndex, x, length);
    }

    public void setBinaryStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        wrappedPreparedStatement.setBinaryStream(parameterIndex, x, length);
    }

    public void clearParameters() throws SQLException {
        wrappedPreparedStatement.clearParameters();
    }

    public void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
        wrappedPreparedStatement.setObject(parameterIndex, x, targetSqlType);
    }

    public void setObject(final int parameterIndex, final Object x) throws SQLException {
        wrappedPreparedStatement.setObject(parameterIndex, x);
    }

    public boolean execute() throws SQLException {
        return wrappedPreparedStatement.execute();
    }

    public void addBatch() throws SQLException {
        wrappedPreparedStatement.addBatch();
    }

    public void setCharacterStream(final int parameterIndex, final Reader reader, final int length) throws SQLException {
        wrappedPreparedStatement.setCharacterStream(parameterIndex, reader, length);
    }

    public void setRef(final int parameterIndex, final Ref x) throws SQLException {
        wrappedPreparedStatement.setRef(parameterIndex, x);
    }

    public void setBlob(final int parameterIndex, final Blob x) throws SQLException {
        wrappedPreparedStatement.setBlob(parameterIndex, x);
    }

    public void setClob(final int parameterIndex, final Clob x) throws SQLException {
        wrappedPreparedStatement.setClob(parameterIndex, x);
    }

    public void setArray(final int parameterIndex, final Array x) throws SQLException {
        wrappedPreparedStatement.setArray(parameterIndex, x);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return wrappedPreparedStatement.getMetaData();
    }

    public void setDate(final int parameterIndex, final Date x, final Calendar cal) throws SQLException {
        wrappedPreparedStatement.setDate(parameterIndex, x, cal);
    }

    public void setTime(final int parameterIndex, final Time x, final Calendar cal) throws SQLException {
        wrappedPreparedStatement.setTime(parameterIndex, x, cal);
    }

    public void setTimestamp(final int parameterIndex, final Timestamp x, final Calendar cal) throws SQLException {
        wrappedPreparedStatement.setTimestamp(parameterIndex, x, cal);
    }

    public void setNull(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
        wrappedPreparedStatement.setNull(parameterIndex, sqlType, typeName);
    }

    public void setURL(final int parameterIndex, final URL x) throws SQLException {
        wrappedPreparedStatement.setURL(parameterIndex, x);
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return wrappedPreparedStatement.getParameterMetaData();
    }

    public void setRowId(final int parameterIndex, final RowId x) throws SQLException {
        wrappedPreparedStatement.setRowId(parameterIndex, x);
    }

    public void setNString(final int parameterIndex, final String value) throws SQLException {
        wrappedPreparedStatement.setNString(parameterIndex, value);
    }

    public void setNCharacterStream(final int parameterIndex, final Reader value, final long length)
            throws SQLException {
        wrappedPreparedStatement.setNCharacterStream(parameterIndex, value, length);
    }

    public void setNClob(final int parameterIndex, final NClob value) throws SQLException {
        wrappedPreparedStatement.setNClob(parameterIndex, value);
    }

    public void setClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        wrappedPreparedStatement.setClob(parameterIndex, reader, length);
    }

    public void setBlob(final int parameterIndex, final InputStream inputStream, final long length) throws SQLException {
        wrappedPreparedStatement.setBlob(parameterIndex, inputStream, length);
    }

    public void setNClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        wrappedPreparedStatement.setNClob(parameterIndex, reader, length);
    }

    public void setSQLXML(final int parameterIndex, final SQLXML xmlObject) throws SQLException {
        wrappedPreparedStatement.setSQLXML(parameterIndex, xmlObject);
    }

    public void setObject(final int parameterIndex, final Object x, final int targetSqlType, final int scaleOrLength)
            throws SQLException {
        wrappedPreparedStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    public void setAsciiStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
        wrappedPreparedStatement.setAsciiStream(parameterIndex, x, length);
    }

    public void setBinaryStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
        wrappedPreparedStatement.setBinaryStream(parameterIndex, x, length);
    }

    public void setCharacterStream(final int parameterIndex, final Reader reader, final long length)
            throws SQLException {
        wrappedPreparedStatement.setCharacterStream(parameterIndex, reader, length);
    }

    public void setAsciiStream(final int parameterIndex, final InputStream x) throws SQLException {
        wrappedPreparedStatement.setAsciiStream(parameterIndex, x);
    }

    public void setBinaryStream(final int parameterIndex, final InputStream x) throws SQLException {
        wrappedPreparedStatement.setBinaryStream(parameterIndex, x);
    }

    public void setCharacterStream(final int parameterIndex, final Reader reader) throws SQLException {
        wrappedPreparedStatement.setCharacterStream(parameterIndex, reader);
    }

    public void setNCharacterStream(final int parameterIndex, final Reader value) throws SQLException {
        wrappedPreparedStatement.setNCharacterStream(parameterIndex, value);
    }

    public void setClob(final int parameterIndex, final Reader reader) throws SQLException {
        wrappedPreparedStatement.setClob(parameterIndex, reader);
    }

    public void setBlob(final int parameterIndex, final InputStream inputStream) throws SQLException {
        wrappedPreparedStatement.setBlob(parameterIndex, inputStream);
    }

    public void setNClob(final int parameterIndex, final Reader reader) throws SQLException {
        wrappedPreparedStatement.setNClob(parameterIndex, reader);
    }

    // ------------- DELEGATES for CallableStatement -----------

    public void registerOutParameter(final int parameterIndex, final int sqlType) throws SQLException {
        wrappedCallableStatement.registerOutParameter(parameterIndex, sqlType);
    }

    public void registerOutParameter(final int parameterIndex, final int sqlType, final int scale) throws SQLException {
        wrappedCallableStatement.registerOutParameter(parameterIndex, sqlType, scale);
    }

    public boolean wasNull() throws SQLException {
        return wrappedCallableStatement.wasNull();
    }

    public String getString(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getString(parameterIndex);
    }

    public boolean getBoolean(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getBoolean(parameterIndex);
    }

    public byte getByte(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getByte(parameterIndex);
    }

    public short getShort(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getShort(parameterIndex);
    }

    public int getInt(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getInt(parameterIndex);
    }

    public long getLong(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getLong(parameterIndex);
    }

    public float getFloat(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getFloat(parameterIndex);
    }

    public double getDouble(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getDouble(parameterIndex);
    }

    @SuppressWarnings("deprecation")
    public BigDecimal getBigDecimal(final int parameterIndex, final int scale) throws SQLException {
        return wrappedCallableStatement.getBigDecimal(parameterIndex, scale);
    }

    public byte[] getBytes(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getBytes(parameterIndex);
    }

    public Date getDate(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getDate(parameterIndex);
    }

    public Time getTime(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getTime(parameterIndex);
    }

    public Timestamp getTimestamp(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getTimestamp(parameterIndex);
    }

    public Object getObject(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getObject(parameterIndex);
    }

    public BigDecimal getBigDecimal(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getBigDecimal(parameterIndex);
    }

    public Object getObject(final int parameterIndex, final Map<String, Class<?>> map) throws SQLException {
        return wrappedCallableStatement.getObject(parameterIndex, map);
    }

    public Ref getRef(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getRef(parameterIndex);
    }

    public Blob getBlob(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getBlob(parameterIndex);
    }

    public Clob getClob(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getClob(parameterIndex);
    }

    public Array getArray(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getArray(parameterIndex);
    }

    public Date getDate(final int parameterIndex, final Calendar cal) throws SQLException {
        return wrappedCallableStatement.getDate(parameterIndex, cal);
    }

    public Time getTime(final int parameterIndex, final Calendar cal) throws SQLException {
        return wrappedCallableStatement.getTime(parameterIndex, cal);
    }

    public Timestamp getTimestamp(final int parameterIndex, final Calendar cal) throws SQLException {
        return wrappedCallableStatement.getTimestamp(parameterIndex, cal);
    }

    public void registerOutParameter(final int parameterIndex, final int sqlType, final String typeName)
            throws SQLException {
        wrappedCallableStatement.registerOutParameter(parameterIndex, sqlType, typeName);
    }

    public void registerOutParameter(final String parameterName, final int sqlType) throws SQLException {
        wrappedCallableStatement.registerOutParameter(parameterName, sqlType);
    }

    public void registerOutParameter(final String parameterName, final int sqlType, final int scale)
            throws SQLException {
        wrappedCallableStatement.registerOutParameter(parameterName, sqlType, scale);
    }

    public void registerOutParameter(final String parameterName, final int sqlType, final String typeName)
            throws SQLException {
        wrappedCallableStatement.registerOutParameter(parameterName, sqlType, typeName);
    }

    public URL getURL(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getURL(parameterIndex);
    }

    public void setURL(final String parameterName, final URL val) throws SQLException {
        wrappedCallableStatement.setURL(parameterName, val);
    }

    public void setNull(final String parameterName, final int sqlType) throws SQLException {
        wrappedCallableStatement.setNull(parameterName, sqlType);
    }

    public void setBoolean(final String parameterName, final boolean x) throws SQLException {
        wrappedCallableStatement.setBoolean(parameterName, x);
    }

    public void setByte(final String parameterName, final byte x) throws SQLException {
        wrappedCallableStatement.setByte(parameterName, x);
    }

    public void setShort(final String parameterName, final short x) throws SQLException {
        wrappedCallableStatement.setShort(parameterName, x);
    }

    public void setInt(final String parameterName, final int x) throws SQLException {
        wrappedCallableStatement.setInt(parameterName, x);
    }

    public void setLong(final String parameterName, final long x) throws SQLException {
        wrappedCallableStatement.setLong(parameterName, x);
    }

    public void setFloat(final String parameterName, final float x) throws SQLException {
        wrappedCallableStatement.setFloat(parameterName, x);
    }

    public void setDouble(final String parameterName, final double x) throws SQLException {
        wrappedCallableStatement.setDouble(parameterName, x);
    }

    public void setBigDecimal(final String parameterName, final BigDecimal x) throws SQLException {
        wrappedCallableStatement.setBigDecimal(parameterName, x);
    }

    public void setString(final String parameterName, final String x) throws SQLException {
        wrappedCallableStatement.setString(parameterName, x);
    }

    public void setBytes(final String parameterName, final byte[] x) throws SQLException {
        wrappedCallableStatement.setBytes(parameterName, x);
    }

    public void setDate(final String parameterName, final Date x) throws SQLException {
        wrappedCallableStatement.setDate(parameterName, x);
    }

    public void setTime(final String parameterName, final Time x) throws SQLException {
        wrappedCallableStatement.setTime(parameterName, x);
    }

    public void setTimestamp(final String parameterName, final Timestamp x) throws SQLException {
        wrappedCallableStatement.setTimestamp(parameterName, x);
    }

    public void setAsciiStream(final String parameterName, final InputStream x, final int length) throws SQLException {
        wrappedCallableStatement.setAsciiStream(parameterName, x, length);
    }

    public void setBinaryStream(final String parameterName, final InputStream x, final int length) throws SQLException {
        wrappedCallableStatement.setBinaryStream(parameterName, x, length);
    }

    public void setObject(final String parameterName, final Object x, final int targetSqlType, final int scale)
            throws SQLException {
        wrappedCallableStatement.setObject(parameterName, x, targetSqlType, scale);
    }

    public void setObject(final String parameterName, final Object x, final int targetSqlType) throws SQLException {
        wrappedCallableStatement.setObject(parameterName, x, targetSqlType);
    }

    public void setObject(final String parameterName, final Object x) throws SQLException {
        wrappedCallableStatement.setObject(parameterName, x);
    }

    public void setCharacterStream(final String parameterName, final Reader reader, final int length)
            throws SQLException {
        wrappedCallableStatement.setCharacterStream(parameterName, reader, length);
    }

    public void setDate(final String parameterName, final Date x, final Calendar cal) throws SQLException {
        wrappedCallableStatement.setDate(parameterName, x, cal);
    }

    public void setTime(final String parameterName, final Time x, final Calendar cal) throws SQLException {
        wrappedCallableStatement.setTime(parameterName, x, cal);
    }

    public void setTimestamp(final String parameterName, final Timestamp x, final Calendar cal) throws SQLException {
        wrappedCallableStatement.setTimestamp(parameterName, x, cal);
    }

    public void setNull(final String parameterName, final int sqlType, final String typeName) throws SQLException {
        wrappedCallableStatement.setNull(parameterName, sqlType, typeName);
    }

    public String getString(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getString(parameterName);
    }

    public boolean getBoolean(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getBoolean(parameterName);
    }

    public byte getByte(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getByte(parameterName);
    }

    public short getShort(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getShort(parameterName);
    }

    public int getInt(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getInt(parameterName);
    }

    public long getLong(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getLong(parameterName);
    }

    public float getFloat(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getFloat(parameterName);
    }

    public double getDouble(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getDouble(parameterName);
    }

    public byte[] getBytes(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getBytes(parameterName);
    }

    public Date getDate(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getDate(parameterName);
    }

    public Time getTime(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getTime(parameterName);
    }

    public Timestamp getTimestamp(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getTimestamp(parameterName);
    }

    public Object getObject(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getObject(parameterName);
    }

    public BigDecimal getBigDecimal(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getBigDecimal(parameterName);
    }

    public Object getObject(final String parameterName, final Map<String, Class<?>> map) throws SQLException {
        return wrappedCallableStatement.getObject(parameterName, map);
    }

    public Ref getRef(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getRef(parameterName);
    }

    public Blob getBlob(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getBlob(parameterName);
    }

    public Clob getClob(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getClob(parameterName);
    }

    public Array getArray(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getArray(parameterName);
    }

    public Date getDate(final String parameterName, final Calendar cal) throws SQLException {
        return wrappedCallableStatement.getDate(parameterName, cal);
    }

    public Time getTime(final String parameterName, final Calendar cal) throws SQLException {
        return wrappedCallableStatement.getTime(parameterName, cal);
    }

    public Timestamp getTimestamp(final String parameterName, final Calendar cal) throws SQLException {
        return wrappedCallableStatement.getTimestamp(parameterName, cal);
    }

    public URL getURL(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getURL(parameterName);
    }

    public RowId getRowId(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getRowId(parameterIndex);
    }

    public RowId getRowId(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getRowId(parameterName);
    }

    public void setRowId(final String parameterName, final RowId x) throws SQLException {
        wrappedCallableStatement.setRowId(parameterName, x);
    }

    public void setNString(final String parameterName, final String value) throws SQLException {
        wrappedCallableStatement.setNString(parameterName, value);
    }

    public void setNCharacterStream(final String parameterName, final Reader value, final long length)
            throws SQLException {
        wrappedCallableStatement.setNCharacterStream(parameterName, value, length);
    }

    public void setNClob(final String parameterName, final NClob value) throws SQLException {
        wrappedCallableStatement.setNClob(parameterName, value);
    }

    public void setClob(final String parameterName, final Reader reader, final long length) throws SQLException {
        wrappedCallableStatement.setClob(parameterName, reader, length);
    }

    public void setBlob(final String parameterName, final InputStream inputStream, final long length)
            throws SQLException {
        wrappedCallableStatement.setBlob(parameterName, inputStream, length);
    }

    public void setNClob(final String parameterName, final Reader reader, final long length) throws SQLException {
        wrappedCallableStatement.setNClob(parameterName, reader, length);
    }

    public NClob getNClob(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getNClob(parameterIndex);
    }

    public NClob getNClob(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getNClob(parameterName);
    }

    public void setSQLXML(final String parameterName, final SQLXML xmlObject) throws SQLException {
        wrappedCallableStatement.setSQLXML(parameterName, xmlObject);
    }

    public SQLXML getSQLXML(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getSQLXML(parameterIndex);
    }

    public SQLXML getSQLXML(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getSQLXML(parameterName);
    }

    public String getNString(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getNString(parameterIndex);
    }

    public String getNString(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getNString(parameterName);
    }

    public Reader getNCharacterStream(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getNCharacterStream(parameterIndex);
    }

    public Reader getNCharacterStream(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getNCharacterStream(parameterName);
    }

    public Reader getCharacterStream(final int parameterIndex) throws SQLException {
        return wrappedCallableStatement.getCharacterStream(parameterIndex);
    }

    public Reader getCharacterStream(final String parameterName) throws SQLException {
        return wrappedCallableStatement.getCharacterStream(parameterName);
    }

    public void setBlob(final String parameterName, final Blob x) throws SQLException {
        wrappedCallableStatement.setBlob(parameterName, x);
    }

    public void setClob(final String parameterName, final Clob x) throws SQLException {
        wrappedCallableStatement.setClob(parameterName, x);
    }

    public void setAsciiStream(final String parameterName, final InputStream x, final long length) throws SQLException {
        wrappedCallableStatement.setAsciiStream(parameterName, x, length);
    }

    public void setBinaryStream(final String parameterName, final InputStream x, final long length) throws SQLException {
        wrappedCallableStatement.setBinaryStream(parameterName, x, length);
    }

    public void setCharacterStream(final String parameterName, final Reader reader, final long length)
            throws SQLException {
        wrappedCallableStatement.setCharacterStream(parameterName, reader, length);
    }

    public void setAsciiStream(final String parameterName, final InputStream x) throws SQLException {
        wrappedCallableStatement.setAsciiStream(parameterName, x);
    }

    public void setBinaryStream(final String parameterName, final InputStream x) throws SQLException {
        wrappedCallableStatement.setBinaryStream(parameterName, x);
    }

    public void setCharacterStream(final String parameterName, final Reader reader) throws SQLException {
        wrappedCallableStatement.setCharacterStream(parameterName, reader);
    }

    public void setNCharacterStream(final String parameterName, final Reader value) throws SQLException {
        wrappedCallableStatement.setNCharacterStream(parameterName, value);
    }

    public void setClob(final String parameterName, final Reader reader) throws SQLException {
        wrappedCallableStatement.setClob(parameterName, reader);
    }

    public void setBlob(final String parameterName, final InputStream inputStream) throws SQLException {
        wrappedCallableStatement.setBlob(parameterName, inputStream);
    }

    public void setNClob(final String parameterName, final Reader reader) throws SQLException {
        wrappedCallableStatement.setNClob(parameterName, reader);
    }

}
