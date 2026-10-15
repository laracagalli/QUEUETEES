package gui.customer;

import gui.components.*;
import gui.staff.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;
import javax.swing.*;
import model.*;
import service.StoreService;

/** Acceptance scenarios: earlier distant deliveries cannot block ready nearby orders. */
public final class RegionalDeliveryTest {
    private static final StoreService STORE = StoreService.getInstance();
    private static DeliveryAddress address(String province) {
        String city = DeliveryAddress.cities(province)[0];
        return new DeliveryAddress(province, city, DeliveryAddress.barangays(province,city)[0], "123 Test Street");
    }
    private static Order place(int customer, String province) {
        STORE.addToCart(customer, 1);
        return support.TestFlow.verify(STORE,STORE.checkout(customer, CheckoutDetails.structured("Maria Cruz", "test@example.com", "+639123456789",
            province == null ? "Pickup" : "Delivery", province == null ? null : address(province), "GCash", "")));
    }
    private static void ready(Order order) { STORE.advanceOrder(order.getId()); STORE.advanceOrder(order.getId()); }
    private static void check(boolean value, String message) { if(!value)throw new AssertionError(message); }
    private static void fails(Runnable action) { try { action.run(); } catch (IllegalArgumentException | IllegalStateException ex) { return; } throw new AssertionError("Invalid action succeeded"); }
    private static Object field(Object o,String name)throws Exception { Field f=o.getClass().getDeclaredField(name); f.setAccessible(true);return f.get(o); }
    private static void layout(Container c) { c.doLayout(); for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child); }
    private static void render(JPanel panel, String name, int width, int height)throws Exception {
        AppTheme.apply(panel); panel.setSize(width,height);layout(panel);
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(231,235,222));g.fillRect(0,0,width,height);panel.printAll(g);g.dispose();
        new java.io.File("bin/review").mkdirs();ImageIO.write(image,"png",new java.io.File("bin/review/"+name+".png"));
    }
    public static void main(String[] args)throws Exception {
        check(address("Pampanga").getDeliveryPoint()==DeliveryPoint.CENTRAL,"Central assignment");
        check(address("Ilocos Norte").getDeliveryPoint()==DeliveryPoint.NORTHERN,"Northern assignment");
        check(address("Laguna").getDeliveryPoint()==DeliveryPoint.SOUTHERN,"Southern assignment");
        check(address("Metro Manila").getDeliveryPoint()==DeliveryPoint.CENTRAL,"NCR operational default");
        fails(()->new DeliveryAddress("Pampanga",DeliveryAddress.cities("Laguna")[0],"Invalid", "123 Street"));
        fails(()->new DeliveryAddress("Cebu","Cebu City","Invalid","123 Street"));
        fails(()->new DeliveryAddress("Pampanga",address("Pampanga").getCity(),address("Pampanga").getBarangay()," "));
        STORE.addToCart(999,1);int stock=STORE.findProduct(1).orElseThrow().getStock();
        fails(()->STORE.checkout(999,new CheckoutDetails("Maria","test@example.com","09123456789","Delivery","Free text","GCash","")));
        check(STORE.getCart(999).size()==1 && STORE.findProduct(1).orElseThrow().getStock()==stock,"Invalid address preserves cart and stock");
        Order north=place(901,"Ilocos Norte"),central=place(902,"Pampanga"),pickup=place(903,null),south=place(904,"Laguna");
        fails(()->STORE.advanceOrder(central.getId()));
        ready(north);STORE.advanceOrder(north.getId());
        check(north.getStatus()==OrderStatus.TRANSFERRING,"North transfers from hub");
        fails(()->STORE.completeOrder(north.getId()));
        User staff=new User(910,"staff","staff@example.com","unused",UserRole.STAFF,true,AccountStatus.ACTIVE);
        ready(central);STORE.advanceOrder(central.getId());STORE.confirmCourierDelivery(central.getId(),"PROOF-CENTRAL-001",staff);
        service.ActivityLogger.setActor(new User(902,"customer","c@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE));STORE.completeOrder(central.getId());service.ActivityLogger.setActor(null);check(north.getStatus()==OrderStatus.TRANSFERRING && central.getStatus()==OrderStatus.COMPLETED,"Central completes before earlier distant order");
        ready(pickup);STORE.completeOrder(pickup.getId());
        check(pickup.getStatus()==OrderStatus.COMPLETED,"Ready pickup collected before distant delivery finishes");
        ready(south);STORE.advanceOrder(south.getId());STORE.advanceOrder(south.getId());
        check(south.getStatus()==OrderStatus.AT_DELIVERY_POINT,"Arrival recorded");
        STORE.advanceOrder(south.getId());STORE.confirmCourierDelivery(south.getId(),"PROOF-SOUTH-001",staff);
        check(north.getStatus()==OrderStatus.TRANSFERRING,"South completes independently of north");
        SwingUtilities.invokeAndWait(()->{try {
            AppTheme.install();
            User user=new User(901,"test","test@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
            STORE.addToCart(901,2);CartPanel cart=new CartPanel(user,()->{});
            DeliveryAddressPanel form=(DeliveryAddressPanel)field(cart,"deliveryAddress");
            @SuppressWarnings("unchecked") JComboBox<String> province=(JComboBox<String>)field(form,"province");
            @SuppressWarnings("unchecked") JComboBox<String> city=(JComboBox<String>)field(form,"city");
            @SuppressWarnings("unchecked") JComboBox<String> barangay=(JComboBox<String>)field(form,"barangay");
            check("Choose a location".equals(province.getItemAt(0)) && "Metro Manila".equals(province.getItemAt(1)),"Placeholder is distinct from actual Metro Manila selection");
            province.setSelectedIndex(1);
            check(city.isEnabled() && city.getItemCount()==18,"Metro Manila enables all 17 cities and municipalities");
            for (int index=1;index<city.getItemCount();index++) {
                city.setSelectedIndex(index);
                check(barangay.isEnabled() && barangay.getItemCount()>1,"Metro Manila city has selectable barangays: "+city.getSelectedItem());
                barangay.setSelectedIndex(1);
                ((JTextArea)field(cart,"address")).setText("123 Test Street");
                check("Metro Manila".equals(form.getAddress().getProvince()),"Metro Manila address accepted");
            }
            city.setSelectedItem("Pasay City");barangay.setSelectedIndex(1);
            check("Pasay City".equals(form.getAddress().getCity()),"Pasay address can be completed");
            province.setSelectedItem("Laguna");city.setSelectedIndex(1);barangay.setSelectedIndex(1);
            ((JTextArea)field(cart,"address")).setText("123 Test Street");
            check(form.getAddress().getDeliveryPoint()==DeliveryPoint.SOUTHERN,"UI derives point");
            province.setSelectedItem("Pampanga");
            check(city.getSelectedIndex()==0 && barangay.getSelectedIndex()==0 && !barangay.isEnabled(),"Province changes reset city and barangay");
            city.setSelectedIndex(1);barangay.setSelectedIndex(1);
            var method=CartPanel.class.getDeclaredMethod("showCheckout");method.setAccessible(true);method.invoke(cart);
            render(cart,"regional-checkout",1120,680);
            check(form.getHeight()>=300,"Address selector has enough layout height");
            @SuppressWarnings("unchecked") JComboBox<String> fulfillment=(JComboBox<String>)field(cart,"fulfillment");
            check(fulfillment.getItemCount()==1 && "Delivery".equals(fulfillment.getItemAt(0)) && form.isVisible(),"Checkout offers only delivery and requires address inputs");
            render(cart,"regional-delivery-only-checkout",1120,680);
            render(new QueueStatusPanel(),"regional-delivery-board",1120,700);
            render(new OrderTrackingPanel(user),"regional-tracking",1330,690);
        } catch(Exception ex){throw new RuntimeException(ex);} });
        System.out.println("PASS: regional routing, independent deliveries, pickup collection, transfer stages, invalid addresses and cascading UI");
    }
}
