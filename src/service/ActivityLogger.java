package service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ActivityLogger {

    private static final int MAX_LOGS = 1000;

    private static final LinkedList<String> logs = new LinkedList<>();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a");

    public static synchronized void log(String message) {
        String timestamp = TIME_FORMAT.format(LocalDateTime.now());
        logs.addFirst("[" + timestamp + "] " + message);

        if (logs.size() > MAX_LOGS) {
            logs.removeLast();
        }
    }

    public static synchronized List<String> getLogs() {

        return Collections.unmodifiableList(new ArrayList<>(logs));
    }
}