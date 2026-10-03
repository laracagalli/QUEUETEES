package gui.components;

import java.awt.*;
import javax.swing.*;

/** Consistent field and action placement for customer and administrator profile editing. */
public final class ProfileEditorPanel extends JPanel {
    public ProfileEditorPanel(JTextField username, JButton choosePicture, JLabel pictureName,
                              JButton cancel, JButton changePassword, JButton save) {
        super(new BorderLayout(0, 18));
        setBackground(AppTheme.PAPER);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(label("Edit Profile", 15), BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(2, 1, 0, 16));
        fields.setOpaque(false);
        JPanel usernameRow = new JPanel(new BorderLayout(0, 6));
        usernameRow.setOpaque(false);
        usernameRow.add(label("Username", 11), BorderLayout.NORTH);
        username.setPreferredSize(new Dimension(100, 36));
        usernameRow.add(username);
        fields.add(usernameRow);

        JPanel pictureRow = new JPanel(new BorderLayout(0, 6));
        pictureRow.setOpaque(false);
        pictureRow.add(label("Profile Picture", 11), BorderLayout.NORTH);
        JPanel chooser = new JPanel(new BorderLayout(10, 0));
        chooser.setOpaque(false);
        choosePicture.setPreferredSize(new Dimension(150, 36));
        chooser.add(choosePicture, BorderLayout.WEST);
        pictureName.setHorizontalAlignment(SwingConstants.LEFT);
        pictureName.setVerticalAlignment(SwingConstants.CENTER);
        chooser.add(pictureName);
        pictureRow.add(chooser);
        fields.add(pictureRow);
        add(fields);

        JPanel actions = new JPanel(new BorderLayout(12, 0));
        actions.setOpaque(false);
        cancel.setPreferredSize(new Dimension(85, 36));
        changePassword.setPreferredSize(new Dimension(145, 36));
        save.setPreferredSize(new Dimension(125, 36));
        actions.add(cancel, BorderLayout.WEST);
        JPanel primaryActions = new JPanel(new GridLayout(1, 2, 8, 0));
        primaryActions.setOpaque(false);
        primaryActions.add(changePassword);
        primaryActions.add(save);
        actions.add(primaryActions, BorderLayout.EAST);
        add(actions, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(500, 265));
    }

    private static JLabel label(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, size));
        label.setForeground(AppTheme.GREEN);
        return label;
    }
}
