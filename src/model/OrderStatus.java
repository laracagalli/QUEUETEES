package model;

public enum OrderStatus {
    CONFIRMED("Confirmed"),
    PREPARING("Preparing"),
    READY_FOR_PICKUP("Ready for pickup"),
    COMPLETED("Completed");

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
