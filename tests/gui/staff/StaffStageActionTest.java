package gui.staff;

import java.awt.*;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;
import model.*;
import service.*;

/** Exercises the real Update stage button and dialogs against isolated memory data. */
public final class StaffStageActionTest {
    record Step(String title,Consumer<JDialog> respond){}
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static java.util.List<Component> children(Container root){java.util.List<Component> result=new ArrayList<>();for(Component child:root.getComponents()){result.add(child);if(child instanceof Container container)result.addAll(children(container));}return result;}
    static void press(JDialog dialog,String text){((JButton)children(dialog).stream().filter(c->c instanceof JButton b&&text.equals(b.getText())).findFirst().orElseThrow()).doClick();}
    static Step respond(String title,String button){return new Step(title,d->press(d,button));}
    static Step stage(Order order,OrderStatus status){return new Step("Update "+order.getControlNumber(),dialog->{
        JComboBox<?> combo=(JComboBox<?>)children(dialog).stream().filter(c->c instanceof JComboBox<?>).findFirst().orElseThrow();
        check(combo.getSelectedItem()==status,"Update stage offers the correct next stage");press(dialog,"OK");
    });}
    static void click(JButton button,Step... steps){
        Deque<Step> pending=new ArrayDeque<>(java.util.List.of(steps));Set<Window> handled=new HashSet<>();Throwable[] error={null};long deadline=System.currentTimeMillis()+8000;
        javax.swing.Timer timer=new javax.swing.Timer(70,event->{
            for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isVisible()&&!handled.contains(dialog)){
                handled.add(dialog);
                try{Step next=pending.removeFirst();check(dialog.getTitle().equals(next.title()),"Unexpected dialog: "+dialog.getTitle()+"; expected "+next.title());
                    SwingUtilities.invokeLater(()->{try{next.respond().accept(dialog);}catch(Throwable failure){error[0]=failure;dialog.dispose();}});
                }
                catch(Throwable failure){error[0]=failure;dialog.dispose();}
                return;
            }
            if(System.currentTimeMillis()>deadline){error[0]=new AssertionError("Update stage did not finish");for(Window window:Window.getWindows())if(window instanceof JDialog)window.dispose();}
        });
        timer.start();try{button.doClick();}finally{timer.stop();}
        if(error[0]!=null)throw new AssertionError(error[0]);check(pending.isEmpty(),"Expected dialogs did not open");
    }
    public static void main(String[] args)throws Exception {
        try{SwingUtilities.invokeAndWait(()->{
            gui.components.AppTheme.install();StoreService store=StoreService.getInstance();
            User staff=new User(9001,"staff","staff@example.com","unused",UserRole.STAFF,true,AccountStatus.ACTIVE);ActivityLogger.setActor(staff);
            DeliveryAddress address=new DeliveryAddress("Metro Manila","Pasay City",DeliveryAddress.barangays("Metro Manila","Pasay City")[0],"123 Test Street");
            Product product=store.addProduct("Stage test shirt","Men","Tops",100,10,"");store.addToCart(9002,product.getId());
            Order order=store.checkoutDelivery(9002,CheckoutDetails.structured("Customer","customer@example.com","09123456789","Delivery",address,"GCash ref. 123456789",""));
            Order stale=Order.restore(order.getId(),order.getQueueNumber(),order.getCustomerId(),order.getCustomerName(),order.getItems(),order.getPlacedAt(),order.getCheckoutDetails(),OrderStatus.CONFIRMED,null).restoreControlNumber(order.getControlNumber());
            int[] refreshes={0};JButton button=StaffOrderActions.shortcutButton(null,()->stale,()->refreshes[0]++);
            click(button,respond("Payment verification required","Cancel"));check(order.getStatus()==OrderStatus.CONFIRMED&&!order.canStartPacking(),"Unpaid order cannot enter preparation");
            click(button,respond("Payment verification required","Verify payment"),new Step("Verify payment • "+order.getControlNumber(),dialog->{
                var fields=children(dialog).stream().filter(c->c instanceof JTextField).map(c->(JTextField)c).toList();
                check(fields.get(1).getText().equals("123456789")&&fields.get(1).isEditable(),"Customer reference is prefilled and editable");
                check(order.getPaymentStatus()==PaymentStatus.PENDING,"Prefilling does not verify the payment");
                fields.get(1).setText("STAGE-PAY-001");press(dialog,"OK");
            }),respond("Message","OK"),stage(order,OrderStatus.PREPARING));
            check(order.getStatus()==OrderStatus.PREPARING&&order.getPaymentStatus()==PaymentStatus.VERIFIED&&refreshes[0]>=2,"Payment verification continues into preparation and refreshes the page");
            check(order.getPaymentReference().equals("STAGE-PAY-001")&&order.getCheckoutDetails().getSubmittedPaymentReference().equals("123456789"),"Staff correction is recorded separately from customer reference");
            click(button,stage(order,OrderStatus.READY_FOR_PICKUP));check(order.getStatus()==OrderStatus.READY_FOR_PICKUP,"Ready stage is saved from current state despite stale selection");
            click(button,stage(order,OrderStatus.OUT_FOR_DELIVERY));check(order.getStatus()==OrderStatus.OUT_FOR_DELIVERY,"Dispatch stage works");
            click(button,respond("Update order","Yes"),new Step("Courier confirmation",dialog->{
                ((JTextField)children(dialog).stream().filter(c->c instanceof JTextField).findFirst().orElseThrow()).setText("COURIER-STAGE-001");press(dialog,"OK");
            }));
            check(order.getStatus()==OrderStatus.DELIVERED&&!order.getCourierReference().isBlank(),"Update stage routes out-for-delivery orders to courier evidence");
            click(button,respond("Awaiting customer receipt","OK"));check(order.getStatus()==OrderStatus.DELIVERED,"Staff cannot bypass customer acknowledgment");
            store.addToCart(9003,product.getId(),2);Order larger=store.checkoutDelivery(9003,order.getCheckoutDetails());
            store.addToCart(9004,product.getId(),1);Order smaller=store.checkoutDelivery(9004,order.getCheckoutDetails());
            store.verifyPayment(larger.getId(),larger.getTotal(),"STAGE-PAY-LARGER",staff);store.verifyPayment(smaller.getId(),smaller.getTotal(),"STAGE-PAY-SMALLER",staff);
            JButton largerButton=StaffOrderActions.shortcutButton(null,()->larger,()->{});
            click(largerButton,respond("Packing priority","OK"));check(larger.getStatus()==OrderStatus.CONFIRMED,"Larger job cannot bypass smaller waiting job");
            click(StaffOrderActions.shortcutButton(null,()->smaller,()->{}),stage(smaller,OrderStatus.PREPARING));
            click(largerButton,stage(larger,OrderStatus.PREPARING));
            ActivityLogger.setActor(new User(9002,"customer","customer@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE));store.completeOrder(order.getId());ActivityLogger.setActor(staff);
            click(button,respond("Update stage","OK"));check(order.getStatus()==OrderStatus.COMPLETED,"Closed order gives feedback without further updates");
            click(StaffOrderActions.shortcutButton(null,()->null,()->{}),respond("Update stage","OK"));
            System.out.println("PASS: real Update stage dialogs verify payment, advance current stages, dispatch, record courier evidence and explain blocked/empty selections");
        });}finally{SwingUtilities.invokeAndWait(()->{for(Window window:Window.getWindows())window.dispose();});}
    }
}
