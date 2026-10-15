package gui.customer;
import model.*;import service.*;import gui.components.*;import javax.swing.*;
import java.util.*;import java.awt.image.BufferedImage;

public final class ReturnWorkflowPresentationTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void fails(Runnable action){try{action.run();throw new AssertionError("Invalid action accepted");}catch(IllegalStateException|IllegalArgumentException expected){}}
    public static void main(String[] args)throws Exception {
        User customer=new User(600,"customer","c@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
        User staff=new User(601,"staff","s@example.com","unused",UserRole.STAFF,true,AccountStatus.ACTIVE);
        User admin=new User(602,"admin","a@example.com","unused",UserRole.ADMIN,true,AccountStatus.ACTIVE);
        StoreService store=StoreService.getInstance();Product product=store.addProduct("Return test shirt","Men","Tops",100,5,"","M");store.addToCart(customer.getId(),product.getId(),3);
        fails(()->store.checkoutDelivery(customer.getId(),new CheckoutDetails("Customer","c@example.com","09123456789","Pickup","","Cash","")));
        String city=DeliveryAddress.cities("Pampanga")[0];Order order=store.checkoutDelivery(customer.getId(),CheckoutDetails.structured("Customer","c@example.com","09123456789","Delivery",new DeliveryAddress("Pampanga",city,DeliveryAddress.barangays("Pampanga",city)[0],"123 Street"),"Cash",""));
        support.TestFlow.advanceTo(store,order.getId(),OrderStatus.CONFIRMED,OrderStatus.OUT_FOR_DELIVERY);store.confirmCourierDelivery(order.getId(),"DELIVERY-TEST",staff);ActivityLogger.setActor(customer);store.completeOrder(order.getId());ActivityLogger.setActor(null);
        byte[] proof;try(var out=new java.io.ByteArrayOutputStream()){javax.imageio.ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out);proof=out.toByteArray();}
        ReturnRequest first=ReturnsService.submit(customer,order.getId(),product.getId(),1,"Damaged item received",proof,"proof.png");
        check(ReturnPresentation.history(order,ReturnsService.requests(customer)).equals("Return under review"),"History shows pending return");
        fails(()->ReturnsService.review(admin,first.id(),"REFUNDED","Paid without receipt","REF-001"));
        ReturnsService.review(staff,first.id(),"APPROVED","Send the item back to our main hub with request #1","");
        SwingUtilities.invokeAndWait(()->{try{AppTheme.install();MaximizedLayoutTest.render(new ReturnsPanel(customer),"returns-approved",1350,760);MaximizedLayoutTest.render(new OrderHistoryPanel(customer),"history-return-approved",1350,760);}catch(Exception ex){throw new RuntimeException(ex);}});
        check(ReturnPresentation.history(order,ReturnsService.requests(customer)).equals("Return approved"),"History shows approval");
        ReturnsService.review(staff,first.id(),"RECEIVED","Returned item inspected","");check(ReturnPresentation.history(order,ReturnsService.requests(customer)).startsWith("Partially returned"),"Partial return distinguished");
        SwingUtilities.invokeAndWait(()->{try{
            ReturnsPanel staffView=new ReturnsPanel(staff),adminView=new ReturnsPanel(admin);
            java.lang.reflect.Field tableField=ReturnsPanel.class.getDeclaredField("table");tableField.setAccessible(true);
            ((JTable)tableField.get(staffView)).setRowSelectionInterval(0,0);((JTable)tableField.get(adminView)).setRowSelectionInterval(0,0);
            java.lang.reflect.Field actions=ReturnsPanel.class.getDeclaredField("actionsByStatus");actions.setAccessible(true);
            @SuppressWarnings("unchecked") Map<String,JButton> staffActions=(Map<String,JButton>)actions.get(staffView);
            @SuppressWarnings("unchecked") Map<String,JButton> adminActions=(Map<String,JButton>)actions.get(adminView);
            check(!staffActions.get("REFUNDED").isEnabled()&&adminActions.get("REFUNDED").isEnabled(),"Payment action explains and enforces admin role");
            check(((JTable)tableField.get(staffView)).getValueAt(0,7).equals("Pending admin payment"),"Received items show the actual next step");
            MaximizedLayoutTest.render(staffView,"returns-staff-payment-pending",900,750);MaximizedLayoutTest.render(adminView,"returns-admin-payment-pending",900,750);
            check(((RoundedButton)adminActions.get("APPROVED")).getBgColor().equals(ActionButtons.GREEN),"Approval retains green action color");
            check(((RoundedButton)adminActions.get("REJECTED")).getBgColor().equals(ActionButtons.RED),"Rejection retains red action color");
            check(((RoundedButton)adminActions.get("REFUNDED")).getBgColor().equals(ActionButtons.BLUE),"Refund retains blue action color");
            java.util.List<String> colorLabels=new java.util.ArrayList<>();collectLabels(adminView,colorLabels);
            check(colorLabels.containsAll(java.util.List.of("Approve / confirm","Reject","Details / payment","Other actions","Unavailable")),"Color meanings stay visible with disabled actions");
            for(JButton button:adminActions.values())check(button.getX()+button.getWidth()<=button.getParent().getWidth(),"Workflow action remains inside footer");
        }catch(Exception ex){throw new RuntimeException(ex);}});
        fails(()->ReturnsService.review(staff,first.id(),"REFUNDED","Paid to customer","REF-001"));
        ReturnsService.review(admin,first.id(),"REFUNDED","Cash refund paid to the customer","REF-001");
        check(ReturnPresentation.history(order,ReturnsService.requests(customer)).equals("Partially refunded"),"Partial refund distinguished");
        ReturnRequest rest=ReturnsService.submit(customer,order.getId(),product.getId(),2,"Remaining items are damaged",proof,"proof.png");
        ReturnsService.review(staff,rest.id(),"APPROVED","Send remaining items to the main hub","");ReturnsService.review(staff,rest.id(),"RECEIVED","Items returned and inspected","");ReturnsService.review(admin,rest.id(),"REFUNDED","Cash refund paid to the customer","REF-002");
        check(ReturnPresentation.history(order,ReturnsService.requests(customer)).equals("Refunded"),"Full refund replaces Completed in history");
        check(ReturnsService.requests(customer).stream().mapToDouble(ReturnRequest::refundAmount).sum()==300,"Refunds use purchased quantity and price");
        check(ReturnsService.requests(customer).get(0).resolution().contains("Send the item back"),"Review history retains return instructions");
        SwingUtilities.invokeAndWait(()->{try{
            MaximizedLayoutTest.render(new ReturnsPanel(customer),"returns-refunded",1350,760);MaximizedLayoutTest.render(new OrderHistoryPanel(customer),"history-refunded",1350,760);
            JPanel returnDetails=ReturnRequestDetails.create(order,ReturnsService.requests(customer).get(0));
            MaximizedLayoutTest.render(returnDetails,"return-details-cards",840,620);
            JPanel orderDetails=CustomerOrderDetails.summary(order,ReturnsService.requests(customer),RefundService.records(customer));
            MaximizedLayoutTest.render(orderDetails,"customer-order-cards",940,440);
            java.util.List<String> labels=new java.util.ArrayList<>();collectLabels(returnDetails,labels);
            check(labels.contains("Expected refund")&&labels.contains("Refund payment")&&labels.contains("Submitted proof")&&labels.contains("Review notes & return instructions"),"Return information is grouped into distinct cards");
            labels.clear();collectLabels(orderDetails,labels);
            check(labels.contains("Customer")&&labels.contains("Delivery & receipt")&&labels.contains("Order overview")&&labels.contains("Payment method"),"Order details are grouped into readable sections");
        }catch(Exception ex){throw new RuntimeException(ex);}});
        System.out.println("PASS: delivery-only storefront, approval/return/refund transitions, full/partial history and retained instructions");
    }
    static void collectLabels(java.awt.Container root,java.util.List<String> labels){for(java.awt.Component child:root.getComponents()){if(child instanceof JLabel label)labels.add(label.getText());if(child instanceof java.awt.Container container)collectLabels(container,labels);}}
}
