package model;
import java.util.*;
public final class OrderWorkflow {
    private OrderWorkflow() { }
    public static List<OrderStatus> targets(Order order) {
        if(order.getStatus().ordinal()>=OrderStatus.OUT_FOR_DELIVERY.ordinal())return List.of();
        List<OrderStatus> result=new ArrayList<>();
        for(OrderStatus target:Fulfillment.stages(order)) {
            try{target(order,target);result.add(target);}catch(IllegalArgumentException|IllegalStateException ignored){}
        }
        return result;
    }
    public static OrderStatus target(Order order,OrderStatus target) {
        if(target==null || target==OrderStatus.COMPLETED || target==OrderStatus.DELIVERED || order.getStatus().ordinal()>=OrderStatus.OUT_FOR_DELIVERY.ordinal() || target.ordinal()<=order.getStatus().ordinal())throw new IllegalArgumentException("Choose a later operational stage.");
        if(!Arrays.asList(Fulfillment.stages(order)).contains(target))throw new IllegalArgumentException("This stage does not apply to the order.");
        if(target!=Fulfillment.next(order,false))throw new IllegalStateException("Complete the current stage before proceeding to the next stage.");
        boolean remote=!order.getCheckoutDetails().isPickup()&&order.getCheckoutDetails().getDeliveryPoint()!=DeliveryPoint.CENTRAL;
        if(remote && (target==OrderStatus.AT_DELIVERY_POINT&&order.getStatus()!=OrderStatus.TRANSFERRING
                || target==OrderStatus.OUT_FOR_DELIVERY&&order.getStatus()!=OrderStatus.AT_DELIVERY_POINT))
            throw new IllegalStateException("Record transfer and arrival separately before local dispatch.");
        if(order.getCheckoutDetails().getDeliveryPoint()==DeliveryPoint.UNASSIGNED && target.ordinal()>=OrderStatus.TRANSFERRING.ordinal())
            throw new IllegalStateException("Review this order's address before dispatch.");
        return target;
    }
}
