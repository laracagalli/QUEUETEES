import model.*;
import java.time.LocalDateTime;
import java.util.*;

public final class DeliveryPriorityTest {
    static Order order(int id,String province,OrderStatus status) {
        DeliveryAddress address=null;
        if(province!=null){String city=DeliveryAddress.cities(province)[0];address=new DeliveryAddress(province,city,DeliveryAddress.barangays(province,city)[0],"123 Test Street");}
        CheckoutDetails details=CheckoutDetails.structured("Customer","test@example.com","09123456789",province==null?"Pickup":"Delivery",address,"Cash","");
        Order order=new Order(id,id,id,"Customer",List.of(),LocalDateTime.now(),details);order.setStatus(status);return order;
    }
    public static void main(String[] args) {
        Order north=order(1,"Ilocos Norte",OrderStatus.OUT_FOR_DELIVERY),south=order(2,"Laguna",OrderStatus.OUT_FOR_DELIVERY);
        Order central=order(3,"Pampanga",OrderStatus.OUT_FOR_DELIVERY),laterCentral=order(4,"Pampanga",OrderStatus.OUT_FOR_DELIVERY);
        List<Order> orders=List.of(laterCentral,south,order(5,"Pampanga",OrderStatus.PREPARING),north,central,order(6,null,OrderStatus.READY_FOR_PICKUP),order(7,"Pampanga",OrderStatus.DELIVERED));
        List<Order> expected=new ArrayList<>(List.of(central,laterCentral,north,south));expected.sort(Comparator.comparingDouble(WarehouseDistance::kilometres).thenComparing(Order::getPlacedAt));
        if(!DeliveryPriority.queue(orders).equals(expected))throw new AssertionError("Nearest-to-Pasay priority and only dispatched deliveries");
        // Arrival evidence may legitimately be recorded out of the recommended sequence.
        service.CourierConfirmation.validate(new User(90,"staff","s@example.com","unused",UserRole.STAFF,true,AccountStatus.ACTIVE),"PROOF-NORTH",north);
        if(north.getQueueNumber()!=1||central.getQueueNumber()!=3)throw new AssertionError("Delivery sorting changed preparation sequence");
        central.setStatus(OrderStatus.DELIVERED);
        expected.remove(central);if(!DeliveryPriority.queue(orders).equals(expected))throw new AssertionError("Delivered orders leave scheduling queue");
        System.out.println("PASS: nearest-to-Pasay delivery priority, arrival ties, eligibility, independent evidence and permanent order identifiers");
    }
}
