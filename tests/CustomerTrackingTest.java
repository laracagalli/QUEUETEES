package gui.customer;
import model.*; import service.*; import javax.swing.*; import java.awt.*; import java.awt.image.*; import javax.imageio.*; import java.io.*;
public class CustomerTrackingTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void layout(Container c){c.doLayout();for(Component x:c.getComponents())if(x instanceof Container)layout((Container)x);}
 static void render(JPanel p,String name)throws Exception{JPanel w=new JPanel(new BorderLayout());w.setBackground(new Color(231,234,224));w.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));w.add(p);w.setSize(1330,690);layout(w);BufferedImage im=new BufferedImage(1330,690,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);w.printAll(g);g.dispose();new File("bin/review").mkdirs();ImageIO.write(im,"png",new File("bin/review/"+name));}
 public static void main(String[]args)throws Exception{SwingUtilities.invokeAndWait(()->{try{
 User u=new User(12,"customer","customer@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
 OrderTrackingPanel p=new OrderTrackingPanel(u);render(p,"tracking-empty.png");StoreService s=StoreService.getInstance();
 s.addToCart(55,1);Order first=s.checkout(55,new CheckoutDetails("Other customer","other@example.com","09123456789","Pickup","","Cash",""));
 s.addToCart(12,6);s.addToCart(12,7);Order own=s.checkout(12,new CheckoutDetails("Maria Dela Cruz","customer@example.com","+639123456789","Delivery","123 Sample Street, San Pedro, Laguna","GCash ref. 1234567890123","Please call on arrival."));
 p.refresh();check(p.ordersAhead(own)==1,"Waiting queue counts earlier order");render(p,"tracking-confirmed.png");
 s.advanceOrder(first.getId());check(p.ordersAhead(own)==0,"Already preparing order leaves waiting queue");s.advanceOrder(own.getId());p.refresh();render(p,"tracking-preparing.png");
 s.advanceOrder(own.getId());s.advanceOrder(own.getId());p.refresh();
 var f=OrderTrackingPanel.class.getDeclaredField("selector");f.setAccessible(true);JComboBox<?> c=(JComboBox<?>)f.get(p);check(c.getItemCount()==1,"Only own orders are shown");check(c.getSelectedItem()==own,"Completed ticket remains visible");
 s.addToCart(12,2);Order another=s.checkout(12,new CheckoutDetails("Maria","customer@example.com","+639123456789","Pickup","","Cash",""));p.refresh();check(c.getItemCount()==2,"Multiple own orders available");c.setSelectedItem(another);p.refresh();check(c.getSelectedItem()==another,"Selected order stays selected");
 }catch(Exception ex){throw new RuntimeException(ex);}});System.out.println("PASS: customer isolation, queue counts, progression, completed ticket and multiple order selection");}
}
