import gui.components.*;
import java.awt.*;
import javax.swing.*;
import model.*;
import service.StoreService;
public class ModernUiTest {
 static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
 static void render(JComponent panel,String name,int w,int h)throws Exception{AppTheme.apply(panel);panel.setSize(w,h);layout(panel);java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(new Color(231,235,222));g.fillRect(0,0,w,h);panel.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/modern-"+name+".png"));}
 static boolean split(Component c){if(c instanceof JSplitPane)return true;if(c instanceof Container)for(Component child:((Container)c).getComponents())if(split(child))return true;return false;}
 public static void main(String[]args)throws Exception{SwingUtilities.invokeAndWait(()->{try{
 AppTheme.install();User user=new User(500,"test","test@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);StoreService store=StoreService.getInstance();int product=store.getProducts().get(0).getId();store.addToCart(500,product);store.checkout(500,new CheckoutDetails("Maria Cruz","test@example.com","+639123456789","Pickup","","GCash",""));
 gui.staff.OrderQueuePanel queue=new gui.staff.OrderQueuePanel();if(split(queue))throw new AssertionError("Queue still has split pane");render(queue,"queue",1120,700);
 render(new gui.customer.OrderHistoryPanel(user),"history",1120,630);
 store.addToCart(500,product);gui.customer.CartPanel cart=new gui.customer.CartPanel(user,()->{});java.lang.reflect.Method show=cart.getClass().getDeclaredMethod("showCheckout");show.setAccessible(true);show.invoke(cart);render(cart,"checkout",1120,680);
 JOptionPane pane=new JOptionPane("1 × Geisha Graphic Top added to your cart.",JOptionPane.INFORMATION_MESSAGE);render(pane,"dialog",460,170);
 JTextField field=new JTextField();AppTheme.apply(field);field.setText("Kept value");if(!field.getText().equals("Kept value"))throw new AssertionError();
 System.out.println("PASS: shared theme rendering, dialog, checkout, history and queue-only layout");
 }catch(Exception e){throw new RuntimeException(e);}});}
}
