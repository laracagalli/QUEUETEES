import model.*;
import service.*;
import java.time.LocalDateTime;
import java.util.List;

public final class FlowSafetyTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static void blocked(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Unsafe action succeeded");}
    public static void main(String[] args)throws Exception {
        StoreService store=StoreService.getInstance();ActivityLogger.setActor(null);
        Product product=store.addProduct("Quantity safety","Men","Tops",10,Integer.MAX_VALUE,"");
        store.addToCart(9101,product.getId(),1);
        blocked(()->store.addToCart(9101,product.getId(),Integer.MAX_VALUE));
        check(store.getCart(9101).get(0).getQuantity()==1,"Overflow rejection preserves cart");
        blocked(()->new CartItem(product,0));blocked(()->new CartItem(product,-1));
        CartItem item=new CartItem(product,1);blocked(()->item.setQuantity(-5));
        check(item.getQuantity()==1,"Invalid quantity does not overwrite valid quantity");
        User customer=new User(9101,"buyer","buyer@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
        User staff=new User(9102,"staff","staff@example.com","unused",UserRole.STAFF,true,AccountStatus.ACTIVE);
        Order legacy=new Order(99001,99001,customer.getId(),"Buyer",List.of(new CartItem(product,1)),LocalDateTime.now(),
                new CheckoutDetails("Buyer","buyer@example.com","09123456789","Delivery","Old address","GCash",""));
        var field=StoreService.class.getDeclaredField("orders");field.setAccessible(true);
        @SuppressWarnings("unchecked") List<Order> orders=(List<Order>)field.get(store);orders.add(legacy);
        String city=DeliveryAddress.cities("Metro Manila")[0];
        DeliveryAddress address=new DeliveryAddress("Metro Manila",city,DeliveryAddress.barangays("Metro Manila",city)[0],"123 Test Street");
        ActivityLogger.setActor(customer);blocked(()->store.assignDeliveryAddress(legacy.getId(),address));
        staff.setStatus(AccountStatus.SUSPENDED);ActivityLogger.setActor(staff);blocked(()->store.assignDeliveryAddress(legacy.getId(),address));
        check(legacy.getCheckoutDetails().getDeliveryPoint()==DeliveryPoint.UNASSIGNED,"Rejected address edits leave order unchanged");
        staff.setStatus(AccountStatus.ACTIVE);store.assignDeliveryAddress(legacy.getId(),address);
        check(legacy.getCheckoutDetails().getDeliveryPoint()==DeliveryPoint.CENTRAL,"Active staff can review legacy delivery address");
        ActivityLogger.setActor(null);
        System.out.println("PASS: cart overflow, positive quantities and authorized delivery address corrections");
    }
}
