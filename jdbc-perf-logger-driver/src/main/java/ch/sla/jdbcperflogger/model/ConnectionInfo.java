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
    private final long connectionCreationDuration;
    /**
     * Connection props without password
     */
    private final Properties connectionProperties;

    public ConnectionInfo(final UUID uuid, final int connectionNumber, final String url, final Date creationDate,
            final long connectionCreationDuration, final Properties connectionProperties) {
        this.uuid = uuid;
        this.connectionNumber = connectionNumber;
        this.url = url;
        this.creationDate = creationDate;
        this.connectionCreationDuration = connectionCreationDuration;
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

    public long getConnectionCreationDuration() {
        return connectionCreationDuration;
    }

    @Override
    public String toString() {
        return "ConnectionInfo["//
                + "connectionId=" + uuid//
                + ", connectionNumber=" + connectionNumber//
                + ", url=" + url//
                + ", creationDate=" + creationDate//
                + ", connectionCreationDuration=" + connectionCreationDuration//
                + "]";
    }
}
