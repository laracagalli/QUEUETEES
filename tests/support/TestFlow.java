package support;
import model.*;
import service.*;
/** Test fixtures complete real verification and each operational stage explicitly. */
public final class TestFlow {
    private static final User OPERATOR=new User(999999,"fixture-staff","fixture@example.com","unused",UserRole.STAFF,true,AccountStatus.ACTIVE);
    public static Order verify(StoreService store,Order order){if(order.getPaymentStatus()!=PaymentStatus.VERIFIED)store.verifyPayment(order.getId(),order.getTotal(),"TEST-PAY-"+order.getId(),OPERATOR);return order;}
    public static void advanceTo(StoreService store,int id,OrderStatus expected,OrderStatus target){
        Order order=store.getOrders().stream().filter(o->o.getId()==id).findFirst().orElseThrow();
        if(order.getStatus()!=expected)throw new IllegalStateException("Fixture expected status mismatch");verify(store,order);
        while(order.getStatus()!=target){OrderStatus next=Fulfillment.next(order,false);
            if(next==OrderStatus.OUT_FOR_DELIVERY){var ready=DeliveryPriority.readyQueue(store.getOrders());while(!ready.isEmpty() && ready.get(0).getId()!=id){store.advanceOrder(ready.get(0).getId());ready=DeliveryPriority.readyQueue(store.getOrders());}}
            store.advanceOrderTo(id,order.getStatus(),next);order=store.getOrders().stream().filter(o->o.getId()==id).findFirst().orElseThrow();
        }
    }
}
