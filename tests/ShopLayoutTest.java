import gui.components.AppTheme;
import gui.customer.ShopPanel;
import java.awt.*;
import javax.swing.*;
import model.*;

/** The catalog must retain accessible controls as its viewport changes width. */
public class ShopLayoutTest {
    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) if (child instanceof Container c) layout(c);
    }
    private static JButton cartButton(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton b && b.getText().equals("Add to Cart")) return b;
            if (child instanceof Container c) { JButton found=cartButton(c); if(found!=null)return found; }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                AppTheme.install();
                ShopPanel shop=new ShopPanel(new User(500,"shoptest","test@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE),()->{});
                AppTheme.apply(shop);
                var field=ShopPanel.class.getDeclaredField("products");field.setAccessible(true);
                JPanel grid=(JPanel)field.get(shop);
                for(int width:new int[]{1020,700,1020}) {
                    shop.setSize(width,620);
                    for(int i=0;i<5;i++)layout(shop);
                    Component card=grid.getComponent(0);
                    JButton add=cartButton((Container)card);
                    if(add==null)throw new AssertionError("Missing Add to Cart");
                    Rectangle bounds=SwingUtilities.convertRectangle(add.getParent(),add.getBounds(),grid);
                    if(!card.getBounds().contains(bounds))throw new AssertionError("Product controls clipped");
                    if(width==1020 && !((JViewport)grid.getParent()).getViewRect().contains(bounds))throw new AssertionError("First-row controls below viewport");
                    int columns=((GridLayout)grid.getLayout()).getColumns();
                    if(columns!=(width==700?2:3))throw new AssertionError("Catalog columns did not adapt");
                }
                var search=ShopPanel.class.getDeclaredField("searchField");search.setAccessible(true);
                JTextField input=(JTextField)search.get(shop);
                input.setText("no-such-product-xyz");
                if(!(grid.getComponent(0) instanceof JLabel))throw new AssertionError("Search no longer filters");
                input.setText("");
                if(cartButton(grid)==null)throw new AssertionError("Clearing search did not restore products");
                System.out.println("PASS: shop controls, responsive columns and search");
            } catch(Exception e) { throw new RuntimeException(e); }
        });
    }
}
