import model.*;
import service.*;
import java.util.*;
public final class PackingAndPaymentTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void blocked(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Unsafe action succeeded");}
    static Order place(StoreService store,Product product,int customer,int amount,String province){String city=DeliveryAddress.cities(province)[0];store.addToCart(customer,product.getId(),amount);return store.checkoutDelivery(customer,CheckoutDetails.structured("Customer","c@example.com","09123456789","Delivery",new DeliveryAddress(province,city,DeliveryAddress.barangays(province,city)[0],"123 Test Street"),"GCash ref. 123456",""));}
    public static void main(String[] args)throws Exception{
        StoreService store=StoreService.getInstance();Product product=store.addProduct("Priority test","Men","Tops",100,50,"");User staff=new User(999,"staff","staff@example.com","unused",UserRole.STAFF,true,AccountStatus.ACTIVE);
        Order large=place(store,product,1,10,"Ilocos Norte"),small=place(store,product,2,1,"Metro Manila"),tie=place(store,product,3,1,"Laguna");
        check(RefundService.paidAmount(large)==0,"Typed payment references are not verified money");blocked(()->store.advanceOrder(large.getId()));blocked(()->store.verifyPayment(large.getId(),1,"BAD-REF",staff));
        for(Order order:List.of(large,small,tie))store.verifyPayment(order.getId(),order.getTotal(),"PAY-"+order.getId(),staff);
        check(PackingPriority.queue(store.getOrders()).equals(List.of(small,tie,large)),"1 piece precedes 10; arrival breaks quantity ties");blocked(()->store.advanceOrder(large.getId()));
        store.advanceOrder(small.getId());check(small.getStatus()==OrderStatus.PREPARING,"Small order starts packing");
        blocked(()->store.advanceOrderTo(small.getId(),OrderStatus.PREPARING,OrderStatus.OUT_FOR_DELIVERY));
        store.advanceOrder(tie.getId());store.advanceOrder(large.getId());store.advanceOrder(small.getId());store.advanceOrder(tie.getId());store.advanceOrder(large.getId());
        store.advanceOrder(large.getId());store.advanceOrder(large.getId()); // Transfer and arrival at distant point.
        store.advanceOrder(tie.getId());store.advanceOrder(tie.getId());
        check(DeliveryPriority.readyQueue(store.getOrders()).get(0).getId()==small.getId(),"Nearest city to Pasay receives first dispatch priority");blocked(()->store.advanceOrder(large.getId()));
        store.advanceOrder(small.getId());store.advanceOrder(tie.getId());store.advanceOrder(large.getId());
        check(DeliveryPriority.queue(store.getOrders()).get(0).getId()==small.getId(),"Dispatched queue is distance ranked");
        blocked(()->store.verifyPayment(large.getId(),large.getTotal(),"AGAIN",staff));
        store.confirmCourierDelivery(small.getId(),"PROOF-SMALL",staff);User buyer=new User(2,"buyer","buyer@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);ActivityLogger.setActor(buyer);store.completeOrder(small.getId());ActivityLogger.setActor(null);
        byte[] proof;try(var out=new java.io.ByteArrayOutputStream()){javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,1),"png",out);proof=out.toByteArray();}
        small.restoreCourier("PROOF-SMALL",java.time.LocalDateTime.now().minusDays(31));blocked(()->ReturnsService.submit(buyer,small.getId(),product.getId(),1,"Damaged item on arrival",proof,"proof.png"));
        small.restoreCourier("PROOF-SMALL",java.time.LocalDateTime.now());ReturnsService.submit(buyer,small.getId(),product.getId(),1,"Damaged item on arrival",proof,"proof.png");
        System.out.println("PASS: verified-money-only payments, 1-before-10 packing, FIFO ties, strict stages and nearest-to-Pasay dispatch");
    }
}
