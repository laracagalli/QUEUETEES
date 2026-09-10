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
    public String getNotes() { return notes; }
}
