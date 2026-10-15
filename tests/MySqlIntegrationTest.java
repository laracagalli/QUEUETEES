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
    static CheckoutDetails checkout(String name) {
        String city=DeliveryAddress.cities("Pampanga")[0];
        return CheckoutDetails.structured(name,name+"@example.invalid","+639123456789","Delivery",new DeliveryAddress("Pampanga",city,DeliveryAddress.barangays("Pampanga",city)[0],"123 Test Street"),"Card ending 1234","Test notes");
    }
    static Order paid(JdbcStoreRepository store,Order order,User actor){if(order.getPaymentStatus()!=PaymentStatus.VERIFIED)store.verifyPayment(order.getId(),order.getTotal(),"PAY-"+order.getId(),actor);return store.orders().stream().filter(o->o.getId()==order.getId()).findFirst().orElseThrow();}
    static void startWaiting(JdbcStoreRepository store,User actor,int except){
        for(Order order:store.orders())if(order.getStatus()==OrderStatus.CONFIRMED && order.getId()!=except)paid(store,order,actor);
        for(Order order:PackingPriority.queue(store.orders()))if(order.getId()!=except)store.advance(order.getId(),OrderStatus.CONFIRMED,false);
    }
    static void to(JdbcStoreRepository store,Order order,OrderStatus target,User actor){paid(store,order,actor);Order current=store.orders().stream().filter(o->o.getId()==order.getId()).findFirst().orElseThrow();while(current.getStatus()!=target){store.advance(current.getId(),current.getStatus(),false);current=store.orders().stream().filter(o->o.getId()==order.getId()).findFirst().orElseThrow();}}
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
        check(!auth.hasAcceptedTerms(customer),"Existing users must accept current terms");
        auth.acceptTerms(customer,true);
        User termsReloaded=reloadedUsers.findByEmailOrUsername(customer.getEmail()).orElseThrow();
        check(auth.hasAcceptedTerms(termsReloaded) && termsReloaded.getTermsAcceptedAt().equals(customer.getTermsAcceptedAt()),"Terms version and acceptance date persist across reload");
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
        fail(()->store.advance(second.getId(),OrderStatus.CONFIRMED,false),"Cannot pack before payment verification");
        paid(store,first,admin);paid(store,second,admin);
        check(new JdbcStoreRepository(db).orders().stream().filter(o->o.getId()==first.getId()).allMatch(o->o.getPaymentStatus()==PaymentStatus.VERIFIED && Math.abs(o.getPaidAmount()-246.90)<.001),"Verified amount and payment status survive repository reload");
        ActivityLogger.setActor(staff);fail(()->store.advance(first.getId(),OrderStatus.CONFIRMED,false),"One piece must be packed before two pieces");store.advance(second.getId(),OrderStatus.CONFIRMED,false);store.advance(first.getId(),OrderStatus.CONFIRMED,false);
        fail(()->store.advance(first.getId(),OrderStatus.CONFIRMED,false),"Stale update rejected");
        store.advance(first.getId(),OrderStatus.PREPARING,false);store.advance(first.getId(),OrderStatus.READY_FOR_PICKUP,false);
                ActivityLogger.setActor(customer);fail(()->store.advance(first.getId(),OrderStatus.OUT_FOR_DELIVERY,true),"Customer cannot confirm in transit");
        ActivityLogger.setActor(staff);store.confirmCourierDelivery(first.getId(),"PROOF-FIRST",staff);
        Order delivered=store.orders().stream().filter(o->o.getId()==first.getId()).findFirst().orElseThrow();
        check(delivered.getStatus()==OrderStatus.DELIVERED&&delivered.getCompletedAt()==null&&delivered.getDeliveredBy()==staff.getId(),"Delivery evidence persists separately from acknowledgment");
        fail(()->store.advance(first.getId(),OrderStatus.DELIVERED,true),"Staff cannot acknowledge for customer");
        store.reportDeliveryIssue(first.getId(),"Package has not arrived",customer);
        check(store.orders().stream().filter(o->o.getId()==first.getId()).findFirst().orElseThrow().getDeliveryIssue().equals("Package has not arrived"),"Non-receipt issue persists");
        store.reviewDeliveryIssue(first.getId(),"REVIEWING","Checking with the delivery team",staff);
        store.reviewDeliveryIssue(first.getId(),"REDELIVERY","Another delivery has been arranged",staff);
        Order redelivery=store.orders().stream().filter(o->o.getId()==first.getId()).findFirst().orElseThrow();
        check(redelivery.getStatus()==OrderStatus.OUT_FOR_DELIVERY&&redelivery.getCourierReference().isBlank()&&redelivery.getDeliveryIssueStatus().equals("REDELIVERY"),"Redelivery status and staff response persist");
        store.confirmCourierDelivery(first.getId(),"PROOF-REDELIVERY",staff);
        ActivityLogger.setActor(customer);Order completed=store.advance(first.getId(),OrderStatus.DELIVERED,true);ActivityLogger.setActor(staff);
        check(completed.getReceivedBy()==customer.getId(),"Customer acknowledgment identity persists");
        db.transaction(c->{try(PreparedStatement s=c.prepareStatement("UPDATE products SET name='Renamed catalog shirt',price=999.99 WHERE id=?")){s.setInt(1,product.getId());s.executeUpdate();}return null;});
        Order restored=new JdbcStoreRepository(db).orders().stream().filter(o->o.getId()==first.getId()).findFirst().orElseThrow();
        check(restored.getStatus()==OrderStatus.COMPLETED&&restored.getCompletedAt().equals(completed.getCompletedAt()),"Completed status/date survives restart");
        check(restored.getPlacedAt().equals(first.getPlacedAt())&&Math.abs(restored.getTotal()-246.9)<0.001&&restored.getItems().get(0).getProduct().getName().equals("Purchased shirt"),"Historical date, name and price preserved");
        User buyer2=account(users,"buyer2-db",UserRole.CUSTOMER);
        ActivityLogger.setActor(admin);Product scarce=store.addProduct("Last stock","Men","Tops",5,5,"");
        store.addToCart(customer.getId(),scarce.getId(),2);store.addToCart(buyer2.getId(),scarce.getId(),2);
        store.adjustStock(scarce.getId(),1,admin);
        User reserveBuyer=users.findByEmailOrUsername("verifiedcustomer").orElseThrow();store.addToCart(reserveBuyer.getId(),scarce.getId(),3);store.checkout(reserveBuyer.getId(),checkout("Reserved stock customer"));
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
        check(StoreService.getInstance().getOrders().size()==4&&StoreService.getInstance().getProducts().size()==2,"Existing catalog is not reseeded");
        check(db.read(c->{try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM users")){r.next();return r.getInt(1);}})==7,"Updates do not duplicate users");
        Product regional=store.addProduct("Regional test shirt","Men","Tops",50,10,"");
        for (User buyer : List.of(customer,buyer2))
            for(CartItem leftover:store.cart(buyer.getId()))store.removeFromCart(buyer.getId(),leftover.getProduct().getId());
        java.util.function.Function<String,CheckoutDetails> regionDetails=province->{
            String city=DeliveryAddress.cities(province)[0];
            return CheckoutDetails.structured("Regional customer","test@example.invalid","+639123456789","Delivery",
                new DeliveryAddress(province,city,DeliveryAddress.barangays(province,city)[0],"123 Test Street"),"GCash","");
        };
        // Start any previous confirmed orders to preserve the production queue.
        startWaiting(store,staff,-1);
        store.addToCart(customer.getId(),regional.getId(),1);Order north=store.checkout(customer.getId(),regionDetails.apply("Ilocos Norte"));
        store.addToCart(customer.getId(),regional.getId(),1);Order central=store.checkout(customer.getId(),regionDetails.apply("Pampanga"));
        store.addToCart(customer.getId(),regional.getId(),1);Order pickup=store.checkout(customer.getId(),CheckoutDetails.structured("Pickup customer","test@example.invalid","+639123456789","Pickup",null,"GCash",""));
        paid(store,north,staff);paid(store,central,staff);paid(store,pickup,staff);
        store.advance(north.getId(),OrderStatus.CONFIRMED,false);store.advance(north.getId(),OrderStatus.PREPARING,false);
        store.advance(north.getId(),OrderStatus.READY_FOR_PICKUP,false);
        fail(()->store.advance(north.getId(),OrderStatus.TRANSFERRING,true),"Transfer cannot be completed before receipt");
        store.advance(central.getId(),OrderStatus.CONFIRMED,false);store.advance(central.getId(),OrderStatus.PREPARING,false);
        store.advance(central.getId(),OrderStatus.READY_FOR_PICKUP,false);store.confirmCourierDelivery(central.getId(),"PROOF-CENTRAL",staff);ActivityLogger.setActor(customer);store.advance(central.getId(),OrderStatus.DELIVERED,true);ActivityLogger.setActor(staff);
        store.advance(pickup.getId(),OrderStatus.CONFIRMED,false);store.advance(pickup.getId(),OrderStatus.PREPARING,false);
        store.advance(pickup.getId(),OrderStatus.READY_FOR_PICKUP,false);
        List<Order> regionalRestored=new JdbcStoreRepository(db).orders();
        check(regionalRestored.stream().filter(o->o.getId()==north.getId()).allMatch(o->o.getStatus()==OrderStatus.TRANSFERRING && o.getCheckoutDetails().getDeliveryPoint()==DeliveryPoint.NORTHERN),"Northern routing and transfer survive restart");
        check(regionalRestored.stream().filter(o->o.getId()==central.getId()||o.getId()==pickup.getId()).allMatch(o->o.getStatus()==OrderStatus.COMPLETED && o.getCompletedAt()!=null),"Near delivery and pickup complete independently with dates");
        store.advance(north.getId(),OrderStatus.TRANSFERRING,false);store.advance(north.getId(),OrderStatus.AT_DELIVERY_POINT,false);
        store.confirmCourierDelivery(north.getId(),"PROOF-NORTH",staff);ActivityLogger.setActor(customer);store.advance(north.getId(),OrderStatus.DELIVERED,true);ActivityLogger.setActor(staff);
        // Simulate a legacy schema, then verify migration can run twice without losing records.
        int orderCount=store.orders().size();
        db.read(c->{try(Statement s=c.createStatement()){s.execute("ALTER TABLE orders DROP COLUMN address_province,DROP COLUMN address_city,DROP COLUMN address_barangay,DROP COLUMN address_street, MODIFY COLUMN status ENUM('CONFIRMED','PREPARING','READY_FOR_PICKUP','OUT_FOR_DELIVERY','COMPLETED') NOT NULL DEFAULT 'CONFIRMED'");}return null;});
        db.verifySchema();db.verifySchema();
        db.read(c->{try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SHOW COLUMNS FROM orders LIKE 'status'")){r.next();check(r.getString("Type").contains("AT_DELIVERY_POINT"),"Legacy status enum expanded");}return null;});
        check(store.orders().size()==orderCount,"Additive migration is idempotent and preserves orders");
        Product legacyProduct=store.addProduct("Legacy review","Men","Tops",50,1,"");
        store.addToCart(customer.getId(),legacyProduct.getId(),1);Order legacy=store.checkout(customer.getId(),checkout("Legacy"));
        db.read(c->{try(PreparedStatement s=c.prepareStatement("UPDATE orders SET address_province=NULL,address_city=NULL,address_barangay=NULL,address_street=NULL WHERE id=?")){s.setInt(1,legacy.getId());s.executeUpdate();}return null;});
        store.assignDeliveryAddress(legacy.getId(),regionDetails.apply("Laguna").getStructuredAddress());
        check(store.orders().stream().filter(o->o.getId()==legacy.getId()).findFirst().orElseThrow().getCheckoutDetails().getDeliveryPoint()==DeliveryPoint.SOUTHERN,"Legacy address review persists");
        Product sized=store.addProduct("Size persistence","Men","Tops",87.50,3,"","L");
        store.addToCart(customer.getId(),sized.getId(),2);
        Order sizedOrder=store.checkout(customer.getId(),CheckoutDetails.structured("Return customer",customer.getEmail(),"+639123456789","Pickup",null,"GCash",""));
        startWaiting(store,staff,sizedOrder.getId());
        to(store,sizedOrder,OrderStatus.READY_FOR_PICKUP,staff);store.advance(sizedOrder.getId(),OrderStatus.READY_FOR_PICKUP,false);
        Order sizedRestored=new JdbcStoreRepository(db).orders().stream().filter(o->o.getId()==sizedOrder.getId()).findFirst().orElseThrow();
        check(sizedRestored.getItems().get(0).getProduct().getSize().equals("L")&&sizedRestored.getControlNumber().equals(sizedOrder.getControlNumber()),"Purchased size and random control survive restart");
        byte[] proof;
        try(java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(3,3,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);proof=out.toByteArray();}
        ReturnRequest request=ReturnsService.submit(customer,sizedOrder.getId(),sized.getId(),1,"Damaged seam on arrival",proof,"proof.png");
        ReturnsService.review(staff,request.id(),"APPROVED","Evidence supports return","");ReturnsService.review(staff,request.id(),"RECEIVED","Returned item inspected","");ReturnsService.review(admin,request.id(),"REFUNDED","Refund issued and recorded","REF-MYSQL-001");
        ReturnRequest restoredReturn=ReturnsService.requests(customer).stream().filter(r->r.id()==request.id()).findFirst().orElseThrow();
        check(restoredReturn.status().equals("REFUNDED")&&restoredReturn.refundAmount()==87.5&&java.util.Arrays.equals(restoredReturn.proof(),proof),"Return proof, refund status/reference and purchased amount persist");
        check(store.findProduct(sized.getId()).orElseThrow().getStock()==1,"Return processing does not silently add or remove inventory");
        SecurityIncidents.flag(customer,"Repeated unsupported requests need review");
        var incident=SecurityIncidents.incidents().get(0);
        check(SecurityIncidents.pendingWarnings(customer.getId()).isEmpty(),"Unreviewed flags do not automatically notify users");
        fail(()->SecurityIncidents.sendWarning(staff,incident.id()),"Only administrators can send a warning");
        SecurityIncidents.sendWarning(admin,incident.id());
        check(SecurityIncidents.warningMessages(customer.getId()).stream().anyMatch(m->m.id()==incident.id() && !m.read() && m.message().equals(SecurityIncidents.WARNING_MESSAGE) && m.sentAt()!=null),"Inbox stores message text, sent date and unread status");
        SecurityIncidents.acknowledgePopup(customer.getId(),incident.id());
        check(SecurityIncidents.pendingWarnings(customer.getId()).contains(incident.id()),"Popup dismissal does not consume mailbox unread status");
        check(!SecurityIncidents.pendingPopupWarnings(customer.getId()).contains(incident.id()),"Popup delivery state persists separately");
        fail(()->SecurityIncidents.sendWarning(admin,incident.id()),"Duplicate warning is blocked");
        Database reopened=new Database("jdbc:mysql://127.0.0.1:3307/queuetees_test","root",new char[0]);
        SecurityIncidents.useDatabase(reopened);
        check(SecurityIncidents.pendingWarnings(customer.getId()).contains(incident.id()),"Sent warning survives connection restart");
        check(SecurityIncidents.pendingWarnings(staff.getId()).isEmpty(),"Warning is limited to intended user");
        SecurityIncidents.acknowledgeWarning(staff.getId(),incident.id());
        check(SecurityIncidents.pendingWarnings(customer.getId()).contains(incident.id()),"Another user cannot acknowledge warning");
        SecurityIncidents.resolve(admin,incident.id(),"Reviewed and explained to customer");check(SecurityIncidents.incidents().stream().anyMatch(i->i.id()==incident.id()&&i.resolved()),"Usage warning review persists");
        check(SecurityIncidents.pendingWarnings(customer.getId()).contains(incident.id()),"Review preserves pending delivery");
        SecurityIncidents.acknowledgeWarning(customer.getId(),incident.id());
        SecurityIncidents.useDatabase(db);
        check(SecurityIncidents.pendingWarnings(customer.getId()).isEmpty(),"Acknowledgment survives connection restart");
        check(SecurityIncidents.warningMessages(customer.getId()).stream().anyMatch(m->m.id()==incident.id() && m.read() && m.message().equals(SecurityIncidents.WARNING_MESSAGE)),"Read and reviewed inbox messages survive restart");
        check(SecurityIncidents.warningMessages(staff.getId()).stream().noneMatch(m->m.id()==incident.id()),"Inbox history is restricted to the recipient");
        User permanentlySuspended=account(users,"permanently-suspended",UserRole.CUSTOMER);
        auth.permanentlySuspendUser(admin,permanentlySuspended.getId(),"Confirmed repeated misuse");
        check(new AuthService(new JdbcUserRepository(reopened)).login(permanentlySuspended.getEmail(),"Password123!".toCharArray()).getMessage().equals(SecurityIncidents.PERMANENT_SUSPENSION_MESSAGE),"Permanent suspension and approved message survive restart");
        fail(()->auth.suspendUser(admin,permanentlySuspended.getId(),false),"Permanent suspension cannot be undone with Unsuspend");
        User protectedAccount=account(users,"stale-session-protection",UserRole.CUSTOMER),staleProfile=reloadedUsers.findByEmailOrUsername("stale-session-protection").orElseThrow();
        String[] protectionCode={null};PasswordResetService protectionReset=new PasswordResetService(users,(email,code)->protectionCode[0]=code);
        String protectionToken=protectionReset.request(protectedAccount.getEmail());protectionReset.verify(protectionToken,protectionCode[0]);
        auth.permanentlySuspendUser(admin,protectedAccount.getId(),"Confirmed repeated misuse");
        fail(()->auth.updateUser(staleProfile),"Stale profile cannot resurrect suspended account");fail(()->auth.verifyEmail(staleProfile),"Stale verification cannot resurrect suspended account");
        protectionReset.reset(protectionToken,"Repaired123!".toCharArray(),"Repaired123!".toCharArray());
        check(auth.currentAccountStatus(protectedAccount)==AccountStatus.BANNED && auth.login(protectedAccount.getEmail(),"Repaired123!".toCharArray()).getStatus()==AuthStatus.ACCOUNT_BANNED,"Real database password reset preserves permanent suspension");
        check(store.orders().stream().filter(o->o.getId()==central.getId()).findFirst().orElseThrow().getCourierReference().equals("PROOF-CENTRAL"),"Courier confirmation proof persists");
        check(store.orders().stream().filter(o->o.getId()==legacy.getId()).findFirst().orElseThrow().getStatus()==OrderStatus.PREPARING,"Suspension test uses a currently preparing order");
        auth.suspendUser(admin,staff.getId(),true);ActivityLogger.setActor(staff);
        fail(()->store.advance(legacy.getId(),OrderStatus.PREPARING,false),"An already-open suspended staff session cannot update orders");
        auth.suspendUser(admin,staff.getId(),false);store.advance(legacy.getId(),OrderStatus.PREPARING,false);
        Product raceProduct=store.addProduct("Cancellation race","Men","Tops",25,10,"");
        startWaiting(store,staff,-1);
        store.addToCart(customer.getId(),raceProduct.getId(),2);Order raceOrder=store.checkout(customer.getId(),checkout("Race"));
        paid(store,raceOrder,staff);
        ExecutorService racers=Executors.newFixedThreadPool(2);CountDownLatch startRace=new CountDownLatch(1);
        try {
            Future<Boolean> cancel=racers.submit(()->{startRace.await();try{store.cancelOrder(raceOrder.getId(),"Customer changed their mind",customer);return true;}catch(IllegalStateException e){return false;}});
            Future<Boolean> prepare=racers.submit(()->{startRace.await();try{store.advance(raceOrder.getId(),OrderStatus.CONFIRMED,false);return true;}catch(IllegalStateException e){return false;}});
            startRace.countDown();boolean cancelled=cancel.get(20,TimeUnit.SECONDS),prepared=prepare.get(20,TimeUnit.SECONDS);
            check(cancelled!=prepared,"Exactly one of cancellation/preparation succeeds");
            check(store.findProduct(raceProduct.getId()).orElseThrow().getStock()==(cancelled?10:8),"Cancellation restores stock only when it wins");
        }finally{racers.shutdownNow();}
        store.addToCart(customer.getId(),raceProduct.getId(),1);Order cancelAgain=store.checkout(customer.getId(),checkout("Cancel"));paid(store,cancelAgain,staff);store.cancelOrder(cancelAgain.getId(),"No longer required",customer);
        int afterCancelStock=store.findProduct(raceProduct.getId()).orElseThrow().getStock();fail(()->store.cancelOrder(cancelAgain.getId(),"Repeat cancellation",customer),"Duplicate cancellation rejected");check(store.findProduct(raceProduct.getId()).orElseThrow().getStock()==afterCancelStock,"No second stock restoration");
        RefundService.recordOrderRefund("ORDER:"+cancelAgain.getId(),"CANCEL-MYSQL-001","Refund paid and recorded",admin);check(RefundService.totalPaid(cancelAgain.getId(),RefundService.records(customer))==25,"Cancellation refund persists");
        store.addToCart(customer.getId(),raceProduct.getId(),1);Order lost=store.checkout(customer.getId(),checkout("Lost"));
        to(store,lost,OrderStatus.OUT_FOR_DELIVERY,staff);store.confirmCourierDelivery(lost.getId(),"LOST-PROOF",staff);store.reportDeliveryIssue(lost.getId(),"Parcel never arrived",customer);int beforeLossStock=store.findProduct(raceProduct.getId()).orElseThrow().getStock();store.reviewDeliveryIssue(lost.getId(),"LOST","Courier confirmed parcel loss",staff);
        RefundService.recordOrderRefund("ORDER:"+lost.getId(),"LOST-MYSQL-001","Refund paid and recorded",admin);check(store.orders().stream().anyMatch(o->o.getId()==lost.getId()&&o.getStatus()==OrderStatus.LOST)&&store.findProduct(raceProduct.getId()).orElseThrow().getStock()==beforeLossStock,"Lost refund persists without restocking");
        User incomplete=account(users,"registration-gates",UserRole.CUSTOMER);
        incomplete.setTermsRequired(true);incomplete.setEmailVerified(false);users.save(incomplete);
        fail(()->store.addToCart(incomplete.getId(),raceProduct.getId(),1),"Unverified account cannot add stock to cart");
        fail(()->store.checkout(incomplete.getId(),checkout("Incomplete")),"Unverified account cannot check out");
        users.markEmailVerified(incomplete.getId());
        fail(()->store.addToCart(incomplete.getId(),raceProduct.getId(),1),"Terms must be accepted before shopping");
        fail(()->store.checkout(incomplete.getId(),checkout("Incomplete")),"Terms gate also protects checkout");
        auth.acceptTerms(incomplete,true);
        store.addToCart(incomplete.getId(),raceProduct.getId(),1);Order addressReview=store.checkout(incomplete.getId(),checkout("Address review"));
        db.transaction(c->{try(PreparedStatement s=c.prepareStatement("UPDATE orders SET address_province=NULL,address_city=NULL,address_barangay=NULL,address_street=NULL WHERE id=?")){s.setInt(1,addressReview.getId());s.executeUpdate();}return null;});
        DeliveryAddress reviewedAddress=checkout("Address review").getStructuredAddress();
        ActivityLogger.setActor(incomplete);
        fail(()->store.assignDeliveryAddress(addressReview.getId(),reviewedAddress),"Customer cannot perform staff address corrections");
        ActivityLogger.setActor(staff);auth.suspendUser(admin,staff.getId(),true);
        fail(()->store.assignDeliveryAddress(addressReview.getId(),reviewedAddress),"Stale staff session cannot correct addresses after suspension");
        auth.suspendUser(admin,staff.getId(),false);store.assignDeliveryAddress(addressReview.getId(),reviewedAddress);
        check(store.orders().stream().filter(o->o.getId()==addressReview.getId()).findFirst().orElseThrow().getCheckoutDetails().getStructuredAddress()!=null,"Authorized address correction persists");
        User draft=auth.registerCustomer("draft@example.com","Draft Person","draftperson","Example123!".toCharArray(),
                "123 Street, Barangay One, Pasay City, Metro Manila","9123456789","Female",LocalDate.of(2000,1,1)).getUser();
        int registrationCount=users.findAll().size();
        RegistrationResult revision=auth.reviseRegistration(draft,"updateddraft@example.com","Updated Person","updateddraft","Changed123!".toCharArray(),
                "456 Street, Barangay Two, Pasay City, Metro Manila","9234567890","Male",LocalDate.of(2001,2,3));
        check(revision.isSuccess() && revision.getUser().getId()==draft.getId() && users.findAll().size()==registrationCount,"Editing signup updates same persisted account");
        User revised=users.findByEmailOrUsername("updateddraft@example.com").orElseThrow();
        check(revised.getFullName().equals("Updated Person") && revised.getContactNumber().equals("9234567890") && revised.getGender().equals("Male")
                && revised.getBirthday().equals(LocalDate.of(2001,2,3)) && revised.getRegisteredAt().equals(draft.getRegisteredAt())
                && PasswordUtil.verifyPassword("Changed123!",revised.getPasswordHash()),"Revised fields persist while original registration date remains");
        auth.suspendUser(admin,revised.getId(),true);
        fail(()->auth.reviseRegistration(revised,"updateddraft@example.com","Changed Person","updateddraft","Changed123!".toCharArray(),
                revised.getAddress(),revised.getContactNumber(),revised.getGender(),revised.getBirthday()),"Returning to signup cannot undo suspension");
        auth.suspendUser(admin,revised.getId(),false);auth.acceptTerms(revised,true);
        fail(()->auth.reviseRegistration(revised,"updateddraft@example.com","Changed Person","updateddraft","Changed123!".toCharArray(),
                revised.getAddress(),revised.getContactNumber(),revised.getGender(),revised.getBirthday()),"Accepted registrations cannot be edited as unfinished signup");
        System.out.println("PASS: real MySQL schema, editable signup without duplicate accounts, registration gates, address permissions, suspension-safe account updates, verified payments, shortest-job packing, strict fulfillment, concurrent stock, refunds and persistent audit");
    }
}
