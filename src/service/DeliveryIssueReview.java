package service;
import model.*;
public final class DeliveryIssueReview {
    private DeliveryIssueReview() { }
    public static void validate(User actor,String outcome,String note) {
        if(actor==null||actor.getStatus()!=AccountStatus.ACTIVE||actor.getRole()==UserRole.CUSTOMER)throw new IllegalStateException("Active staff or administrator access is required.");
        if(outcome==null||!java.util.Set.of("REVIEWING","REDELIVERY","RESOLVED","LOST").contains(outcome))throw new IllegalArgumentException("Choose a valid delivery review action.");
        if(note==null||note.trim().length()<5||note.trim().length()>1000)throw new IllegalArgumentException("Provide a customer response in 5–1000 characters.");
    }
    public static void checkOrder(Order order) {
        if(order.getStatus()!=OrderStatus.DELIVERED||order.getDeliveryIssue().isBlank()||!java.util.Set.of("OPEN","REVIEWING").contains(order.getDeliveryIssueStatus()))throw new IllegalStateException("This report changed or no longer awaits review. Refresh the order.");
    }
    public static String label(String state){return switch(state){case "OPEN"->"Awaiting staff review";case "REVIEWING"->"Staff investigating";case "REDELIVERY"->"Redelivery arranged";case "RESOLVED"->"Staff response recorded";default->"No report";};}
}
