package ch.sla.jdbcperflogger.console.ui;

import org.eclipse.jdt.annotation.Nullable;

public class HostPort implements Comparable<HostPort> {
    private final String host;
    private final int port;

    public HostPort(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public int compareTo( final HostPort o) {
        int cmp = host.compareToIgnoreCase(o.host);
        if (cmp != 0) {
            return cmp;
        }
        cmp = port - o.port;
        return cmp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + host.hashCode();
        result = prime * result + port;
        return result;
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HostPort other = (HostPort) obj;
        return compareTo(other) == 0;
    }

}
