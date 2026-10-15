package model;

public enum DeliveryPoint {
    CENTRAL("Pasay warehouse / Central delivery zone"), NORTHERN("Northern Luzon delivery point"),
    SOUTHERN("Southern Luzon delivery point"), UNASSIGNED("Address requires review");
    private final String label;
    DeliveryPoint(String label) { this.label = label; }
    public String getLabel() { return label; }
}
