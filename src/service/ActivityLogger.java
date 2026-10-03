package service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ActivityLogger {
    public record Entry(LocalDateTime time, String username, String role, String action, String details) { }
    private static final List<Entry> entries = new ArrayList<>();
    private static model.User actor;
    public static synchronized void setActor(model.User user) { actor = user; }
    public static synchronized model.User getActor() { return actor; }
    public static synchronized void record(model.User user, String action, String details) {
        if (user == null || (user.getRole() != model.UserRole.ADMIN && user.getRole() != model.UserRole.STAFF)) return;
        Entry entry = new Entry(LocalDateTime.now(), user.getUsername(), user.getRole().name(), action, details);
        entries.add(0, entry);
        log(user.getUsername() + " | " + user.getEmail() + " | " + TIME_FORMAT.format(entry.time())
                + " | " + entry.role() + " | " + action + ": " + details);
    }
    public static synchronized void record(String action, String details) { record(actor, action, details); }
    public static synchronized List<Entry> getEntries() { return List.copyOf(entries); }

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
