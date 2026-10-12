package service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ActivityLogger {
    public record Entry(LocalDateTime time, String username, String role, String action, String details, int userId) {
        public Entry(LocalDateTime time,String username,String role,String action,String details) { this(time,username,role,action,details,0); }
    }
    private static final List<Entry> entries = new ArrayList<>();
    private static model.User actor;
    private static repository.JdbcActivityRepository persistence;
    public static synchronized void useDatabase(database.Database database) { persistence = new repository.JdbcActivityRepository(database); }
    public static synchronized boolean isPersistent() { return persistence != null; }
    public static synchronized void setActor(model.User user) { actor = user; }
    public static synchronized model.User getActor() { return actor; }
    public static synchronized void record(model.User user, String action, String details) {
        if (user == null || (user.getRole() != model.UserRole.ADMIN && user.getRole() != model.UserRole.STAFF)) return;
        // Navigation and table controls are not account or operational changes.
        if (java.util.Set.of("UI command", "Search changed", "Table sorted", "Filter changed", "Date filter changed").contains(action)) return;
        Entry entry = new Entry(LocalDateTime.now(), user.getUsername(), user.getRole().name(), action, details, user.getId());
        if (persistence != null) persistence.record(user, entry);
        entries.add(0, entry);
        log(user.getUsername() + " | " + user.getEmail() + " | " + TIME_FORMAT.format(entry.time())
                + " | " + entry.role() + " | " + action + ": " + details);
    }
    public static synchronized void record(String action, String details) { record(actor, action, details); }
    public static synchronized List<Entry> getEntries() { return persistence == null ? List.copyOf(entries) : persistence.entries(); }

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
