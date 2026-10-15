package repository;

import database.Database;
import java.sql.*;
import java.util.*;
import java.time.LocalDateTime;
import model.*;
import service.ActivityLogger;

/** Database-backed catalog, carts and queue; stock and checkout use transactions. */
public final class JdbcStoreRepository {
    private final Database database;
    public JdbcStoreRepository(Database database) { this.database = database; }
    private static int generated(PreparedStatement s) throws SQLException {
        try (ResultSet r = s.getGeneratedKeys()) { if (!r.next()) throw new SQLException("Missing generated ID"); return r.getInt(1); }
    }
    private static Product product(ResultSet r) throws SQLException {
        return new Product(r.getInt("id"),r.getString("name"),r.getString("category"),r.getString("subcategory"),
                r.getBigDecimal("price").doubleValue(),r.getInt("stock"),r.getString("image_path"),r.getString("size"));
    }
    private static Product product(Connection c,int id,boolean lock) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM products WHERE id=?" + (lock ? " FOR UPDATE" : ""))) {
            s.setInt(1,id);try (ResultSet r=s.executeQuery()) { if (!r.next()) throw new IllegalArgumentException("Product was not found.");return product(r); }
        }
    }
    public List<Product> products() { return database.read(c -> {
        List<Product> result=new ArrayList<>();
        try (Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM products ORDER BY id")) { while(r.next())result.add(product(r)); }
        return result;
    }); }
    public Optional<Product> findProduct(int id) { return products().stream().filter(p->p.getId()==id).findFirst(); }
    private static Product insertProduct(Connection c,Product product) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("INSERT INTO products (name,category,subcategory,price,stock,image_path,size) VALUES (?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1,product.getName());s.setString(2,product.getCategory());s.setString(3,product.getSubcategory());
            s.setBigDecimal(4,java.math.BigDecimal.valueOf(product.getPrice()));s.setInt(5,product.getStock());s.setString(6,product.getImagePath());s.setString(7,product.getSize());s.executeUpdate();
            return new Product(generated(s),product.getName(),product.getCategory(),product.getSubcategory(),product.getPrice(),product.getStock(),product.getImagePath(),product.getSize());
        }
    }
    public void seedCatalog(List<Product> defaults) { database.transaction(c->{
        try (Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM products")) {r.next();if(r.getInt(1)>0)return null;}
        for(Product p:defaults)insertProduct(c,p);return null;
    }); }
    public Product addProduct(String name,String category,String subcategory,double price,int stock,String imagePath) { return addProduct(name,category,subcategory,price,stock,imagePath,"Accessories".equals(category)?"One Size":"M"); }
    public Product addProduct(String name,String category,String subcategory,double price,int stock,String imagePath,String size) {
        if(name==null||name.trim().isEmpty())throw new IllegalArgumentException("Product name is required.");
        if(!Double.isFinite(price)||price<0||stock<0)throw new IllegalArgumentException("Price and stock cannot be negative.");
        return database.transaction(c->{
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT GET_LOCK('queuetees_catalog',10)")){r.next();if(r.getInt(1)!=1)throw new IllegalStateException("Catalog is busy. Try again.");}
            try {
            try(PreparedStatement s=c.prepareStatement("SELECT id FROM products WHERE name=? AND size=? LIMIT 1")){s.setString(1,name.trim());s.setString(2,size);try(ResultSet r=s.executeQuery()){if(r.next())throw new IllegalArgumentException("That product size already exists. Replenish its stock instead.");}}
            Product p=insertProduct(c,new Product(0,name.trim(),category,subcategory,price,stock,imagePath==null?"":imagePath.trim(),size));
            JdbcActivityRepository.record(c,ActivityLogger.getActor(),"Product added",p.getDisplayName()+" (#"+p.getId()+"), initial stock: "+stock);c.commit();return p;
            } finally{try(Statement s=c.createStatement()){s.execute("SELECT RELEASE_LOCK('queuetees_catalog')");}}
        });
    }
    public Product adjustStock(int id,int delta,User actor) {
        if(actor==null || actor.getRole()!=UserRole.ADMIN || actor.getStatus()!=AccountStatus.ACTIVE)throw new IllegalStateException("Administrator access is required to replenish stock.");
        if(delta<=0)throw new IllegalArgumentException("Manual stock decreases are prohibited. Enter a positive replenishment.");
        return database.transaction(c->{
            Product p=product(c,id,true);int before=p.getStock();long after=(long)before+delta;
            try(PreparedStatement s=c.prepareStatement("SELECT id FROM users WHERE id=? AND role='ADMIN' AND status='ACTIVE'")){s.setInt(1,actor.getId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Active administrator access is required.");}}
            if(after<0||after>Integer.MAX_VALUE)throw new IllegalArgumentException("Stock must stay between 0 and "+Integer.MAX_VALUE+".");
            try(PreparedStatement s=c.prepareStatement("UPDATE products SET stock=? WHERE id=?")){s.setInt(1,(int)after);s.setInt(2,id);s.executeUpdate();}
            p.setStock((int)after);JdbcActivityRepository.record(c,actor,"Stock adjusted",p.getName()+" (#"+id+"): "+before+" → "+after+" ("+(delta>0?"+":"")+delta+")");return p;
        });
    }
    private static void lockCustomer(Connection c,int id) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT role,status,email_verified,terms_required,terms_version,terms_accepted_at FROM users WHERE id=? FOR UPDATE")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){
                if(!r.next()||!r.getString(1).equals("CUSTOMER")||!r.getString(2).equals("ACTIVE"))throw new IllegalStateException("An active customer account is required.");
                if(!r.getBoolean("email_verified"))throw new IllegalStateException("Verify your email before shopping.");
                if(r.getBoolean("terms_required") && (!service.AuthService.TERMS_VERSION.equals(r.getString("terms_version")) || r.getTimestamp("terms_accepted_at")==null))
                    throw new IllegalStateException("Accept your registration terms before shopping.");
            }
        }
    }
    public void addToCart(int customerId,int productId,int amount) {
        if(amount<1)throw new IllegalArgumentException("Quantity must be at least 1.");
        database.transaction(c->{
            lockCustomer(c,customerId);Product p=product(c,productId,true);int current=0;
            try(PreparedStatement s=c.prepareStatement("SELECT quantity FROM cart_items WHERE customer_id=? AND product_id=?")){
                s.setInt(1,customerId);s.setInt(2,productId);try(ResultSet r=s.executeQuery()){if(r.next())current=r.getInt(1);}
            }
            long quantity=(long)current+amount;
            if(quantity>p.getStock())throw new IllegalStateException("Only "+Math.max(0,p.getStock()-current)+" more of this item can be added.");
            try(PreparedStatement s=c.prepareStatement("INSERT INTO cart_items (customer_id,product_id,quantity) VALUES (?,?,?) ON DUPLICATE KEY UPDATE quantity=?")){
                s.setInt(1,customerId);s.setInt(2,productId);s.setInt(3,(int)quantity);s.setInt(4,(int)quantity);s.executeUpdate();
            }return null;
        });
    }
    private static List<CartItem> cart(Connection c,int customer) throws SQLException {
        List<CartItem> result=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement("SELECT p.*,ci.quantity FROM cart_items ci JOIN products p ON p.id=ci.product_id WHERE ci.customer_id=? ORDER BY p.id")){
            s.setInt(1,customer);try(ResultSet r=s.executeQuery()){while(r.next())result.add(new CartItem(product(r),r.getInt("quantity")));}
        }return result;
    }
    public List<CartItem> cart(int customer) { return database.read(c->cart(c,customer)); }
    public void removeFromCart(int customer,int product) { database.transaction(c->{
        lockCustomer(c,customer);
        try(PreparedStatement s=c.prepareStatement("DELETE FROM cart_items WHERE customer_id=? AND product_id=?")){
            s.setInt(1,customer);s.setInt(2,product);s.executeUpdate();
        }return null;
    }); }
    public Order checkout(int customer,CheckoutDetails details) {
        if(details==null)throw new IllegalArgumentException("Checkout details are required.");
        details.validateFulfillment();
        if(details==null||details.getFullName()==null||details.getFullName().isBlank())throw new IllegalArgumentException("Full name is required.");
        if(details.getContactNumber()==null||details.getContactNumber().isBlank())throw new IllegalArgumentException("Contact number is required.");
        if("Delivery".equals(details.getFulfillmentMethod())&&(details.getAddress()==null||details.getAddress().isBlank()))throw new IllegalArgumentException("Delivery address is required.");
        return database.transaction(c->{
            lockCustomer(c,customer);List<CartItem> items=cart(c,customer);
            if(items.isEmpty())throw new IllegalStateException("Your cart is empty.");
            // Lock products in ascending ID order to serialize stock reservations.
            for(CartItem item:items){
                Product p=product(c,item.getProduct().getId(),true);
                if(item.getQuantity()>p.getStock())throw new IllegalStateException(p.getName()+" no longer has enough stock.");
                item.getProduct().setPrice(p.getPrice());item.getProduct().setName(p.getName());item.getProduct().setStock(p.getStock()-item.getQuantity());
                item.getProduct().setCategory(p.getCategory());item.getProduct().setSubcategory(p.getSubcategory());item.getProduct().setImagePath(p.getImagePath());
                try(PreparedStatement s=c.prepareStatement("UPDATE products SET stock=stock-? WHERE id=?")){s.setInt(1,item.getQuantity());s.setInt(2,p.getId());s.executeUpdate();}
            }
            LocalDateTime now=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);int id;String control=Order.newControlNumber();
            // Named lock prevents separate clients allocating the same next queue number.
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT GET_LOCK('queuetees_queue',10)")){r.next();if(r.getInt(1)!=1)throw new IllegalStateException("Queue is busy. Try checkout again.");}
            try{
                int queue;
                try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COALESCE(MAX(queue_number),0)+1 FROM orders")){r.next();queue=r.getInt(1);}
                try(PreparedStatement s=c.prepareStatement("INSERT INTO orders (queue_number,customer_id,customer_name,checkout_full_name,checkout_email,checkout_contact_number,fulfillment_method,delivery_address,payment_method,notes,placed_at,address_province,address_city,address_barangay,address_street,control_number,payment_status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    s.setInt(1,queue);s.setInt(2,customer);s.setString(3,details.getFullName().trim());s.setString(4,details.getFullName());
                    s.setString(5,Objects.toString(details.getEmail(),""));s.setString(6,details.getContactNumber());s.setString(7,details.getFulfillmentMethod());
                    s.setString(8,Objects.toString(details.getAddress(),""));s.setString(9,details.getPaymentMethod());s.setString(10,Objects.toString(details.getNotes(),""));s.setObject(11,now); DeliveryAddress a=details.getStructuredAddress();s.setString(12,a==null?null:a.getProvince());s.setString(13,a==null?null:a.getCity());s.setString(14,a==null?null:a.getBarangay());s.setString(15,a==null?null:a.getStreet());s.setString(16,control);s.setString(17,"Cash".equalsIgnoreCase(details.getPaymentMethod())?"CASH_ON_DELIVERY":"PENDING");s.executeUpdate();id=generated(s);
                }
                for(CartItem item:items){Product p=item.getProduct();
                    try(PreparedStatement s=c.prepareStatement("INSERT INTO order_items (order_id,product_id,product_name,product_category,product_subcategory,product_image_path,product_stock_at_purchase,unit_price,quantity,product_size) VALUES (?,?,?,?,?,?,?,?,?,?)")){
                        s.setInt(1,id);s.setInt(2,p.getId());s.setString(3,p.getName());s.setString(4,p.getCategory());s.setString(5,p.getSubcategory());
                        s.setString(6,p.getImagePath());s.setInt(7,p.getStock());s.setBigDecimal(8,java.math.BigDecimal.valueOf(p.getPrice()));s.setInt(9,item.getQuantity());s.setString(10,p.getSize());s.executeUpdate();
                    }
                }
                try(PreparedStatement s=c.prepareStatement("DELETE FROM cart_items WHERE customer_id=?")){s.setInt(1,customer);s.executeUpdate();}
                // Commit before releasing the allocation lock so the next client sees this number.
                c.commit();return new Order(id,queue,customer,details.getFullName().trim(),items,now,details).restoreControlNumber(control);
            }finally{try(Statement s=c.createStatement()){s.execute("SELECT RELEASE_LOCK('queuetees_queue')");}}
        });
    }
    private static List<CartItem> orderItems(Connection c,int id) throws SQLException {
        List<CartItem> result=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM order_items WHERE order_id=? ORDER BY id")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){while(r.next()){
                Product p=new Product(r.getInt("product_id"),r.getString("product_name"),r.getString("product_category"),r.getString("product_subcategory"),
                        r.getBigDecimal("unit_price").doubleValue(),r.getInt("product_stock_at_purchase"),r.getString("product_image_path"),r.getString("product_size"));
                result.add(new CartItem(p,r.getInt("quantity")));
            }}
        }return result;
    }
    private static Order order(Connection c,ResultSet r) throws SQLException {
        CheckoutDetails d=new CheckoutDetails(r.getString("checkout_full_name"),r.getString("checkout_email"),r.getString("checkout_contact_number"),
                r.getString("fulfillment_method"),r.getString("delivery_address"),r.getString("payment_method"),r.getString("notes"));
        if(r.getString("address_province")!=null) d=CheckoutDetails.structured(d.getFullName(),d.getEmail(),d.getContactNumber(),d.getFulfillmentMethod(),new DeliveryAddress(r.getString("address_province"),r.getString("address_city"),r.getString("address_barangay"),r.getString("address_street")),d.getPaymentMethod(),d.getNotes());
        int id=r.getInt("id");return Order.restore(id,r.getInt("queue_number"),r.getInt("customer_id"),r.getString("customer_name"),orderItems(c,id),
                r.getObject("placed_at",LocalDateTime.class),d,OrderStatus.valueOf(r.getString("status")),r.getObject("completed_at",LocalDateTime.class)).restoreControlNumber(r.getString("control_number")).restoreCourier(r.getString("courier_reference"),r.getObject("courier_confirmed_at",LocalDateTime.class)).restoreAcknowledgment(r.getObject("delivered_by",Integer.class),r.getObject("received_by",Integer.class),r.getString("delivery_issue")).restoreIssueReview(r.getString("delivery_issue_status"),r.getString("delivery_issue_response"),r.getObject("delivery_issue_reviewer",Integer.class)).restorePayment(PaymentStatus.valueOf(r.getString("payment_status")),r.getDouble("paid_amount"),r.getString("payment_reference"));
    }
    public List<Order> orders() { return database.read(c->{
        List<Order> result=new ArrayList<>();try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM orders ORDER BY queue_number")){while(r.next())result.add(order(c,r));}return result;
    }); }
    public void verifyPayment(int id,double amount,String reference,User actor){database.transaction(c->{
        Order existing;try(PreparedStatement s=c.prepareStatement("SELECT * FROM orders WHERE id=? FOR UPDATE")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Order not found.");existing=order(c,r);}}
        service.PaymentVerification.validate(actor,existing,amount,reference);
        try(PreparedStatement s=c.prepareStatement("SELECT id FROM users WHERE id=? AND role IN ('STAFF','ADMIN') AND status='ACTIVE' FOR UPDATE")){s.setInt(1,actor.getId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Account access changed.");}}
        try(PreparedStatement s=c.prepareStatement("UPDATE orders SET payment_status='VERIFIED',paid_amount=?,payment_reference=? WHERE id=? AND payment_status<>'VERIFIED'")){
            s.setBigDecimal(1,java.math.BigDecimal.valueOf(amount));s.setString(2,reference.trim());s.setInt(3,id);if(s.executeUpdate()!=1)throw new IllegalStateException("Payment changed. Refresh first.");
        }JdbcActivityRepository.record(c,actor,"Payment verified",existing.getControlNumber()+": "+reference.trim());return null;
    });}
    public void assignDeliveryAddress(int id, DeliveryAddress address) { database.transaction(c -> {
        Order existing;
        try (PreparedStatement s=c.prepareStatement("SELECT * FROM orders WHERE id=? FOR UPDATE")) {
            s.setInt(1,id); try(ResultSet r=s.executeQuery()) {
                if(!r.next())throw new IllegalArgumentException("Order was not found."); existing=order(c,r);
            }
        }
        service.OrderAccess.database(c,ActivityLogger.getActor(),existing,false);
        existing.assignDeliveryAddress(address);
        try(PreparedStatement s=c.prepareStatement("UPDATE orders SET delivery_address=?,address_province=?,address_city=?,address_barangay=?,address_street=? WHERE id=?")) {
            s.setString(1,address.format()); s.setString(2,address.getProvince()); s.setString(3,address.getCity());
            s.setString(4,address.getBarangay()); s.setString(5,address.getStreet()); s.setInt(6,id); s.executeUpdate();
        }
        JdbcActivityRepository.record(c,ActivityLogger.getActor(),"Delivery address reviewed","Q-"+existing.getQueueNumber()+": "+address.format());
        return null;
    }); }
    public Order confirmCourierDelivery(int id,String reference,User actor) { return database.transaction(c -> {
        Order existing;
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM orders WHERE id=? FOR UPDATE")) {
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Order was not found.");existing=order(c,r);}
        }
        service.CourierConfirmation.validate(actor,reference,existing);
        try(PreparedStatement s=c.prepareStatement("SELECT id FROM users WHERE id=? AND role IN ('ADMIN','STAFF') AND status='ACTIVE'")) {
            s.setInt(1,actor.getId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Active staff access is required.");}
        }
        LocalDateTime now=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        try(PreparedStatement s=c.prepareStatement("UPDATE orders SET status='DELIVERED',courier_reference=?,courier_confirmed_at=?,delivered_by=? WHERE id=?")) {
            s.setString(1,reference.trim());s.setObject(2,now);s.setInt(3,actor.getId());s.setInt(4,id);s.executeUpdate();
        }
        JdbcActivityRepository.record(c,actor,"Courier delivery confirmed",existing.getControlNumber()+": "+reference.trim());
        return Order.restore(existing.getId(),existing.getQueueNumber(),existing.getCustomerId(),existing.getCustomerName(),existing.getItems(),existing.getPlacedAt(),existing.getCheckoutDetails(),OrderStatus.DELIVERED,null)
            .restoreControlNumber(existing.getControlNumber()).restoreCourier(reference.trim(),now).restoreAcknowledgment(actor.getId(),null,existing.getDeliveryIssue()).restoreIssueReview(existing.getDeliveryIssueStatus(),existing.getDeliveryIssueResponse(),existing.getDeliveryIssueReviewer()).copyPaymentFrom(existing);
    }); }
    public Order advanceTo(int id,OrderStatus expected,OrderStatus target){return advance(id,expected,false,target);}
    public Order advance(int id,OrderStatus expected,boolean complete){return advance(id,expected,complete,null);}
    private Order advance(int id,OrderStatus expected,boolean complete,OrderStatus target) { return database.transaction(c->{
        // Serialize preparation decisions across app instances, then lock the chosen order.
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT GET_LOCK('queuetees_preparation',10)")){r.next();if(r.getInt(1)!=1)throw new IllegalStateException("Queue is busy. Try again.");}
        try{
            Order order;
            try(PreparedStatement s=c.prepareStatement("SELECT * FROM orders WHERE id=? FOR UPDATE")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Order was not found.");order=order(c,r);}}
            OrderStatus before=order.getStatus();
            service.OrderAccess.database(c,ActivityLogger.getActor(),order,complete);
            if(expected!=null&&before!=expected)throw new IllegalStateException("This order has changed. Review its current status and try again.");
            if(before==OrderStatus.COMPLETED)throw new IllegalStateException("This order is already completed.");
            if(complete&&!Fulfillment.canConfirmReceipt(order))throw new IllegalStateException("Order cannot be completed at this stage.");
            if(complete&&!order.getCheckoutDetails().isPickup()&&ActivityLogger.getActor().getRole()!=UserRole.CUSTOMER)throw new IllegalStateException("Staff delivery completion requires recorded courier confirmation.");
            if(!complete&&before==OrderStatus.OUT_FOR_DELIVERY)throw new IllegalStateException("This order is out for delivery. Waiting for customer to confirm receipt.");
            if(before==OrderStatus.CONFIRMED){
                if(!order.canStartPacking())throw new IllegalStateException("Verify payment before starting preparation.");
                try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT o.id FROM orders o JOIN order_items i ON i.order_id=o.id WHERE o.status='CONFIRMED' AND o.payment_status IN ('VERIFIED','CASH_ON_DELIVERY') GROUP BY o.id,o.placed_at,o.queue_number ORDER BY SUM(i.quantity),o.placed_at,o.queue_number,o.id LIMIT 1")){
                    if(!r.next() || r.getInt(1)!=order.getId())throw new IllegalStateException("Start the waiting order with the fewest pieces first.");
                }
            }
            OrderStatus after=target==null?Fulfillment.next(order,complete):OrderWorkflow.target(order,target);
            if(after==OrderStatus.OUT_FOR_DELIVERY){List<Order> ready=new ArrayList<>();
                try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM orders WHERE status IN ('READY_FOR_PICKUP','AT_DELIVERY_POINT')")){while(r.next())ready.add(order(c,r));}
                DeliveryPriority.requireNearest(ready,order);
            }
            LocalDateTime completed=after==OrderStatus.COMPLETED?LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS):null;
            if(after==OrderStatus.COMPLETED && !order.getDeliveryIssue().isBlank())try(PreparedStatement s=c.prepareStatement("UPDATE orders SET delivery_issue_status='RESOLVED',delivery_issue_response=CONCAT(COALESCE(delivery_issue_response,''),' / Customer acknowledged receipt.') WHERE id=?")){s.setInt(1,id);s.executeUpdate();}
            Integer receivedBy=after==OrderStatus.COMPLETED?Integer.valueOf(ActivityLogger.getActor().getId()):order.getReceivedBy();
            try(PreparedStatement s=c.prepareStatement("UPDATE orders SET status=?,completed_at=?,received_by=? WHERE id=?")){s.setString(1,after.name());s.setObject(2,completed);s.setObject(3,receivedBy);s.setInt(4,id);s.executeUpdate();}
            JdbcActivityRepository.record(c,ActivityLogger.getActor(),after==OrderStatus.COMPLETED?"Order completed":"Order updated",order.getControlNumber()+": "+order.getStatusLabel()+" → "+(after==OrderStatus.READY_FOR_PICKUP&&!order.getCheckoutDetails().isPickup()?"Ready for dispatch":after.getLabel()));
            c.commit();return Order.restore(order.getId(),order.getQueueNumber(),order.getCustomerId(),order.getCustomerName(),order.getItems(),order.getPlacedAt(),order.getCheckoutDetails(),after,completed).restoreControlNumber(order.getControlNumber()).restoreCourier(order.getCourierReference(),order.getCourierConfirmedAt()).restoreAcknowledgment(order.getDeliveredBy(),receivedBy,order.getDeliveryIssue()).restoreIssueReview(after==OrderStatus.COMPLETED&&!order.getDeliveryIssue().isBlank()?"RESOLVED":order.getDeliveryIssueStatus(),order.getDeliveryIssueResponse(),order.getDeliveryIssueReviewer()).copyPaymentFrom(order);
        }finally{try(Statement s=c.createStatement()){s.execute("SELECT RELEASE_LOCK('queuetees_preparation')");}}
    }); }
    public void reportDeliveryIssue(int id,String note,User actor) { database.transaction(c->{
        Order order;
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM orders WHERE id=? FOR UPDATE")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Order was not found.");order=order(c,r);}
        }
        service.OrderAccess.database(c,actor,order,true);
        if(actor.getRole()!=UserRole.CUSTOMER || order.getStatus()!=OrderStatus.DELIVERED)
            throw new IllegalStateException("Report non-receipt while awaiting acknowledgment.");
        if(java.util.Set.of("OPEN","REVIEWING").contains(order.getDeliveryIssueStatus()))throw new IllegalStateException("Your report is already awaiting staff review.");
        try(PreparedStatement s=c.prepareStatement("UPDATE orders SET delivery_issue=?,delivery_issue_status='OPEN',delivery_issue_response=NULL,delivery_issue_reviewer=NULL WHERE id=?")){s.setString(1,note);s.setInt(2,id);s.executeUpdate();}
        return null;
    }); }
    public void reviewDeliveryIssue(int id,String outcome,String note,User actor) {database.transaction(c->{
        service.DeliveryIssueReview.validate(actor,outcome,note);
        Order order;
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM orders WHERE id=? FOR UPDATE")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Order was not found.");order=order(c,r);}}
        service.OrderAccess.database(c,actor,order,false);service.DeliveryIssueReview.checkOrder(order);
        try(PreparedStatement s=c.prepareStatement("UPDATE orders SET delivery_issue_status=?,delivery_issue_response=?,delivery_issue_reviewer=? WHERE id=?")){s.setString(1,outcome);s.setString(2,note);s.setInt(3,actor.getId());s.setInt(4,id);s.executeUpdate();}
        if(outcome.equals("REDELIVERY"))try(PreparedStatement s=c.prepareStatement("UPDATE orders SET status='OUT_FOR_DELIVERY',courier_reference=NULL,courier_confirmed_at=NULL WHERE id=?")){s.setInt(1,id);s.executeUpdate();}
        if(outcome.equals("LOST")){
            service.RefundService.request(c,order,"ORDER:"+id,"LOST",service.RefundService.paidAmount(order),note);
            try(PreparedStatement s=c.prepareStatement("UPDATE orders SET status='LOST' WHERE id=?")){s.setInt(1,id);s.executeUpdate();}
        }
        JdbcActivityRepository.record(c,actor,"Delivery issue reviewed",order.getControlNumber()+": "+outcome+" / "+note);return null;
    });}
    public void cancelOrder(int id,String reason,User actor){database.transaction(c->{
        Order order;
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM orders WHERE id=? FOR UPDATE")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Order was not found.");order=order(c,r);}}
        // Locks are shared with preparation; whichever commits first determines eligibility.
        service.OrderAccess.database(c,actor,order,true);
        if(actor.getRole()!=UserRole.CUSTOMER||order.getCustomerId()!=actor.getId()||order.getStatus()!=OrderStatus.CONFIRMED)throw new IllegalStateException("Only your order waiting for preparation can be cancelled.");
        List<CartItem> items=new ArrayList<>(order.getItems());items.sort(Comparator.comparingInt(i->i.getProduct().getId()));
        for(CartItem item:items){Product p=product(c,item.getProduct().getId(),true);long stock=(long)p.getStock()+item.getQuantity();if(stock>Integer.MAX_VALUE)throw new IllegalStateException("Stock restoration would overflow.");try(PreparedStatement s=c.prepareStatement("UPDATE products SET stock=? WHERE id=?")){s.setLong(1,stock);s.setInt(2,p.getId());s.executeUpdate();}}
        service.RefundService.request(c,order,"ORDER:"+id,"CANCELLATION",service.RefundService.paidAmount(order),reason);
        try(PreparedStatement s=c.prepareStatement("UPDATE orders SET status='CANCELLED',delivery_issue_status='CANCELLED',delivery_issue_response=?,delivery_issue_reviewer=? WHERE id=?")){s.setString(1,LocalDateTime.now()+" / "+reason);s.setInt(2,actor.getId());s.setInt(3,id);s.executeUpdate();}
        return null;
    });}
}
