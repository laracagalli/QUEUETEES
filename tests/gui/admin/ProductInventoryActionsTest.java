package gui.admin;
import java.awt.*;
import java.lang.reflect.*;
import javax.swing.*;
import model.*;
import service.*;
@SuppressWarnings("unchecked")
public class ProductInventoryActionsTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Object field(Object p,String name)throws Exception{Field f=p.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(p);}
    static void reject(Runnable task){try{task.run();throw new AssertionError("Invalid adjustment accepted");}catch(IllegalArgumentException expected){}}
    public static void main(String[] args)throws Exception{
        User admin=new User(940,"audit-admin","admin@example.com","unused",UserRole.ADMIN,true,AccountStatus.ACTIVE);
        StoreService store=StoreService.getInstance();
        Product p=store.addProduct("Adjustment shirt","Test","Tops",99,5,"/Gui_Images/mentop1.png","M");
        store.addToCart(940,p.getId());Order order=store.checkout(940,new CheckoutDetails("Maria Cruz","test@example.com","+639123456789","Pickup","","GCash",""));
        store.addToCart(941,p.getId());store.adjustStock(p.getId(),7,admin);check(p.getStock()==11,"Replenishment adds stock");
        int logged=ActivityLogger.getEntries().size();reject(()->store.adjustStock(p.getId(),-11,admin));reject(()->store.adjustStock(p.getId(),0,admin));
        check(p.getStock()==11&&ActivityLogger.getEntries().size()==logged,"Manual decreases cannot change stock or produce success logs");
        check(!SecurityIncidents.incidents().isEmpty(),"Prohibited changes create reviewable warnings");
        check(store.getCart(941).size()==1&&order.getTotal()==99&&order.getItems().get(0).getProduct().getSize().equals("M"),"Cart and purchased size/price preserved");
        Product limit=store.addProduct("Capacity shirt","Test","Tops",99,Integer.MAX_VALUE-1,"","M");store.adjustStock(limit.getId(),1,admin);reject(()->store.adjustStock(limit.getId(),1,admin));
        reject(()->store.addProduct("Adjustment shirt","Test","Tops",99,20,"","M"));
        store.addProduct("Adjustment shirt","Test","Tops",99,20,"/Gui_Images/mentop1.png","L");
        SwingUtilities.invokeAndWait(()->{try{
            ProductManagementPanel panel=new ProductManagementPanel(admin);JTable table=(JTable)field(panel,"table");((JComboBox<?>)field(panel,"categoryFilter")).setSelectedItem("Test");
            check(!table.isCellEditable(0,0)&&table.getColumnCount()==7,"Read-only inventory without repeated action cells");
            java.util.List<Product> rows=(java.util.List<Product>)field(panel,"rowProducts");
            JButton replenish=(JButton)field(panel,"replenishButton");JSpinner quantity=(JSpinner)field(panel,"replenishQuantity");
            check(!replenish.isEnabled()&&!quantity.isEnabled(),"Replenishment requires a selection");
            table.setRowSelectionInterval(0,0);
            check(replenish.isEnabled(),"Selected product can be replenished");
            check(((JLabel)field(panel,"selectedStockLabel")).getText().equals(rows.get(table.convertRowIndexToModel(0)).getDisplayName()),"Selection targets sorted product and size");
            quantity.setValue(4);check(p.getStock()==11,"Choosing quantity does not change stock");
            table.clearSelection();check(!replenish.isEnabled(),"Cleared selection disables action");
            ((JComboBox<?>)field(panel,"categoryFilter")).setSelectedIndex(0);
            gui.components.AppTheme.apply(panel);panel.setSize(1200,780);for(int i=0;i<4;i++)layout(panel);
            java.awt.image.BufferedImage imageOut=new java.awt.image.BufferedImage(1200,780,java.awt.image.BufferedImage.TYPE_INT_RGB);Graphics2D g=imageOut.createGraphics();g.setColor(new Color(231,235,222));g.fillRect(0,0,1200,780);panel.printAll(g);g.dispose();new java.io.File("bin/review").mkdirs();javax.imageio.ImageIO.write(imageOut,"png",new java.io.File("bin/review/inventory-stock-bar.png"));
            AdminTableReport report=new AdminTableReport(table,"Stock report",admin);JTable snapshot=(JTable)field(report,"snapshot");check(snapshot.getColumnCount()==8,"Report includes sizes and hidden picture, excludes action controls");
            boolean image=false;for(int r=0;r<snapshot.getRowCount();r++)for(int c=0;c<snapshot.getColumnCount();c++)image|=snapshot.getValueAt(r,c) instanceof Icon;
            check(image,"Printed report includes product images");
        }catch(Exception ex){throw new RuntimeException(ex);}});
        System.out.println("PASS: replenishment-only inventory, separate quantity control, sizing, warnings, historical stock, sorted targeting and print images");
    }
    static void layout(Container root){root.doLayout();for(Component child:root.getComponents())if(child instanceof Container container)layout(container);}
}
