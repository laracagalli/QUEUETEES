package gui.customer;

import gui.components.DeliveryAddressPanel;
import java.util.*;
import javax.swing.*;
import model.*;
import service.StoreService;

public final class CheckoutSubmissionTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked") static <T> T field(Object target,String name){try{var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return (T)f.get(target);}catch(Exception e){throw new RuntimeException(e);}}
    static void await(java.util.function.BooleanSupplier condition)throws Exception {
        for(int i=0;i<150;i++){boolean[] ready={false};SwingUtilities.invokeAndWait(()->ready[0]=condition.getAsBoolean());if(ready[0])return;Thread.sleep(20);}
        throw new AssertionError("Checkout did not finish");
    }
    public static void main(String[] args)throws Exception {
        StoreService store=StoreService.getInstance();
        String barangay=DeliveryAddress.barangays("Metro Manila","Pasay City")[0];
        DeliveryAddress saved=new DeliveryAddress("Metro Manila","Pasay City",barangay,"Unit 2, 123 Main Street");
        check(DeliveryAddress.fromSavedAddress(saved.format()).orElseThrow().getStreet().equals(saved.getStreet()),"Saved address preserves commas inside street");
        check(DeliveryAddress.fromSavedAddress("Unknown street, Unknown barangay, Unknown city, Metro Manila").isEmpty(),"Unrecognized legacy address is not silently assigned");
        User user=new User(9401,"checkoutuser","checkout@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE,
                "Maria Cruz",saved.format(),"9123456789","Female",java.time.LocalDate.of(2000,1,1));
        Product product=store.addProduct("Checkout test","Women","Tops",100,3,"");store.addToCart(user.getId(),product.getId(),1);
        List<String> notices=new ArrayList<>();int[] tracking={0};CartPanel[] panels={null};
        SwingUtilities.invokeAndWait(()->{
            CartPanel panel=new CartPanel(user,()->tracking[0]++,(title,message)->notices.add(title+": "+message));panels[0]=panel;
            check(((JTextField)field(panel,"fullName")).getText().equals(user.getFullName()),"Registration full name fills checkout");
            check(((JTextField)field(panel,"email")).getText().equals(user.getEmail()),"Registration email fills checkout");
            check(((JTextField)field(panel,"contact")).getText().equals(user.getContactNumber()),"Registration phone fills checkout");
            DeliveryAddressPanel address=field(panel,"deliveryAddress");
            check(address.getAddress().format().equals(saved.format()),"Province, city, barangay and street fill from registration");
            JButton place=field(panel,"placeOrderButton");
            place.doClick();check(notices.get(0).contains("reference"),"Place Order explains missing payment reference");
            check(store.getOrdersForCustomer(user.getId()).isEmpty(),"Invalid checkout creates no order");
            ((JTextField)field(panel,"qrReference")).setText("123456789");
            ((JComboBox<?>)field(address,"province")).setSelectedIndex(0);
            place.doClick();check(notices.get(1).contains("province"),"Place Order explains missing address");
            address.setSavedAddress(saved.format());place.doClick();place.doClick();
            check(!place.isEnabled() && place.getText().equals("Placing order..."),"Place Order immediately shows saving state and prevents repeat clicks");
        });
        await(()->tracking[0]==1);
        List<Order> orders=store.getOrdersForCustomer(user.getId());check(orders.size()==1,"One click creates exactly one order");
        Order order=orders.get(0);check(order.getCheckoutDetails().getAddress().equals(saved.format()) && order.getPaymentStatus()==PaymentStatus.PENDING,"Saved address reaches order and payment still requires verification");
        check(order.getCheckoutDetails().getSubmittedPaymentReference().equals("123456789"),"Checkout preserves customer's GCash reference for verification autofill");
        check(new CheckoutDetails("Customer","c@example.com","09123456789","Pickup","","Card ending 4321 / ref. CARD-REF-001","").getSubmittedPaymentReference().equals("CARD-REF-001"),"Card reference is separate from last four digits");
        check(new CheckoutDetails("Customer","c@example.com","09123456789","Pickup","","Card ending 4321","").getSubmittedPaymentReference().isEmpty(),"Missing card reference stays blank");
        check(store.getCart(user.getId()).isEmpty() && product.getStock()==2,"Successful checkout clears cart and reserves one item");
        SwingUtilities.invokeAndWait(()->{
            CartPanel panel=panels[0];store.addToCart(user.getId(),product.getId(),1);product.setStock(0);panel.refresh();
            ((JTextField)field(panel,"qrReference")).setText("987654321");((JButton)field(panel,"placeOrderButton")).doClick();
        });
        await(()->!(boolean)field(panels[0],"placingOrder"));
        SwingUtilities.invokeAndWait(()->{
            check(notices.get(notices.size()-1).contains("enough stock"),"Save failures are shown to customer");
            check(((JButton)field(panels[0],"placeOrderButton")).isEnabled(),"Place Order becomes usable after failure");
        });
        check(store.getCart(user.getId()).size()==1 && store.getOrdersForCustomer(user.getId()).size()==1,"Failed save preserves cart and creates no extra order");
        System.out.println("PASS: registration details fill checkout, Place Order validation, background save, duplicate-click protection, tracking and failed-save recovery");
    }
}
