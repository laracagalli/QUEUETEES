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
        var admin=new User(940,"audit-admin","admin@example.com","unused",UserRole.ADMIN,true,AccountStatus.ACTIVE);
        StoreService store=StoreService.getInstance();
        Product p=store.addProduct("Adjustment shirt","Test","Tops",99,5,"");
        store.addToCart(940,p.getId());
        Order order=store.checkout(940,new CheckoutDetails("Maria Cruz","test@example.com","+639123456789","Pickup","","GCash",""));
        store.addToCart(941,p.getId());
        store.adjustStock(p.getId(),7,admin);
        check(p.getStock()==11,"Stock increase");
        store.adjustStock(p.getId(),-11,admin);
        check(p.getStock()==0&&!p.isAvailable(),"Stock can decrease to zero");
        check(store.findProduct(p.getId()).isPresent()&&store.getCart(941).size()==1,"Minus never deletes products or cart entries");
        check(order.getTotal()==99&&order.getItems().get(0).getProduct().getStock()==4,"Historical snapshot unchanged");
        int logged=ActivityLogger.getEntries().size();
        reject(()->store.adjustStock(p.getId(),-1,admin));
        reject(()->store.adjustStock(p.getId(),0,admin));
        store.adjustStock(p.getId(),Integer.MAX_VALUE,admin);
        reject(()->store.adjustStock(p.getId(),1,admin));
        check(ActivityLogger.getEntries().size()==logged+1,"Invalid changes do not create success logs");
        store.adjustStock(p.getId(),-Integer.MAX_VALUE,admin);
        Product duplicate=store.addProduct("Adjustment shirt","Test","Tops",99,20,"");
        SwingUtilities.invokeAndWait(()->{try{
            ProductManagementPanel panel=new ProductManagementPanel(admin);
            JTable table=(JTable)field(panel,"table");
            ((JComboBox<?>)field(panel,"categoryFilter")).setSelectedItem("Test");
            int column=table.convertColumnIndexToView(7);
            check(table.isCellEditable(0,column)&&!table.isCellEditable(0,0),"Only stock actions editable");
            check(table.getColumnCount()==7,"Dedicated action column present");
            JPanel rendered=(JPanel)table.prepareRenderer(table.getCellRenderer(0,column),0,column);
            for(Component child:rendered.getComponents())check(child.getWidth()>0&&child.getHeight()>0,"Stock controls must render with visible bounds");
            java.util.List<Product> rows=(java.util.List<Product>)field(panel,"rowProducts");
            final int[] submitted={0,0};
            var cell=new gui.components.StockAdjustmentCell(rows::get,(product,delta)->{submitted[0]=product.getId();submitted[1]=delta;});
            JPanel editor=(JPanel)cell.getTableCellEditorComponent(table,"",true,0,column);
            JButton minus=(JButton)editor.getComponent(0),plus=(JButton)editor.getComponent(2);
            check(!minus.isEnabled(),"Minus disabled at zero stock");
            ((JTextField)editor.getComponent(1)).setText("3");
            plus.doClick();
            check(submitted[0]==p.getId()&&submitted[1]==3,"Plus submits quantity for correct sorted product");
            table.getRowSorter().toggleSortOrder(4);
            editor=(JPanel)cell.getTableCellEditorComponent(table,"",true,0,column);
            ((JTextField)editor.getComponent(1)).setText("2");
            ((JButton)editor.getComponent(0)).doClick();
            check(submitted[0]==duplicate.getId()&&submitted[1]==-2,"Minus targets correct product after reverse sort");
            var report=new AdminTableReport(table,"Stock report",admin);
            JTable snapshot=(JTable)field(report,"snapshot");
            check(snapshot.getColumnCount()==6,"Report excludes action controls");
            System.out.println("PASS: row stock controls, limits, sorted targeting, history, carts, audit and reports");
        }catch(Exception ex){throw new RuntimeException(ex);}});
    }
}
