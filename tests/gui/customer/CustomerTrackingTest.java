package gui.customer;
import model.*; import service.*; import javax.swing.*; import java.awt.*; import java.awt.image.*; import javax.imageio.*; import java.io.*;
public class CustomerTrackingTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static boolean hasButton(Container c,String text){for(Component x:c.getComponents()){if(x instanceof JButton b && text.equals(b.getText()))return true;if(x instanceof Container child && hasButton(child,text))return true;}return false;}
 static boolean hasLabel(Container c,String text){for(Component x:c.getComponents()){if(x instanceof JLabel label&&text.equals(label.getText()))return true;if(x instanceof Container child&&hasLabel(child,text))return true;}return false;}
 static void layout(Container c){c.doLayout();for(Component x:c.getComponents())if(x instanceof Container)layout((Container)x);}
 static void render(JPanel p,String name)throws Exception{JPanel w=new JPanel(new BorderLayout());w.setBackground(new Color(231,234,224));w.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));w.add(p);w.setSize(1330,690);layout(w);BufferedImage im=new BufferedImage(1330,690,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);w.printAll(g);g.dispose();new File("bin/review").mkdirs();ImageIO.write(im,"png",new File("bin/review/"+name));}
 public static void main(String[]args)throws Exception{SwingUtilities.invokeAndWait(()->{try{
 User u=new User(12,"customer","customer@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
 OrderTrackingPanel p=new OrderTrackingPanel(u);render(p,"tracking-empty.png");StoreService s=StoreService.getInstance();
 s.addToCart(55,1);Order first=s.checkout(55,new CheckoutDetails("Other customer","other@example.com","09123456789","Pickup","","Cash",""));
 String city=DeliveryAddress.cities("Laguna")[0];
 s.addToCart(12,6);s.addToCart(12,7);Order own=s.checkout(12,CheckoutDetails.structured("Maria Dela Cruz","customer@example.com","+639123456789","Delivery",new DeliveryAddress("Laguna",city,DeliveryAddress.barangays("Laguna",city)[0],"123 Sample Street"),"GCash ref. 1234567890123","Please call on arrival."));
 p.refresh();check(hasLabel(p,"Payment: PENDING"),"Tracking shows pending payment before verification");
 support.TestFlow.verify(s,own);p.refresh();check(hasLabel(p,"Payment: VERIFIED"),"Tracking refreshes payment without requiring a stage change");check(own.getStatus()==OrderStatus.CONFIRMED,"Own order begins confirmed");render(p,"tracking-confirmed.png");
 s.advanceOrder(first.getId());check(own.getStatus()==OrderStatus.CONFIRMED,"Another customer's preparation does not change own status");s.advanceOrder(own.getId());p.refresh();render(p,"tracking-preparing.png");
 s.advanceOrder(own.getId());s.advanceOrder(own.getId());p.refresh();
 var f=OrderTrackingPanel.class.getDeclaredField("selector");f.setAccessible(true);JComboBox<?> c=(JComboBox<?>)f.get(p);check(c.getItemCount()==1,"Only own orders are shown");check(c.getSelectedItem()==own,"Completed ticket remains visible");
  while(own.getStatus()!=OrderStatus.OUT_FOR_DELIVERY)s.advanceOrder(own.getId());
  p.refresh();check(!hasButton(p,"Order Received"),"Cannot acknowledge before delivery is recorded");
 ActivityLogger.setActor(u);try{s.completeOrder(own.getId());throw new AssertionError("Early confirmation succeeded");}catch(IllegalStateException expected){}
 ActivityLogger.setActor(null);User staff=new User(77,"staff","staff@example.com","unused",UserRole.STAFF,true,AccountStatus.ACTIVE);
 s.confirmCourierDelivery(own.getId(),"DELIVERY-001",staff);check(own.getStatus()==OrderStatus.DELIVERED&&own.getCompletedAt()==null,"Courier evidence does not complete order");
 check(own.getDeliveredBy()==77,"Delivery operator retained");
 try{s.advanceOrderTo(own.getId(),OrderStatus.DELIVERED,OrderStatus.COMPLETED);throw new AssertionError("Shortcut completed delivery");}catch(IllegalArgumentException expected){}
 s.reportDeliveryIssue(own.getId(),"Package has not arrived",u);check(own.getStatus()==OrderStatus.DELIVERED&&!own.getDeliveryIssue().isBlank(),"Issue leaves acknowledgment pending");
 p.refresh();check(hasButton(p,"Order Received")&&hasButton(p,"Report awaiting review"),"Delivery acknowledgment and issue actions visible");render(p,"tracking-delivered.png");
 try{s.reportDeliveryIssue(own.getId(),"Duplicate report",u);throw new AssertionError("Duplicate pending report accepted");}catch(IllegalStateException expected){}
 try{s.reviewDeliveryIssue(own.getId(),"REDELIVERY","Send it again",u);throw new AssertionError("Customer reviewed own report");}catch(IllegalStateException expected){}
 s.reviewDeliveryIssue(own.getId(),"REVIEWING","Checking with the delivery team",staff);check(own.getDeliveryIssueStatus().equals("REVIEWING"),"Investigation status recorded");
 s.reviewDeliveryIssue(own.getId(),"REDELIVERY","New delivery scheduled for tomorrow",staff);check(own.getStatus()==OrderStatus.OUT_FOR_DELIVERY&&own.getCourierReference().isBlank(),"Redelivery resets delivery evidence");
 ActivityLogger.setActor(u);try{s.completeOrder(own.getId());throw new AssertionError("Redelivery prematurely acknowledged");}catch(IllegalStateException expected){}
 s.confirmCourierDelivery(own.getId(),"DELIVERY-002",staff);check(own.getDeliveryIssueResponse().contains("tomorrow"),"Staff response remains visible");
 ActivityLogger.setActor(new User(55,"other","other@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE));
 try{s.completeOrder(own.getId());throw new AssertionError("Other customer confirmed receipt");}catch(IllegalStateException expected){}
 ActivityLogger.setActor(u);s.completeOrder(own.getId());check(own.getStatus()==OrderStatus.COMPLETED,"Owner confirms delivered order");check(own.getCompletedAt()!=null,"Completion time recorded");p.refresh();check(c.getItemCount()==0,"Received order leaves tracking");
 ActivityLogger.setActor(null);
s.addToCart(12,2);Order another=s.checkout(12,new CheckoutDetails("Maria","customer@example.com","+639123456789","Pickup","","Cash",""));p.refresh();check(c.getItemCount()==1,"Completed order is excluded");c.setSelectedItem(another);p.refresh();check(c.getSelectedItem()==another,"Selected order stays selected");
 }catch(Exception ex){throw new RuntimeException(ex);}});System.out.println("PASS: customer isolation, queue counts, progression, completed ticket and multiple order selection");}
}
