package service;
import model.*;
public final class CourierConfirmation {
    private CourierConfirmation() { }
    public static void validate(User actor,String reference,Order order) {
        if(actor==null || actor.getStatus()!=AccountStatus.ACTIVE || actor.getRole()==UserRole.CUSTOMER)
            throw new IllegalStateException("Active staff or administrator access is required.");
        if(reference==null || !reference.trim().matches("[A-Za-z0-9][A-Za-z0-9 ./_-]{3,99}"))
            throw new IllegalArgumentException("Enter the courier delivery-confirmation reference (4–100 characters).");
        if(order.getStatus()!=OrderStatus.OUT_FOR_DELIVERY || order.getCheckoutDetails().isPickup())
            throw new IllegalStateException("Courier confirmation applies only to dispatched delivery orders.");
    }
}
