package gui.components;

import javax.swing.JComponent;
import model.User;
import service.ActivityLogger;

/** Sets the dashboard actor for explicit account and operational activity records. */
public final class ActivityTracking {
    private ActivityTracking() { }
    public static void track(JComponent root, User user) {
        root.putClientProperty("activity.actor", user);
        ActivityLogger.setActor(user);
    }
}