import gui.components.*;
import java.awt.*;
import javax.swing.*;
import model.*;
import service.StoreService;
public class ModernUiTest {
 static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
 static void render(JComponent panel,String name,int w,int h)throws Exception{AppTheme.apply(panel);panel.setSize(w,h);for(int i=0;i<3;i++)layout(panel);java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(new Color(231,235,222));g.fillRect(0,0,w,h);panel.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/modern-"+name+".png"));}
 static boolean split(Component c){if(c instanceof JSplitPane)return true;if(c instanceof Container)for(Component child:((Container)c).getComponents())if(split(child))return true;return false;}
 static java.util.List<JButton> navigation(Container root){java.util.List<JButton> result=new java.util.ArrayList<>();for(Component child:root.getComponents()){if(child instanceof JButton button&&button.getClass().getSimpleName().endsWith("NavButton"))result.add(button);if(child instanceof Container container)result.addAll(navigation(container));}return result;}
 static void checkNavigation(JPanel dashboard,int expected)throws Exception{
 var nav=navigation(dashboard);if(nav.size()!=expected)throw new AssertionError("Missing navigation links");
 var contentField=dashboard.getClass().getDeclaredField("content");contentField.setAccessible(true);Container content=(Container)contentField.get(dashboard);
 java.util.Set<Component> visited=new java.util.HashSet<>();
 for(JButton button:nav){if(button.getIcon()==null)throw new AssertionError("Missing icon: "+button.getText());button.doClick();for(Component page:content.getComponents())if(page.isVisible())visited.add(page);}
 if(visited.size()!=expected)throw new AssertionError("Navigation does not open distinct pages");
 }
 public static void main(String[]args)throws Exception{SwingUtilities.invokeAndWait(()->{try{
 AppTheme.install();User user=new User(500,"test","test@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);StoreService store=StoreService.getInstance();int product=store.getProducts().get(0).getId();store.addToCart(500,product);store.checkout(500,new CheckoutDetails("Maria Cruz","test@example.com","+639123456789","Pickup","","GCash",""));
 gui.staff.OrderQueuePanel queue=new gui.staff.OrderQueuePanel();if(split(queue))throw new AssertionError("Queue still has split pane");render(queue,"queue",1120,700);
 var users=new repository.InMemoryUserRepository();User staff=new User(501,"stafftest","staff@example.com","unused",UserRole.STAFF,true,AccountStatus.ACTIVE);users.save(staff);
 var dashboard=new gui.staff.StaffDashboardPanel(new service.AuthService(users),staff);
 render(dashboard,"staff-sidebar",1440,820);
 var nav=navigation(dashboard);if(nav.size()!=7)throw new AssertionError("All seven staff sidebar links remain available");
 var targets=java.util.Map.of("Dashboard","overview","Order Queue","queue","Order Details","orders","Completed Orders","completed","Delivery Points","status","Order Returns","returns","My Account","profile");
 var current=dashboard.getClass().getDeclaredField("currentPanel");current.setAccessible(true);
 for(JButton button:nav){if(button.getIcon()==null)throw new AssertionError("Missing sidebar icon: "+button.getText());button.doClick();if(!targets.get(button.getText()).equals(current.get(dashboard)))throw new AssertionError("Sidebar navigation changed");}
 users.save(user);User admin=new User(502,"admintest","admin@example.com","unused",UserRole.ADMIN,true,AccountStatus.ACTIVE);users.save(admin);
 var customerDashboard=new gui.customer.CustomerDashboardPanel(new service.AuthService(users),user);
 render(customerDashboard,"customer-icons",1440,820);render(customerDashboard,"customer-icons-narrow",1100,760);
 for(JButton button:navigation(customerDashboard)){Rectangle bounds=SwingUtilities.convertRectangle(button.getParent(),button.getBounds(),customerDashboard);if(!new Rectangle(0,0,customerDashboard.getWidth(),customerDashboard.getComponent(0).getHeight()).contains(bounds))throw new AssertionError("Customer navigation clipped: "+button.getText());}
 checkNavigation(customerDashboard,6);
 var adminDashboard=new gui.admin.AdminDashboardPanel(new service.AuthService(users),admin);render(adminDashboard,"admin-icons",1440,820);checkNavigation(adminDashboard,9);
 render(new gui.customer.OrderHistoryPanel(user),"history",1120,630);
 store.addToCart(500,product);gui.customer.CartPanel cart=new gui.customer.CartPanel(user,()->{});java.lang.reflect.Method show=cart.getClass().getDeclaredMethod("showCheckout");show.setAccessible(true);show.invoke(cart);render(cart,"checkout",1120,680);
 JOptionPane pane=new JOptionPane("1 × Geisha Graphic Top added to your cart.",JOptionPane.INFORMATION_MESSAGE);render(pane,"dialog",460,170);
 JTextField field=new JTextField();AppTheme.apply(field);field.setText("Kept value");if(!field.getText().equals("Kept value"))throw new AssertionError();
 System.out.println("PASS: shared theme rendering, dialog, checkout, history and queue-only layout");
 }catch(Exception e){throw new RuntimeException(e);}});}
}
