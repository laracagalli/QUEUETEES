package model;
import java.util.*;
/** Shortest packing job first; the heap is rebuilt from current waiting orders. */
public final class PackingPriority {
    private PackingPriority(){}
    public static long pieces(Order order){return order.getItems().stream().mapToLong(CartItem::getQuantity).sum();}
    public static final Comparator<Order> ORDER=Comparator.comparingLong(PackingPriority::pieces).thenComparing(Order::getPlacedAt).thenComparingInt(Order::getQueueNumber).thenComparingInt(Order::getId);
    public static List<Order> queue(Collection<Order> orders){
        PriorityQueue<Order> heap=new PriorityQueue<>(ORDER);for(Order order:orders)if(order.getStatus()==OrderStatus.CONFIRMED && order.canStartPacking())heap.offer(order);
        List<Order> result=new ArrayList<>();while(!heap.isEmpty())result.add(heap.poll());return List.copyOf(result);
    }
    public static boolean isNext(Collection<Order> orders,Order order){var waiting=queue(orders);return !waiting.isEmpty() && waiting.get(0).getId()==order.getId();}
}
