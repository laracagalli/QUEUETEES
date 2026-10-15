package gui.customer;
import gui.components.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import model.*;
import service.*;
import java.io.File;
import javax.imageio.ImageIO;

/** Render the reported screens at normal and maximized sizes for visual inspection. */
public final class MaximizedLayoutTest {
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container cc)layout(cc);}
    static void render(JPanel panel,String name,int width,int height)throws Exception {
        AppTheme.apply(panel);panel.setSize(width,height);for(int i=0;i<3;i++)layout(panel);
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(new Color(231,235,222));g.fillRect(0,0,width,height);panel.printAll(g);g.dispose();
        new File("bin/review").mkdirs();ImageIO.write(image,"png",new File("bin/review/layout-"+name+".png"));
    }
    public static void main(String[] args)throws Exception {SwingUtilities.invokeAndWait(()->{try {
        AppTheme.install();User customer=new User(800,"customer","c@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
        OrderHistoryPanel history=new OrderHistoryPanel(customer);render(history,"history-max",1600,850);
        for(Component child:history.getComponents())if(child instanceof JPanel && ((BorderLayout)history.getLayout()).getConstraints(child)==BorderLayout.CENTER && child.getHeight()<650)throw new AssertionError("History does not fill maximized content");
        StoreService store=StoreService.getInstance();store.addToCart(800,1);String city=DeliveryAddress.cities("Pampanga")[0];
        Order order=store.checkout(800,CheckoutDetails.structured("Customer","c@example.com","09123456789","Delivery",new DeliveryAddress("Pampanga",city,DeliveryAddress.barangays("Pampanga",city)[0],"123 Street"),"Cash",""));
        support.TestFlow.advanceTo(store,order.getId(),OrderStatus.CONFIRMED,OrderStatus.OUT_FOR_DELIVERY);
        render(new gui.staff.QueueStatusPanel(),"delivery-priority",1500,900);
        store.confirmCourierDelivery(order.getId(),"PROOF-001",new User(900,"staff","s@example.com","unused",UserRole.STAFF,true,AccountStatus.ACTIVE));
        render(new OrderTrackingPanel(customer),"tracking-max",1600,900);render(new OrderTrackingPanel(customer),"tracking-normal",1120,800);
        var users=new repository.InMemoryUserRepository();var admin=new User(901,"admin","admin@example.com","unused",UserRole.ADMIN,true,AccountStatus.ACTIVE);users.save(admin);
        render(new gui.admin.AdminDashboardPanel(new AuthService(users),admin),"admin-max",1600,1000);
        System.out.println("PASS: maximized history expansion and rendered delivery/tracking/admin layouts");
    }catch(Exception ex){throw new RuntimeException(ex);}});}
}
