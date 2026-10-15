package gui.admin;

import gui.components.*;
import gui.customer.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDate;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.*;
import model.*;
import repository.*;
import service.*;

public final class ConsultationChangesTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void fails(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Invalid action succeeded");}
    private static Object field(Object value,String name)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    private static void layout(Container panel){panel.doLayout();for(Component c:panel.getComponents())if(c instanceof Container p)layout(p);}
    private static void render(JPanel panel,String name)throws Exception{AppTheme.apply(panel);panel.setSize(1120,700);layout(panel);BufferedImage image=new BufferedImage(1120,700,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(AppTheme.PAPER);g.fillRect(0,0,1120,700);panel.printAll(g);g.dispose();new File("bin/review").mkdirs();ImageIO.write(image,"png",new File("bin/review/consultation-"+name+".png"));}
    private static RegistrationResult register(AuthService auth,String email,LocalDate birthday){return auth.registerCustomer(email,"Maria Cruz",email.split("@")[0],"Customer123!".toCharArray(),"123 Street, Barangay One, Apalit, Pampanga","9123456789","Female",birthday);}
    public static void main(String[] args)throws Exception{
        for(String email:new String[]{"a@b.com","mary+tag@example.co.uk","o'connor@example.com"})check(RegistrationValidation.validEmail(email),"Valid mailbox syntax");
        for(String email:new String[]{"@.","a@b","a@@b.com","a..b@example.com",".a@example.com","a.@example.com","a@-bad.com","a@bad-.com","a@b..com","a@b.123"," a@b.com","a b@example.com"})check(!RegistrationValidation.validEmail(email),"Invalid email rejected: "+email);
        InMemoryUserRepository users=new InMemoryUserRepository();AuthService auth=new AuthService(users);
        check(!register(auth,"bad@.",LocalDate.now().minusYears(25)).isSuccess()&&users.findAll().isEmpty(),"Invalid email cannot create an account before OTP");
        check(!register(auth,"old@example.com",LocalDate.now().minusYears(121)).isSuccess(),"Age above 120 rejected");
        check(!register(auth,"young@example.com",LocalDate.now().minusYears(18).plusDays(1)).isSuccess(),"Age under 18 rejected");
        check(register(auth,"oldvalid@example.com",LocalDate.now().minusYears(120)).isSuccess(),"Age 120 accepted without ID approval");
        User admin=new User(100,"admin","admin@example.com",PasswordUtil.hashPassword("Admin123!"),UserRole.ADMIN,true,AccountStatus.ACTIVE);
        User staff=new User(101,"staff","staff@example.com",PasswordUtil.hashPassword("Staff123!"),UserRole.STAFF,true,AccountStatus.ACTIVE);
        User customer=new User(102,"customer","customer@example.com",PasswordUtil.hashPassword("Customer123!"),UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
        User other=new User(103,"other","other@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
        users.save(admin);users.save(staff);users.save(customer);users.save(other);ActivityLogger.setActor(admin);
        StoreService store=StoreService.getInstance();Product medium=store.addProduct("Sized shirt","Test","Tops",99,2,"/Gui_Images/mentop1.png","M"),large=store.addProduct("Sized shirt","Test","Tops",99,3,"/Gui_Images/mentop1.png","L");
        store.addToCart(customer.getId(),medium.getId());store.addToCart(customer.getId(),large.getId());Order order=store.checkout(customer.getId(),CheckoutDetails.structured("Maria Cruz",customer.getEmail(),"+639123456789","Pickup",null,"GCash",""));
        check(order.getItems().size()==2&&medium.getStock()==1&&large.getStock()==2,"Separate variant stock and cart items");
        support.TestFlow.advanceTo(store,order.getId(),OrderStatus.CONFIRMED,OrderStatus.READY_FOR_PICKUP);store.completeOrder(order.getId());
        check(order.getControlNumber().matches("QT-[A-F0-9]{12}"),"Random control number format");
        ByteArrayOutputStream proofOut=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(3,3,BufferedImage.TYPE_INT_RGB),"png",proofOut);byte[] proof=proofOut.toByteArray();
        fails(()->ReturnsService.submit(customer,order.getId(),medium.getId(),1,"Damaged stitching",null,"proof.png"));
        fails(()->ReturnsService.submit(other,order.getId(),medium.getId(),1,"Damaged stitching",proof,"proof.png"));
        fails(()->ReturnsService.submit(customer,order.getId(),medium.getId(),2,"Damaged stitching",proof,"proof.png"));
        ReturnRequest request=ReturnsService.submit(customer,order.getId(),medium.getId(),1,"Damaged stitching",proof,"proof.png");
        fails(()->ReturnsService.submit(customer,order.getId(),medium.getId(),1,"Duplicate return",proof,"proof.png"));
        fails(()->ReturnsService.review(staff,request.id(),"REFUNDED","Reviewed evidence","REF-001"));
        ReturnsService.review(staff,request.id(),"APPROVED","Proof supports return","");ReturnsService.review(staff,request.id(),"RECEIVED","Returned item received","");medium.setPrice(1);
        ReturnRequest refunded=ReturnsService.review(admin,request.id(),"REFUNDED","Payment sent and recorded","REF-001");
        check(refunded.refundAmount()==99&&medium.getStock()==1,"Refund uses purchased price; returns do not silently replenish damaged stock");
        check(ReturnsService.requests(other).isEmpty()&&ReturnsService.requests(customer).size()==1,"Return records isolated by customer");
        String city=DeliveryAddress.cities("Pampanga")[0];DeliveryAddress address=new DeliveryAddress("Pampanga",city,DeliveryAddress.barangays("Pampanga",city)[0],"123 Street");
        store.addToCart(customer.getId(),large.getId());Order delivery=store.checkout(customer.getId(),CheckoutDetails.structured("Maria Cruz",customer.getEmail(),"+639123456789","Delivery",address,"GCash",""));
        support.TestFlow.advanceTo(store,delivery.getId(),OrderStatus.CONFIRMED,OrderStatus.OUT_FOR_DELIVERY);fails(()->store.completeOrder(delivery.getId()));fails(()->store.confirmCourierDelivery(delivery.getId(),"PROOF-001",customer));
        store.confirmCourierDelivery(delivery.getId(),"PROOF-001",staff);check(delivery.getCourierReference().equals("PROOF-001")&&delivery.getStatus()==OrderStatus.DELIVERED&&delivery.getCompletedAt()==null,"Courier proof records delivery without completing");ActivityLogger.setActor(customer);store.completeOrder(delivery.getId());ActivityLogger.setActor(staff);
        check(!delivery.getControlNumber().equals(order.getControlNumber()),"Control numbers unique across orders");
        for(int i=0;i<5;i++)auth.login(customer.getUsername(),"Incorrect123!".toCharArray());check(SecurityIncidents.incidents().stream().anyMatch(i->i.userId()==customer.getId()),"Repeated failed login creates a reviewable warning");
        store.addToCart(customer.getId(),large.getId());Order active=store.checkout(customer.getId(),CheckoutDetails.structured("Maria Cruz",customer.getEmail(),"+639123456789","Pickup",null,"GCash",""));
        ActivityLogger.setActor(customer);fails(()->store.advanceOrder(active.getId()));ActivityLogger.setActor(admin);
        SwingUtilities.invokeAndWait(()->{try{
            OrderTrackingPanel tracking=new OrderTrackingPanel(customer);JComboBox<?> selector=(JComboBox<?>)field(tracking,"selector");check(selector.getItemCount()==1&&selector.getSelectedItem()==active,"Completed orders removed from Track Order selector");
            OrderHistoryPanel history=new OrderHistoryPanel(customer);JTable historyTable=(JTable)field(history,"table");check(historyTable.getRowCount()==2&&historyTable.getValueAt(0,0).equals(order.getControlNumber()),"History contains completed orders with control numbers");
            DefaultTableModel records=new DefaultTableModel(new String[]{"Number","Name"},0){public Class<?> getColumnClass(int c){return c==0?Integer.class:String.class;}};
            for(int i=1;i<=25;i++)records.addRow(new Object[]{i,"Record "+i});JTable table=new JTable(records);JPanel page=new JPanel(new BorderLayout());page.add(new ModernScrollPane(table));
            check(table.getRowCount()==10&&TablePagination.matchingCount(table)==25,"Ten visible rows and all matching records available");
            JPanel controls=(JPanel)table.getClientProperty("pagination.controls");((JButton)controls.getComponent(2)).doClick();check(table.getRowCount()==10&&table.getValueAt(0,0).equals(11),"Next page maps to correct records");
            AdminTableReport report=new AdminTableReport(table,"All filtered records",admin);check(report.rows==25,"Printing ignores current table page");
            table.getRowSorter().toggleSortOrder(0);table.getRowSorter().toggleSortOrder(0);check(TablePagination.matchingRows(table).get(0)==24,"Pagination sorts the whole filtered result");
            ((TableRowSorter<?>)table.getRowSorter()).setRowFilter(RowFilter.regexFilter("Record (2[0-5])$",1));check(table.getRowCount()==6&&TablePagination.matchingCount(table)==6,"Search operates across all pages and resets pagination");
            render(tracking,"tracking");render(history,"history");render(new ReturnsPanel(admin),"returns");render(new CustomerManagementPanel(auth,admin),"users");render(new ProductManagementPanel(admin),"stock");render(page,"pagination");
            java.lang.reflect.Field logs=ActivityLogger.class.getDeclaredField("entries");logs.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.List<ActivityLogger.Entry> entries=(java.util.List<ActivityLogger.Entry>)logs.get(null);
            entries.add(new ActivityLogger.Entry(java.time.LocalDateTime.now().minusDays(2),admin.getUsername(),"ADMIN","Historical change","Date-filter fixture",admin.getId()));
            ActivityLogPanel activity=new ActivityLogPanel();((JCheckBox)field(activity,"allDates")).doClick();
            java.util.Date oldDate=java.util.Date.from(LocalDate.now().minusDays(2).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            ((JSpinner)field(activity,"from")).setValue(oldDate);((JSpinner)field(activity,"to")).setValue(oldDate);
            check(TablePagination.matchingCount((JTable)field(activity,"table"))==1,"Activity date range filters the full result");
            ((JSpinner)field(activity,"from")).setValue(new java.util.Date());check(((JTable)field(activity,"table")).getRowCount()==0&&!((JButton)field(activity,"print")).isEnabled(),"Invalid activity date range cannot print records");
            ((JCheckBox)field(activity,"allDates")).doClick();render(activity,"activity");
            java.util.List<Product> defaults=store.getProducts().subList(0,12);for(Product product:defaults)store.addToCart(other.getId(),product.getId());
            CartPanel cart=new CartPanel(other,()->{});JTable cartTable=(JTable)field(cart,"table");JPanel cartPages=(JPanel)cartTable.getClientProperty("pagination.controls");((JButton)cartPages.getComponent(2)).doClick();
            int target=store.getCart(other.getId()).get(cartTable.convertRowIndexToModel(0)).getProduct().getId();int first=store.getCart(other.getId()).get(0).getProduct().getId();cartTable.setRowSelectionInterval(0,0);
            var remove=CartPanel.class.getDeclaredMethod("removeSelected");remove.setAccessible(true);remove.invoke(cart);
            check(store.getCart(other.getId()).stream().noneMatch(i->i.getProduct().getId()==target)&&store.getCart(other.getId()).stream().anyMatch(i->i.getProduct().getId()==first),"Cart removal uses model identity on later pages");
            render(new gui.staff.StaffDashboardPanel(auth,staff),"staff-dashboard");
            render(new AdminDashboardPanel(auth,admin),"admin-dashboard");
        }catch(Exception ex){throw new RuntimeException(ex);}});
        System.out.println("PASS: signup email/age, per-size stock, random controls, completed history, courier proof, evidence-based returns/refunds, warnings, and 10-row pagination with full reports");
    }
}
