package ch.sla.jdbcperflogger.logger;

import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.model.LogMessage;

public class RecordingLogSender implements LogSender {
    private static final int DEFAULT_RETAINED_LOG_MESSAGES_COUNT = 50;
    private final LinkedBlockingDeque<LogMessage> queue;

    public RecordingLogSender() {
        this(DEFAULT_RETAINED_LOG_MESSAGES_COUNT);
    }

    public RecordingLogSender(final int retainedLogMessagesCount) {
        queue = new LinkedBlockingDeque<LogMessage>(retainedLogMessagesCount);

    }

    @Override
    public void postLog(final LogMessage log) {
        while (!queue.offerFirst(log)) {
            queue.pollLast();
        }
    }

    public LogMessage[] getRecordedLogMessages() {
        return queue.toArray(new LogMessage[0]);
    }

    public void clearLogs() {
        queue.clear();
    }

    public @Nullable LogMessage lastLogMessage(final int index) {
        final LogMessage[] logs = getRecordedLogMessages();
        if (logs.length <= index) {
            return null;
        }
        return logs[index];
    }

}
