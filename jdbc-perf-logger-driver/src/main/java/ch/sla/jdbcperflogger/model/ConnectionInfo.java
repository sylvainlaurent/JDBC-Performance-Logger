package ch.sla.jdbcperflogger.model;

import java.util.Date;
import java.util.Properties;
import java.util.UUID;

public class ConnectionInfo implements LogMessage {
    private static final long serialVersionUID = 1L;

    private final UUID uuid;
    private final int connectionNumber;
    private final String url;
    private final Date creationDate;
    /**
     * Connection props without password
     */
    private final Properties connectionProperties;

    public ConnectionInfo(final UUID uuid, final int connectionNumber, final String url, final Date creationDate,
            final Properties connectionProperties) {
        this.uuid = uuid;
        this.connectionNumber = connectionNumber;
        this.url = url;
        this.creationDate = creationDate;
        this.connectionProperties = connectionProperties;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getConnectionNumber() {
        return connectionNumber;
    }

    public String getUrl() {
        return url;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Properties getConnectionProperties() {
        return connectionProperties;
    }

}
