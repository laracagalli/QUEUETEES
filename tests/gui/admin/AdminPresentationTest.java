package gui.admin;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
public class AdminPresentationTest {
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
    static void render(JPanel panel,String name)throws Exception{panel.setSize(1120,700);layout(panel);BufferedImage image=new BufferedImage(1120,700,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(new Color(230,234,224));g.fillRect(0,0,1120,700);panel.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/admin-"+name+".png"));}
    public static void main(String[] args)throws Exception{SwingUtilities.invokeAndWait(()->{try{
        render(new StaffApprovalsPanel(),"staff");render(new CustomerManagementPanel(),"customers");
        ProductManagementPanel products=new ProductManagementPanel();render(products,"products");render(new SalesReportsPanel(),"sales");render(new AdminOrdersPanel(false,null),"queue");
        DefaultTableModel data=new DefaultTableModel(new String[]{"Product","Stock","Status"},0);for(int i=0;i<90;i++)data.addRow(new Object[]{"Product "+i,i,"Available"});JTable table=new JTable(data);AdminUi.style(table);table.setSize(900,500);table.doLayout();AdminTableReport report=new AdminTableReport(table,"Inventory test",null);if(report.page(0)==null||report.page(1)==null)throw new AssertionError("Missing pages");data.setRowCount(0);if(report.rows!=90||report.page(1)==null)throw new AssertionError("Report snapshot changed");javax.imageio.ImageIO.write(report.page(0),"png",new java.io.File("bin/review/admin-report.png"));
        AdminTableReport.open(products,table,"Empty report",null);render(products,"preview");if(!"adminReportPreview".equals(products.getComponent(0).getName()))throw new AssertionError("Preview missing");
        System.out.println("PASS: admin layouts, preview, pagination and immutable report snapshot");
    }catch(Exception ex){throw new RuntimeException(ex);}});}
}
