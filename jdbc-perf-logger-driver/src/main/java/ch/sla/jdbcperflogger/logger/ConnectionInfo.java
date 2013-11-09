package ch.sla.jdbcperflogger.logger;

import java.util.Date;
import java.util.UUID;

import ch.sla.jdbcperflogger.model.LogMessage;

public class ConnectionInfo implements LogMessage {
    private static final long serialVersionUID = 1L;

    private final UUID uuid;
    private final int connectionNumber;
    private final String url;
    private final Date creationDate;

    public ConnectionInfo(final UUID uuid, final int connectionNumber, final String url, final Date creationDate) {
        this.uuid = uuid;
        this.connectionNumber = connectionNumber;
        this.url = url;
        this.creationDate = creationDate;
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
}
