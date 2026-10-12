import database.Database;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import model.*;
import repository.*;
import service.*;

/** Runs only against the isolated scratch server created by scripts/test-mysql.ps1. */
public final class MySqlIntegrationTest {
    static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    static void fail(Runnable action,String message) { try {action.run();throw new AssertionError(message);}catch(IllegalStateException expected){} }
    static User account(JdbcUserRepository users,String name,UserRole role) {
        User user=new User(0,name,name+"@example.invalid",PasswordUtil.hashPassword("Password123!"),role,true,AccountStatus.ACTIVE,
                name,"Manila","9123456789","Other",LocalDate.of(1999,1,2),LocalDateTime.of(2020,2,3,4,5,6));
        users.save(user);return user;
    }
    static CheckoutDetails checkout(String name) {return new CheckoutDetails(name,name+"@example.invalid","+639123456789","Delivery","Manila","Card ending 1234","Test notes");}
    public static void main(String[] args)throws Exception {
        check(args.length==1&&args[0].equals("isolated-3307"),"Use the isolated test server; never run on the user's database.");
        Database setup=new Database("jdbc:mysql://127.0.0.1:3307/","root",new char[0]);
        String schema=Files.readString(Path.of("database/queuetees.sql")).replace("queuetees","queuetees_test");
        setup.read(c->{for(String sql:schema.replaceAll("(?m)^--.*$","").split(";"))if(!sql.isBlank())try(Statement s=c.createStatement()){s.execute(sql);}return null;});
        Database db=new Database("jdbc:mysql://127.0.0.1:3307/queuetees_test","root",new char[0]);db.verifySchema();
        String seed=Files.readString(Path.of("database/seed-defense-users.sql")).replace("USE queuetees;","USE queuetees_test;");
        db.read(c->{for(int pass=0;pass<2;pass++)for(String sql:seed.replaceAll("(?m)^--.*$","").split(";"))if(!sql.isBlank())try(Statement s=c.createStatement()){s.execute(sql);}return null;});
        JdbcUserRepository users=new JdbcUserRepository(db);
        check(users.findAll().size()==3,"Defense account seed is idempotent");
        for(String[] defense:new String[][]{{"admin","Admin123!"},{"staff","Staff123!"},{"verifiedcustomer","Customer123!"}}){
            User u=users.findByEmailOrUsername(defense[0]).orElseThrow();
            check(u.isEmailVerified()&&u.getStatus()==AccountStatus.ACTIVE&&PasswordUtil.verifyPassword(defense[1],u.getPasswordHash()),"Defense credentials use compatible password hashes");
        }
        User admin=account(users,"admin-db",UserRole.ADMIN),staff=account(users,"staff-db",UserRole.STAFF),customer=account(users,"customer-db",UserRole.CUSTOMER);
        check(admin.getId()>0&&customer.getId()!=admin.getId(),"Generated user IDs");
        JdbcUserRepository reloadedUsers=new JdbcUserRepository(db);
        check(reloadedUsers.findByEmailOrUsername("ADMIN-DB").orElseThrow().getRegisteredAt().equals(admin.getRegisteredAt()),"Registration timestamp hydrates");
        ActivityLogger.useDatabase(db);AuthService auth=new AuthService(users);
        auth.changePassword(admin,"Password123!".toCharArray(),"Changed123!".toCharArray(),"Changed123!".toCharArray());
        check(PasswordUtil.verifyPassword("Changed123!",reloadedUsers.findByEmailOrUsername("admin-db").orElseThrow().getPasswordHash()),"Password change persists");
        auth.suspendUser(admin,staff.getId(),true);
        check(reloadedUsers.findByEmailOrUsername("staff-db").orElseThrow().getStatus()==AccountStatus.SUSPENDED,"Suspension persists with loaded admin identity");
        auth.suspendUser(admin,staff.getId(),false);
        staff.setUsername("staff-renamed");auth.updateUser(staff);
        check(reloadedUsers.findByEmailOrUsername("staff-renamed").isPresent(),"Profile persists");
        customer.setEmailVerified(false);users.save(customer);auth.verifyEmail(customer);
        check(reloadedUsers.findByEmailOrUsername("customer-db").orElseThrow().isEmailVerified(),"Email verification persists");
        String[] resetCode={null};PasswordResetService reset=new PasswordResetService(users,(email,code)->resetCode[0]=code);
        String resetToken=reset.request(customer.getEmail());reset.verify(resetToken,resetCode[0]);reset.reset(resetToken,"Reset123!".toCharArray(),"Reset123!".toCharArray());
        check(PasswordUtil.verifyPassword("Reset123!",reloadedUsers.findByEmailOrUsername("customer-db").orElseThrow().getPasswordHash()),"Reset password persists");
        ActivityLogger.setActor(admin);JdbcStoreRepository store=new JdbcStoreRepository(db);
        Product product=store.addProduct("Purchased shirt","Men","Tops",123.45,10,"shirt.png");
        store.addToCart(customer.getId(),product.getId(),2);
        JdbcStoreRepository afterRestart=new JdbcStoreRepository(db);
        check(afterRestart.cart(customer.getId()).get(0).getQuantity()==2,"Cart persists across repository restart");
        Order first=afterRestart.checkout(customer.getId(),checkout("First customer"));
        check(store.cart(customer.getId()).isEmpty()&&store.findProduct(product.getId()).orElseThrow().getStock()==8,"Checkout reserves stock and clears cart");
        store.addToCart(customer.getId(),product.getId(),1);Order second=store.checkout(customer.getId(),checkout("Second customer"));
        check(second.getQueueNumber()==first.getQueueNumber()+1,"Unique increasing queue numbers");
        fail(()->store.advance(second.getId(),OrderStatus.CONFIRMED,false),"Cannot skip FIFO");
        ActivityLogger.setActor(staff);store.advance(first.getId(),OrderStatus.CONFIRMED,false);
        fail(()->store.advance(first.getId(),OrderStatus.CONFIRMED,false),"Stale update rejected");
        store.advance(first.getId(),OrderStatus.PREPARING,false);store.advance(first.getId(),OrderStatus.READY_FOR_PICKUP,false);
        ActivityLogger.setActor(customer);Order completed=store.advance(first.getId(),OrderStatus.OUT_FOR_DELIVERY,true);
        db.transaction(c->{try(PreparedStatement s=c.prepareStatement("UPDATE products SET name='Renamed catalog shirt',price=999.99 WHERE id=?")){s.setInt(1,product.getId());s.executeUpdate();}return null;});
        Order restored=new JdbcStoreRepository(db).orders().stream().filter(o->o.getId()==first.getId()).findFirst().orElseThrow();
        check(restored.getStatus()==OrderStatus.COMPLETED&&restored.getCompletedAt().equals(completed.getCompletedAt()),"Completed status/date survives restart");
        check(restored.getPlacedAt().equals(first.getPlacedAt())&&Math.abs(restored.getTotal()-246.9)<0.001&&restored.getItems().get(0).getProduct().getName().equals("Purchased shirt"),"Historical date, name and price preserved");
        User buyer2=account(users,"buyer2-db",UserRole.CUSTOMER);
        ActivityLogger.setActor(admin);Product scarce=store.addProduct("Last stock","Men","Tops",5,5,"");
        store.addToCart(customer.getId(),scarce.getId(),2);store.addToCart(buyer2.getId(),scarce.getId(),2);store.adjustStock(scarce.getId(),-2,admin);
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            List<Future<Boolean>> attempts=new ArrayList<>();
            for(User buyer:List.of(customer,buyer2))attempts.add(pool.submit(()->{start.await();try{new JdbcStoreRepository(db).checkout(buyer.getId(),checkout(buyer.getUsername()));return true;}catch(IllegalStateException ex){return false;}}));
            start.countDown();int successes=0;for(Future<Boolean> attempt:attempts)if(attempt.get(20,TimeUnit.SECONDS))successes++;
            check(successes==1&&store.findProduct(scarce.getId()).orElseThrow().getStock()==1,"Concurrent checkout cannot oversell");
            check(store.cart(customer.getId()).size()+store.cart(buyer2.getId()).size()==1,"Failed checkout keeps its cart");
        } finally {pool.shutdownNow();}
        var activity=new JdbcActivityRepository(db).entries();
        check(activity.stream().anyMatch(e->e.action().equals("Stock adjusted"))&&activity.stream().noneMatch(e->e.action().equals("UI command")),"Meaningful audit persists");
        int before=activity.size();ActivityLogger.record(admin,"Filter changed","ignored");
        check(new JdbcActivityRepository(db).entries().size()==before,"Unnecessary interactions excluded from database");
        StoreService.getInstance().useDatabase(db);
        check(StoreService.getInstance().getOrders().size()==3&&StoreService.getInstance().getProducts().size()==2,"Existing catalog is not reseeded");
        check(db.read(c->{try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM users")){r.next();return r.getInt(1);}})==7,"Updates do not duplicate users");
        System.out.println("PASS: real MySQL schema, generated IDs, account updates, restart hydration, carts, atomic checkout, FIFO, stale updates, concurrent stock, historical prices/dates and persistent audit");
    }
}
