package service;
import model.*;
public final class PaymentVerification {
    private PaymentVerification(){}
    public static void validate(User actor,Order order,double amount,String reference){
        if(actor==null || actor.getStatus()!=AccountStatus.ACTIVE || actor.getRole()==UserRole.CUSTOMER)throw new IllegalStateException("Active staff or administrator access is required.");
        if(order.getPaymentStatus()==PaymentStatus.VERIFIED || order.getStatus()==OrderStatus.CANCELLED || order.getStatus()==OrderStatus.LOST)throw new IllegalStateException("Payment already verified or order is closed.");
        if(!Double.isFinite(amount) || amount<=0 || Math.abs(amount-order.getTotal())>.001)throw new IllegalArgumentException("Verified payment must equal the order total.");
        if(reference==null || !reference.trim().matches("[A-Za-z0-9][A-Za-z0-9 ./_-]{3,99}"))throw new IllegalArgumentException("Enter the actual payment reference (4–100 characters).");
    }
}
