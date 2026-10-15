package model;

/** Customer-supplied fulfillment and simulated payment details for an order. */
public final class CheckoutDetails {
    private final String fullName;
    private final String email;
    private final String contactNumber;
    private final String fulfillmentMethod;
    private final String address;
    private final String paymentMethod;
    private final String notes;
    private DeliveryAddress structuredAddress;

    public static CheckoutDetails structured(String name, String email, String contact, String method,
                                              DeliveryAddress address, String payment, String notes) {
        CheckoutDetails details = new CheckoutDetails(name, email, contact, method,
                address == null ? "" : address.format(), payment, notes);
        details.structuredAddress = address;
        details.validateFulfillment();
        return details;
    }
    public DeliveryAddress getStructuredAddress() { return structuredAddress; }
    public boolean isPickup() { return "Pickup".equals(fulfillmentMethod); }
    public DeliveryPoint getDeliveryPoint() {
        return isPickup() ? DeliveryPoint.CENTRAL : structuredAddress == null ? DeliveryPoint.UNASSIGNED : structuredAddress.getDeliveryPoint();
    }
    public void validateFulfillment() {
        if (!isPickup() && !"Delivery".equals(fulfillmentMethod))
            throw new IllegalArgumentException("Choose Delivery or Pickup.");
        if (!isPickup() && structuredAddress == null)
            throw new IllegalArgumentException("Select a structured delivery address before placing an order.");
    }

    public CheckoutDetails(String fullName, String email, String contactNumber,
                           String fulfillmentMethod, String address,
                           String paymentMethod, String notes) {
        this.fullName = fullName;
        this.email = email;
        this.contactNumber = contactNumber;
        this.fulfillmentMethod = fulfillmentMethod;
        this.address = address;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getContactNumber() { return contactNumber; }
    public String getFulfillmentMethod() { return fulfillmentMethod; }
    public String getAddress() { return address; }
    public String getPaymentMethod() { return paymentMethod; }
    /** Customer-supplied reference, separate from the verified payment record. */
    public String getSubmittedPaymentReference() {
        if(paymentMethod==null)return "";
        var match=java.util.regex.Pattern.compile("^(?:GCash\\s+ref\\.\\s*|Card\\s+ending\\s+[0-9]{4}\\s*/\\s*ref\\.\\s*)(.+)$",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(paymentMethod.trim());
        return match.matches()?match.group(1).trim():"";
    }
    public String getNotes() { return notes; }
}
