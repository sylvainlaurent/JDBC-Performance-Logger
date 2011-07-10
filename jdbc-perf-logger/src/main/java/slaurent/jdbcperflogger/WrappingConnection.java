package slaurent.jdbcperflogger;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WrappingConnection implements Connection {
    private final static Logger LOGGER = LoggerFactory.getLogger(WrappingConnection.class);

    private final Connection wrappedConnection;
    final DatabaseType databaseType;

    public WrappingConnection(final Connection wrappedConnection) {
        this.wrappedConnection = wrappedConnection;
        databaseType = getDatabaseType(wrappedConnection);
    }

    static DatabaseType getDatabaseType(final Connection connection) {
        String dbProduct;
        try {
            dbProduct = connection.getMetaData().getDatabaseProductName();
        } catch (final SQLException e) {
            LOGGER.error("cannot get db product name");
            return DatabaseType.UNKNOWN;
        }
        if ("Oracle".equals(dbProduct)) {
            return DatabaseType.ORACLE;
        }
        return DatabaseType.UNKNOWN;
    }

    // --------- DELEGATED METHODS -----------

    public void clearWarnings() throws SQLException {
        wrappedConnection.clearWarnings();
    }

    public void close() throws SQLException {
        wrappedConnection.close();
    }

    public void commit() throws SQLException {
        wrappedConnection.commit();
    }

    public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
        return wrappedConnection.createArrayOf(typeName, elements);
    }

    public Blob createBlob() throws SQLException {
        return wrappedConnection.createBlob();
    }

    public Clob createClob() throws SQLException {
        return wrappedConnection.createClob();
    }

    public NClob createNClob() throws SQLException {
        return wrappedConnection.createNClob();
    }

    public SQLXML createSQLXML() throws SQLException {
        return wrappedConnection.createSQLXML();
    }

    public Statement createStatement() throws SQLException {
        return wrappedConnection.createStatement();
    }

    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
        return wrappedConnection.createStatement(resultSetType, resultSetConcurrency);
    }

    public Statement createStatement(final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        return wrappedConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
        return wrappedConnection.createStruct(typeName, attributes);
    }

    public boolean getAutoCommit() throws SQLException {
        return wrappedConnection.getAutoCommit();
    }

    public String getCatalog() throws SQLException {
        return wrappedConnection.getCatalog();
    }

    public Properties getClientInfo() throws SQLException {
        return wrappedConnection.getClientInfo();
    }

    public String getClientInfo(final String name) throws SQLException {
        return wrappedConnection.getClientInfo(name);
    }

    public int getHoldability() throws SQLException {
        return wrappedConnection.getHoldability();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return wrappedConnection.getMetaData();
    }

    public int getTransactionIsolation() throws SQLException {
        return wrappedConnection.getTransactionIsolation();
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return wrappedConnection.getTypeMap();
    }

    public SQLWarning getWarnings() throws SQLException {
        return wrappedConnection.getWarnings();
    }

    public boolean isClosed() throws SQLException {
        return wrappedConnection.isClosed();
    }

    public boolean isReadOnly() throws SQLException {
        return wrappedConnection.isReadOnly();
    }

    public boolean isValid(final int timeout) throws SQLException {
        return wrappedConnection.isValid(timeout);
    }

    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return wrappedConnection.isWrapperFor(iface);
    }

    public String nativeSQL(final String sql) throws SQLException {
        return wrappedConnection.nativeSQL(sql);
    }

    public CallableStatement prepareCall(final String sql) throws SQLException {
        return wrappedConnection.prepareCall(sql);
    }

    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency)
            throws SQLException {
        return wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        return wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        return wrappedConnection.prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
        return wrappedConnection.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency)
            throws SQLException {
        return wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(final String sql, final int resultSetType,
            final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
        return wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
        return wrappedConnection.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
        return wrappedConnection.prepareStatement(sql, columnNames);
    }

    public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        wrappedConnection.releaseSavepoint(savepoint);
    }

    public void rollback() throws SQLException {
        wrappedConnection.rollback();
    }

    public void rollback(final Savepoint savepoint) throws SQLException {
        wrappedConnection.rollback(savepoint);
    }

    public void setAutoCommit(final boolean autoCommit) throws SQLException {
        wrappedConnection.setAutoCommit(autoCommit);
    }

    public void setCatalog(final String catalog) throws SQLException {
        wrappedConnection.setCatalog(catalog);
    }

    public void setClientInfo(final Properties properties) throws SQLClientInfoException {
        wrappedConnection.setClientInfo(properties);
    }

    public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
        wrappedConnection.setClientInfo(name, value);
    }

    public void setHoldability(final int holdability) throws SQLException {
        wrappedConnection.setHoldability(holdability);
    }

    public void setReadOnly(final boolean readOnly) throws SQLException {
        wrappedConnection.setReadOnly(readOnly);
    }

    public Savepoint setSavepoint() throws SQLException {
        return wrappedConnection.setSavepoint();
    }

    public Savepoint setSavepoint(final String name) throws SQLException {
        return wrappedConnection.setSavepoint(name);
    }

    public void setTransactionIsolation(final int level) throws SQLException {
        wrappedConnection.setTransactionIsolation(level);
    }

    public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
        wrappedConnection.setTypeMap(map);
    }

    public <T> T unwrap(final Class<T> iface) throws SQLException {
        return wrappedConnection.unwrap(iface);
    }
}
