package model;
import java.util.List;

/** Customer-facing return outcomes, separate from the delivery completion record. */
public final class ReturnPresentation {
    private ReturnPresentation() { }
    public static String refundLabel(ReturnRequest r){return switch(r.status()){
        case "REFUNDED"->String.format("₱%,.2f",r.refundAmount());case "RECEIVED"->"Pending admin payment";case "APPROVED"->"Awaiting item return";case "SUBMITTED"->"Awaiting review";default->"Not applicable";};}
    public static String status(ReturnRequest r){return switch(r.status()){
        case "SUBMITTED"->"Awaiting review";case "APPROVED"->"Approved / send item back";
        case "RECEIVED"->"Returned / awaiting refund";case "REFUNDED"->"Refunded";case "REJECTED"->"Rejected";default->r.status();};}
    public static String nextStep(ReturnRequest r){return switch(r.status()){
        case "SUBMITTED"->"Staff will review your reason and proof.";
        case "APPROVED"->"Follow the staff instructions to send the approved items back. Refund follows inspection.";
        case "RECEIVED"->"Staff received the returned items. Admin must record the refund payment.";
        case "REFUNDED"->"Refund recorded. Check the payment reference and staff note in request details.";
        case "REJECTED"->"Read the review reason in request details.";default->"View request details.";};}
    public static double expected(Order order,ReturnRequest r){return order.getItems().stream().filter(i->i.getProduct().getId()==r.productId()).findFirst().orElseThrow().getProduct().getPrice()*r.quantity();}
    public static String history(Order order,List<ReturnRequest> requests){
        List<ReturnRequest> own=requests.stream().filter(r->r.orderId()==order.getId()).toList();
        int refunded=own.stream().filter(r->r.status().equals("REFUNDED")).mapToInt(ReturnRequest::quantity).sum();
        int returned=own.stream().filter(r->r.status().equals("RECEIVED")||r.status().equals("REFUNDED")).mapToInt(ReturnRequest::quantity).sum();
        if(refunded>0)return refunded==order.getItemCount()?"Refunded":"Partially refunded";
        if(returned>0)return returned==order.getItemCount()?"Returned / awaiting refund":"Partially returned / awaiting refund";
        if(own.stream().anyMatch(r->r.status().equals("APPROVED")))return "Return approved";
        if(own.stream().anyMatch(r->r.status().equals("SUBMITTED")))return "Return under review";
        if(!own.isEmpty())return "Completed / return rejected";
        return order.getStatusLabel();
    }
}
