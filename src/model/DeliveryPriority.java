package model;
import java.util.*;

/** Scheduling guidance for dispatched deliveries; actual delivery evidence remains independent. */
public final class DeliveryPriority {
    private DeliveryPriority() { }
    public static List<Order> queue(Collection<Order> orders) {
        return ordered(orders.stream().filter(o->!o.getCheckoutDetails().isPickup() && o.getStatus()==OrderStatus.OUT_FOR_DELIVERY).toList());
    }
    private static List<Order> ordered(Collection<Order> orders){PriorityQueue<Order> heap=new PriorityQueue<>(Comparator.comparingDouble(WarehouseDistance::kilometres).thenComparing(Order::getPlacedAt).thenComparingInt(Order::getQueueNumber).thenComparingInt(Order::getId));heap.addAll(orders);List<Order> result=new ArrayList<>();while(!heap.isEmpty())result.add(heap.poll());return List.copyOf(result);}
    public static List<Order> readyQueue(Collection<Order> orders){return ordered(orders.stream().filter(o->!o.getCheckoutDetails().isPickup() && (o.getStatus()==OrderStatus.AT_DELIVERY_POINT || o.getStatus()==OrderStatus.READY_FOR_PICKUP && o.getCheckoutDetails().getDeliveryPoint()==DeliveryPoint.CENTRAL)).toList());}
    public static void requireNearest(Collection<Order> orders,Order order){var ready=readyQueue(orders);if(!ready.isEmpty() && ready.get(0).getId()!=order.getId())throw new IllegalStateException("Dispatch the nearest ready destination to Pasay first: "+ready.get(0).getControlNumber());}
}
