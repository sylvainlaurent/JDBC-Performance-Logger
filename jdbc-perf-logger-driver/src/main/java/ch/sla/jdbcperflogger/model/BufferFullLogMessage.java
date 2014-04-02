package ch.sla.jdbcperflogger.model;

public class BufferFullLogMessage implements LogMessage {
    private static final long serialVersionUID = 1L;
    private final long timestamp;

    public BufferFullLogMessage(final long tstamp) {
        this.timestamp = tstamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
