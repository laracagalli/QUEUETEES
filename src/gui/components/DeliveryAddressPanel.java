package gui.components;
import java.awt.*;
import javax.swing.*;
import model.*;

/** Offline cascading address selection for checkout and staff address review. */
public final class DeliveryAddressPanel extends JPanel {
    private final JComboBox<String> province = new RoundedComboBox<>(new String[]{"Choose a location"});
    private final JComboBox<String> city = new RoundedComboBox<>(new String[]{"Select city / municipality"});
    private final JComboBox<String> barangay = new RoundedComboBox<>(new String[]{"Select barangay"});
    private final JTextArea street;
    private final JLabel point = new JLabel("Select your location to assign a delivery point.");
    public DeliveryAddressPanel(JTextArea street, Runnable changed) {
        this.street = street;
        setOpaque(false); setLayout(new GridLayout(0, 1, 0, 6));
        add(new JLabel("Province / Metro Manila")); add(province);
        add(new JLabel("City / Municipality")); add(city);
        add(new JLabel("Barangay")); add(barangay);
        add(new JLabel("House number / street / subdivision"));
        street.setLineWrap(true); street.setWrapStyleWord(true);
        add(new ModernScrollPane(street)); add(point);
        province.getAccessibleContext().setAccessibleName("Province / Metro Manila");
        city.getAccessibleContext().setAccessibleName("City / Municipality");
        barangay.getAccessibleContext().setAccessibleName("Barangay");
        province.addItem("Metro Manila");
        for (String name : DeliveryAddress.provinces())
            if (!"Metro Manila".equals(name)) province.addItem(name);
        city.setEnabled(false); barangay.setEnabled(false);
        province.addActionListener(e -> {
            reset(city, "Select city / municipality", DeliveryAddress.cities(selected(province)));
            reset(barangay, "Select barangay", new String[0]);
            point.setText(province.getSelectedIndex() > 0 ? DeliveryAddress.pointForProvince(selected(province)).getLabel() : "Select your location to assign a delivery point.");
            changed.run();
        });
        city.addActionListener(e -> { reset(barangay, "Select barangay", DeliveryAddress.barangays(selected(province), selected(city))); changed.run(); });
        barangay.addActionListener(e -> changed.run());
        street.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { changed.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { changed.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { changed.run(); }
        });
    }
    private static String selected(JComboBox<String> box) { return box.getSelectedIndex() > 0 ? String.valueOf(box.getSelectedItem()) : ""; }
    private static void reset(JComboBox<String> box, String hint, String[] values) {
        box.setModel(new DefaultComboBoxModel<>(new String[]{hint}));
        for (String name : values) box.addItem(name);
        box.setEnabled(values.length > 0);
    }
    public DeliveryAddress getAddress() { return new DeliveryAddress(selected(province), selected(city), selected(barangay), street.getText()); }
    public boolean setSavedAddress(String saved) {
        var parsed=DeliveryAddress.fromSavedAddress(saved);
        if(parsed.isEmpty())return false;
        DeliveryAddress value=parsed.get();
        province.setSelectedItem(value.getProvince());city.setSelectedItem(value.getCity());
        barangay.setSelectedItem(value.getBarangay());street.setText(value.getStreet());return true;
    }
    public String getPointLabel() { return point.getText(); }
}
