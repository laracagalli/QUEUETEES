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
                r.getBigDecimal("price").doubleValue(),r.getInt("stock"),r.getString("image_path"));
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
        try (PreparedStatement s=c.prepareStatement("INSERT INTO products (name,category,subcategory,price,stock,image_path) VALUES (?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1,product.getName());s.setString(2,product.getCategory());s.setString(3,product.getSubcategory());
            s.setBigDecimal(4,java.math.BigDecimal.valueOf(product.getPrice()));s.setInt(5,product.getStock());s.setString(6,product.getImagePath());s.executeUpdate();
            return new Product(generated(s),product.getName(),product.getCategory(),product.getSubcategory(),product.getPrice(),product.getStock(),product.getImagePath());
        }
    }
    public void seedCatalog(List<Product> defaults) { database.transaction(c->{
        try (Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM products")) {r.next();if(r.getInt(1)>0)return null;}
        for(Product p:defaults)insertProduct(c,p);return null;
    }); }
    public Product addProduct(String name,String category,String subcategory,double price,int stock,String imagePath) {
        if(name==null||name.trim().isEmpty())throw new IllegalArgumentException("Product name is required.");
        if(!Double.isFinite(price)||price<0||stock<0)throw new IllegalArgumentException("Price and stock cannot be negative.");
        return database.transaction(c->{
            Product p=insertProduct(c,new Product(0,name.trim(),category,subcategory,price,stock,imagePath==null?"":imagePath.trim()));
            JdbcActivityRepository.record(c,ActivityLogger.getActor(),"Product added",p.getName()+" (#"+p.getId()+"), initial stock: "+stock);return p;
        });
    }
    public Product adjustStock(int id,int delta,User actor) {
        if(delta==0)throw new IllegalArgumentException("Enter a nonzero stock adjustment.");
        return database.transaction(c->{
            Product p=product(c,id,true);int before=p.getStock();long after=(long)before+delta;
            if(after<0||after>Integer.MAX_VALUE)throw new IllegalArgumentException("Stock must stay between 0 and "+Integer.MAX_VALUE+".");
            try(PreparedStatement s=c.prepareStatement("UPDATE products SET stock=? WHERE id=?")){s.setInt(1,(int)after);s.setInt(2,id);s.executeUpdate();}
            p.setStock((int)after);JdbcActivityRepository.record(c,actor,"Stock adjusted",p.getName()+" (#"+id+"): "+before+" → "+after+" ("+(delta>0?"+":"")+delta+")");return p;
        });
    }
    private static void lockCustomer(Connection c,int id) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT role,status FROM users WHERE id=? FOR UPDATE")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){
                if(!r.next()||!r.getString(1).equals("CUSTOMER")||!r.getString(2).equals("ACTIVE"))throw new IllegalStateException("An active customer account is required.");
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
            LocalDateTime now=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);int id;
            // Named lock prevents separate clients allocating the same next queue number.
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT GET_LOCK('queuetees_queue',10)")){r.next();if(r.getInt(1)!=1)throw new IllegalStateException("Queue is busy. Try checkout again.");}
            try{
                int queue;
                try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COALESCE(MAX(queue_number),0)+1 FROM orders")){r.next();queue=r.getInt(1);}
                try(PreparedStatement s=c.prepareStatement("INSERT INTO orders (queue_number,customer_id,customer_name,checkout_full_name,checkout_email,checkout_contact_number,fulfillment_method,delivery_address,payment_method,notes,placed_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    s.setInt(1,queue);s.setInt(2,customer);s.setString(3,details.getFullName().trim());s.setString(4,details.getFullName());
                    s.setString(5,Objects.toString(details.getEmail(),""));s.setString(6,details.getContactNumber());s.setString(7,details.getFulfillmentMethod());
                    s.setString(8,Objects.toString(details.getAddress(),""));s.setString(9,details.getPaymentMethod());s.setString(10,Objects.toString(details.getNotes(),""));s.setObject(11,now);s.executeUpdate();id=generated(s);
                }
                for(CartItem item:items){Product p=item.getProduct();
                    try(PreparedStatement s=c.prepareStatement("INSERT INTO order_items (order_id,product_id,product_name,product_category,product_subcategory,product_image_path,product_stock_at_purchase,unit_price,quantity) VALUES (?,?,?,?,?,?,?,?,?)")){
                        s.setInt(1,id);s.setInt(2,p.getId());s.setString(3,p.getName());s.setString(4,p.getCategory());s.setString(5,p.getSubcategory());
                        s.setString(6,p.getImagePath());s.setInt(7,p.getStock());s.setBigDecimal(8,java.math.BigDecimal.valueOf(p.getPrice()));s.setInt(9,item.getQuantity());s.executeUpdate();
                    }
                }
                try(PreparedStatement s=c.prepareStatement("DELETE FROM cart_items WHERE customer_id=?")){s.setInt(1,customer);s.executeUpdate();}
                // Commit before releasing the allocation lock so the next client sees this number.
                c.commit();return new Order(id,queue,customer,details.getFullName().trim(),items,now,details);
            }finally{try(Statement s=c.createStatement()){s.execute("SELECT RELEASE_LOCK('queuetees_queue')");}}
        });
    }
    private static List<CartItem> orderItems(Connection c,int id) throws SQLException {
        List<CartItem> result=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM order_items WHERE order_id=? ORDER BY id")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){while(r.next()){
                Product p=new Product(r.getInt("product_id"),r.getString("product_name"),r.getString("product_category"),r.getString("product_subcategory"),
                        r.getBigDecimal("unit_price").doubleValue(),r.getInt("product_stock_at_purchase"),r.getString("product_image_path"));
                result.add(new CartItem(p,r.getInt("quantity")));
            }}
        }return result;
    }
    private static Order order(Connection c,ResultSet r) throws SQLException {
        CheckoutDetails d=new CheckoutDetails(r.getString("checkout_full_name"),r.getString("checkout_email"),r.getString("checkout_contact_number"),
                r.getString("fulfillment_method"),r.getString("delivery_address"),r.getString("payment_method"),r.getString("notes"));
        int id=r.getInt("id");return Order.restore(id,r.getInt("queue_number"),r.getInt("customer_id"),r.getString("customer_name"),orderItems(c,id),
                r.getObject("placed_at",LocalDateTime.class),d,OrderStatus.valueOf(r.getString("status")),r.getObject("completed_at",LocalDateTime.class));
    }
    public List<Order> orders() { return database.read(c->{
        List<Order> result=new ArrayList<>();try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM orders ORDER BY queue_number")){while(r.next())result.add(order(c,r));}return result;
    }); }
    public Order advance(int id,OrderStatus expected,boolean complete) { return database.transaction(c->{
        // Serialize preparation decisions across app instances, then lock the chosen order.
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT GET_LOCK('queuetees_preparation',10)")){r.next();if(r.getInt(1)!=1)throw new IllegalStateException("Queue is busy. Try again.");}
        try{
            Order order;
            try(PreparedStatement s=c.prepareStatement("SELECT * FROM orders WHERE id=? FOR UPDATE")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Order was not found.");order=order(c,r);}}
            OrderStatus before=order.getStatus();
            if(expected!=null&&before!=expected)throw new IllegalStateException("This order has changed. Review its current status and try again.");
            if(before==OrderStatus.COMPLETED)throw new IllegalStateException("This order is already completed.");
            if(complete&&before!=OrderStatus.OUT_FOR_DELIVERY)throw new IllegalStateException("Order cannot be completed at this stage.");
            if(!complete&&before==OrderStatus.OUT_FOR_DELIVERY)throw new IllegalStateException("This order is out for delivery. Waiting for customer to confirm receipt.");
            if(before==OrderStatus.CONFIRMED){
                try(PreparedStatement s=c.prepareStatement("SELECT id FROM orders WHERE status='CONFIRMED' AND queue_number<? LIMIT 1")){s.setInt(1,order.getQueueNumber());try(ResultSet r=s.executeQuery()){if(r.next())throw new IllegalStateException("Start the earliest waiting order first.");}}
            }
            OrderStatus after=complete?OrderStatus.COMPLETED:switch(before){case CONFIRMED->OrderStatus.PREPARING;case PREPARING->OrderStatus.READY_FOR_PICKUP;case READY_FOR_PICKUP->OrderStatus.OUT_FOR_DELIVERY;default->throw new IllegalStateException("Invalid order status.");};
            LocalDateTime completed=complete?LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS):null;
            try(PreparedStatement s=c.prepareStatement("UPDATE orders SET status=?,completed_at=? WHERE id=?")){s.setString(1,after.name());s.setObject(2,completed);s.setInt(3,id);s.executeUpdate();}
            JdbcActivityRepository.record(c,ActivityLogger.getActor(),complete?"Order completed":"Order updated","Q-"+order.getQueueNumber()+": "+before.getLabel()+" → "+after.getLabel());
            c.commit();return Order.restore(order.getId(),order.getQueueNumber(),order.getCustomerId(),order.getCustomerName(),order.getItems(),order.getPlacedAt(),order.getCheckoutDetails(),after,completed);
        }finally{try(Statement s=c.createStatement()){s.execute("SELECT RELEASE_LOCK('queuetees_preparation')");}}
    }); }
}
