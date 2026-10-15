package service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import model.CartItem;
import model.CheckoutDetails;
import model.Order;
import model.OrderStatus;
import model.Product;

/** Shared state: shortest packing jobs first with independently fulfilled regional orders. */
public final class StoreService {
    private static final StoreService INSTANCE = new StoreService();

    private final List<Product> products = new ArrayList<>();
    private final Map<Integer, List<CartItem>> carts = new LinkedHashMap<>();
    private final List<Order> orders = new ArrayList<>();
    private final AtomicInteger productIds = new AtomicInteger(1);
    private final AtomicInteger orderIds = new AtomicInteger(1);
    private final AtomicInteger queueNumbers = new AtomicInteger(1);
    private boolean initializing = true;
    private repository.JdbcStoreRepository persistence;
    public synchronized void useDatabase(database.Database database) {
        repository.JdbcStoreRepository repository = new repository.JdbcStoreRepository(database);
        repository.seedCatalog(new ArrayList<>(products));
        persistence = repository;
        ReturnsService.useDatabase(database);
        SecurityIncidents.useDatabase(database);
        RefundService.useDatabase(database);
    }

    private StoreService() {
        // Men's catalog
        addProduct("Vintage Moto Jacket", "Men", "Tops", 1299.00, 10, "/Gui_Images/mentop1.png");
        addProduct("Washed Zip Hoodie", "Men", "Tops", 899.00, 14, "/Gui_Images/mentop2.png");
        addProduct("Garden Check Shirt", "Men", "Tops", 749.00, 12, "/Gui_Images/mentop3.png");
        addProduct("Heritage Stripe Tee", "Men", "Tops", 549.00, 18, "/Gui_Images/mentop4.png");
        addProduct("Camo Cargo Pants", "Men", "Bottoms", 999.00, 11, "/Gui_Images/menbottom1.png");

        // Women's catalog
        addProduct("Wave Surfer Tee", "Women", "Tops", 549.00, 16, "/Gui_Images/womentop1.png");
        addProduct("Short Sleeve Hoodie", "Women", "Tops", 699.00, 13, "/Gui_Images/womentop2.png");
        addProduct("Geisha Graphic Top", "Women", "Tops", 649.00, 12, "/Gui_Images/womentop3.png");
        addProduct("Rhinestone Tank Top", "Women", "Tops", 499.00, 17, "/Gui_Images/womentop4.png");
        addProduct("Jeweled Wide-Leg Jeans", "Women", "Bottoms", 949.00, 10, "/Gui_Images/womenbottom1.png");
        addProduct("Vintage Flare Jeans", "Women", "Bottoms", 899.00, 12, "/Gui_Images/womenbottom2.png");
        addProduct("Belted Wide-Leg Jeans", "Women", "Bottoms", 999.00, 9, "/Gui_Images/womenbottom3.png");
        addProduct("Denim Mini Skirt", "Women", "Bottoms", 649.00, 14, "/Gui_Images/womenbottom4.png");

        // Kids' catalog
        addProduct("Boys Layered Stripe Tee", "Kids", "Tops", 399.00, 15, "/Gui_Images/boytop1.png");
        addProduct("Boys Plaid Cargo Shorts", "Kids", "Bottoms", 449.00, 13, "/Gui_Images/boybottom1.png");
        addProduct("Girls Pink Monkey Tee", "Kids", "Tops", 349.00, 16, "/Gui_Images/girltop1.png");
        addProduct("Girls Pastel Polo", "Kids", "Tops", 399.00, 14, "/Gui_Images/girltop2.png");
        addProduct("Girls Butterfly Jeans", "Kids", "Bottoms", 549.00, 12, "/Gui_Images/girlbottom1.png");

        // Accessories
        addProduct("Burgundy Sport Sunglasses", "Accessories", "Accessories", 299.00, 20, "/Gui_Images/accessories1.png");
        addProduct("Olive Heart Cap", "Accessories", "Accessories", 349.00, 16, "/Gui_Images/accessories2.png");
        addProduct("Classic Brown Handbag", "Accessories", "Accessories", 899.00, 9, "/Gui_Images/accessories3.png");
        addProduct("Floral Leather Belt", "Accessories", "Accessories", 399.00, 14, "/Gui_Images/accessories4.png");
        addProduct("Brown Floral Cadet Cap", "Accessories", "Accessories", 349.00, 15, "/Gui_Images/accessories5.png");
        initializing = false;
    }

    public static StoreService getInstance() { return INSTANCE; }

    public synchronized Product addProduct(String name, String category, String subcategory,
                double price, int stock, String imagePath) {
        return addProduct(name,category,subcategory,price,stock,imagePath,"Accessories".equals(category)?"One Size":"M");
    }
    public synchronized Product addProduct(String name, String category, String subcategory,
                double price, int stock, String imagePath, String size) {
        if (persistence != null) return persistence.addProduct(name,category,subcategory,price,stock,imagePath,size);
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Product name is required.");
        if (!Double.isFinite(price) || price < 0 || stock < 0) throw new IllegalArgumentException("Price and stock cannot be negative.");
        if (getProducts().stream().anyMatch(p->p.getName().equalsIgnoreCase(name.trim()) && p.getSize().equals(size)))
            throw new IllegalArgumentException("That product size already exists. Replenish its stock instead.");
        Product product = new Product(productIds.getAndIncrement(), name.trim(), category,
                subcategory, price, stock, imagePath == null ? "" : imagePath.trim(),size);
        products.add(product);
        if (!initializing) ActivityLogger.record("Product added", product.getName() + " (#" + product.getId() + "), initial stock: " + stock);
        return product;
    }

    public synchronized List<Product> getProducts() { return persistence == null ? new ArrayList<>(products) : persistence.products(); }

    public synchronized Optional<Product> findProduct(int id) {
        if (persistence != null) return persistence.findProduct(id);
        return products.stream().filter(product -> product.getId() == id).findFirst();
    }

    public synchronized Product adjustStock(int productId, int delta, model.User actor) {
        if(actor==null || actor.getRole()!=model.UserRole.ADMIN || actor.getStatus()!=model.AccountStatus.ACTIVE)
            throw new IllegalStateException("An active administrator is required to replenish stock.");
        if(delta<=0){SecurityIncidents.flag(actor,"Attempted a non-positive manual stock adjustment for product #"+productId+".");throw new IllegalArgumentException("Manual stock decreases are prohibited.");}
        if (persistence != null) return persistence.adjustStock(productId,delta,actor);
        Product product = findProduct(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product was not found."));
        long updated = (long) product.getStock() + delta;
        if (updated < 0 || updated > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Stock must stay between 0 and " + Integer.MAX_VALUE + ".");
        int before = product.getStock();
        product.setStock((int) updated);
        ActivityLogger.record(actor, "Stock adjusted", product.getName() + " (#" + productId + "): "
                + before + " → " + updated + " (" + (delta > 0 ? "+" : "") + delta + ")");
        return product;
    }

    public synchronized void addToCart(int customerId, int productId) {
        addToCart(customerId, productId, 1);
    }

    public synchronized void addToCart(int customerId, int productId, int amount) {
        if (persistence != null) { persistence.addToCart(customerId,productId,amount); return; }
        if (amount < 1) throw new IllegalArgumentException("Quantity must be at least 1.");
        Product product = findProduct(productId).orElseThrow(() -> new IllegalArgumentException("Product was not found."));
        List<CartItem> cart = carts.computeIfAbsent(customerId, key -> new ArrayList<>());
        CartItem existing = cart.stream().filter(item -> item.getProduct().getId() == productId).findFirst().orElse(null);
        int quantity = existing == null ? 0 : existing.getQuantity();
        long updatedQuantity=(long)quantity+amount;
        if (updatedQuantity > product.getStock()) {
            throw new IllegalStateException("Only " + Math.max(0, product.getStock() - quantity)
                    + " more of this item can be added.");
        }
        if (existing == null) cart.add(new CartItem(product, amount));
        else existing.setQuantity((int)updatedQuantity);
    }

    public synchronized List<CartItem> getCart(int customerId) {
        if (persistence != null) return persistence.cart(customerId);
        return new ArrayList<>(carts.getOrDefault(customerId, new ArrayList<>()));
    }

    public synchronized void removeFromCart(int customerId, int productId) {
        if (persistence != null) { persistence.removeFromCart(customerId,productId); return; }
        carts.computeIfAbsent(customerId, key -> new ArrayList<>())
                .removeIf(item -> item.getProduct().getId() == productId);
    }

    public synchronized Order checkout(int customerId, CheckoutDetails details) {
        if (details == null) throw new IllegalArgumentException("Checkout details are required.");
        details.validateFulfillment();
        if (persistence != null) return persistence.checkout(customerId,details);
        List<CartItem> cart = carts.getOrDefault(customerId, new ArrayList<>());
        if (cart.isEmpty()) throw new IllegalStateException("Your cart is empty.");
        if (details == null || details.getFullName() == null || details.getFullName().trim().isEmpty())
            throw new IllegalArgumentException("Full name is required.");
        if (details.getContactNumber() == null || details.getContactNumber().trim().isEmpty())
            throw new IllegalArgumentException("Contact number is required.");
        if ("Delivery".equals(details.getFulfillmentMethod())
                && (details.getAddress() == null || details.getAddress().trim().isEmpty()))
            throw new IllegalArgumentException("Delivery address is required.");
        for (CartItem item : cart) {
            if (item.getQuantity() > item.getProduct().getStock()) {
                throw new IllegalStateException(item.getProduct().getName() + " no longer has enough stock.");
            }
        }
        for (CartItem item : cart) {
            Product product = item.getProduct();
            product.setStock(product.getStock() - item.getQuantity());
        }
        Order order = new Order(orderIds.getAndIncrement(), queueNumbers.getAndIncrement(),
                customerId, details.getFullName().trim(), cart, LocalDateTime.now(), details);
        orders.add(order);
        carts.remove(customerId);
        return order;
    }
    /** Delivery-only checkout used by the storefront; legacy pickup records remain readable. */
    public synchronized Order checkoutDelivery(int customerId,CheckoutDetails details){
        if(details==null||!"Delivery".equals(details.getFulfillmentMethod()))throw new IllegalArgumentException("Only delivery is available. Select a covered delivery address.");
        return checkout(customerId,details);
    }

    public synchronized List<Order> getOrders() { return persistence == null ? new ArrayList<>(orders) : persistence.orders(); }
    public synchronized void verifyPayment(int id,double amount,String reference,model.User actor){
        if(persistence!=null){persistence.verifyPayment(id,amount,reference,actor);return;}
        Order order=orders.stream().filter(o->o.getId()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Order not found."));
        PaymentVerification.validate(actor,order,amount,reference);
        if(orders.stream().anyMatch(o->o.getPaymentReference().equalsIgnoreCase(reference.trim())))throw new IllegalStateException("Payment reference already used.");
        order.restorePayment(model.PaymentStatus.VERIFIED,amount,reference.trim());ActivityLogger.record(actor,"Payment verified",order.getControlNumber()+": "+reference.trim());
    }
    public synchronized void assignDeliveryAddress(int orderId, model.DeliveryAddress address) {
        if (persistence != null) { persistence.assignDeliveryAddress(orderId, address); return; }
        Order order = orders.stream().filter(o -> o.getId() == orderId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order was not found."));
        OrderAccess.memory(ActivityLogger.getActor(),order,false);
        order.assignDeliveryAddress(address);
        ActivityLogger.record("Delivery address reviewed", "Q-" + order.getQueueNumber() + ": " + address.format());
    }

    public synchronized List<Order> getOrdersForCustomer(int customerId) {
        return getOrders().stream().filter(order -> order.getCustomerId() == customerId).collect(Collectors.toList());
    }

    public synchronized List<Order> getActiveOrders() {
        return getOrders().stream().filter(order -> !RefundService.terminal(order)).collect(Collectors.toList());
    }
    public synchronized List<Order> getDeliveryQueue() { return model.DeliveryPriority.queue(getOrders()); }

    public synchronized List<Order> getCompletedOrders() {
        return getOrders().stream().filter(order -> order.getStatus() == OrderStatus.COMPLETED).collect(Collectors.toList());
    }

    public synchronized Order advanceOrder(int orderId) {
        if (persistence != null) return persistence.advance(orderId,null,false);
        Order order = orders.stream().filter(item -> item.getId() == orderId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order was not found."));
        return advanceOrder(orderId, order.getStatus());
    }

    public synchronized Order advanceOrder(int orderId, OrderStatus expectedStatus) {
        if (persistence != null) return persistence.advance(orderId,expectedStatus,false);
        Order order = orders.stream().filter(item -> item.getId() == orderId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order was not found."));
        if (order.getStatus() != expectedStatus)
            throw new IllegalStateException("This order has changed. Review its current status and try again.");
        OrderAccess.memory(ActivityLogger.getActor(),order,false);
        if (order.getStatus() == OrderStatus.COMPLETED)
            throw new IllegalStateException("This order is already completed.");
        // Start preparation in arrival order; ready orders can await collection independently.
        if(order.getStatus()==OrderStatus.CONFIRMED && !model.PackingPriority.isNext(orders,order))throw new IllegalStateException("Start the waiting order with the fewest pieces first.");
        OrderStatus before = order.getStatus();
        OrderStatus after=model.Fulfillment.next(order,false);if(after==OrderStatus.OUT_FOR_DELIVERY)model.DeliveryPriority.requireNearest(orders,order);order.setStatus(after);
        if(order.getStatus()==OrderStatus.COMPLETED)order.restoreAcknowledgment(order.getDeliveredBy(),ActivityLogger.getActor()==null?null:ActivityLogger.getActor().getId(),order.getDeliveryIssue());
        if (before != order.getStatus()) ActivityLogger.record("Order updated", order.getControlNumber()
                + ": " + before.getLabel() + " → " + order.getStatusLabel());
        return order;
    }

    public synchronized Order completeOrder(int orderId) {
        if (persistence != null) return persistence.advance(orderId,null,true);
        Order order = orders.stream().filter(item -> item.getId() == orderId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order was not found."));
        if (!model.Fulfillment.canConfirmReceipt(order))
            throw new IllegalStateException("Order cannot be completed at this stage.");
        OrderAccess.memory(ActivityLogger.getActor(),order,true);
        if(!order.getCheckoutDetails().isPickup() && (ActivityLogger.getActor()==null || ActivityLogger.getActor().getRole()!=model.UserRole.CUSTOMER))throw new IllegalStateException("Staff delivery completion requires recorded courier confirmation.");
        order.setStatus(OrderStatus.COMPLETED);
        if(!order.getDeliveryIssue().isBlank())order.restoreIssueReview("RESOLVED",order.getDeliveryIssueResponse()+"\nCustomer acknowledged receipt.",order.getDeliveryIssueReviewer());
        order.restoreAcknowledgment(order.getDeliveredBy(),ActivityLogger.getActor()==null?null:ActivityLogger.getActor().getId(),order.getDeliveryIssue());
        ActivityLogger.record("Order completed", order.getControlNumber());
        return order;
    }
    public synchronized Order advanceOrderTo(int orderId,OrderStatus expected,OrderStatus target) {
        if(persistence!=null)return persistence.advanceTo(orderId,expected,target);
        Order order=orders.stream().filter(o->o.getId()==orderId).findFirst().orElseThrow(()->new IllegalArgumentException("Order was not found."));
        if(order.getStatus()!=expected)throw new IllegalStateException("This order has changed. Refresh and try again.");
        OrderAccess.memory(ActivityLogger.getActor(),order,false);
        if(order.getStatus()==OrderStatus.CONFIRMED && !model.PackingPriority.isNext(orders,order))throw new IllegalStateException("Start the waiting order with the fewest pieces first.");
        OrderStatus after=model.OrderWorkflow.target(order,target);if(after==OrderStatus.OUT_FOR_DELIVERY)model.DeliveryPriority.requireNearest(orders,order);order.setStatus(after);
        ActivityLogger.record("Order stage updated",order.getControlNumber()+": "+expected+" → "+target);return order;
    }
    public synchronized Order confirmCourierDelivery(int orderId,String reference,model.User actor) {
        if(persistence!=null)return persistence.confirmCourierDelivery(orderId,reference,actor);
        Order order=orders.stream().filter(o->o.getId()==orderId).findFirst().orElseThrow(()->new IllegalArgumentException("Order was not found."));
        CourierConfirmation.validate(actor,reference,order);
        order.restoreCourier(reference.trim(),LocalDateTime.now());order.setStatus(OrderStatus.DELIVERED);
        order.restoreAcknowledgment(actor.getId(),null,order.getDeliveryIssue());
        ActivityLogger.record(actor,"Courier delivery confirmed",order.getControlNumber()+": "+reference.trim());
        return order;
    }
    public synchronized void reportDeliveryIssue(int orderId, String note, model.User actor) {
        if(actor==null || actor.getRole()!=model.UserRole.CUSTOMER || actor.getStatus()!=model.AccountStatus.ACTIVE)
            throw new IllegalStateException("An active customer account is required.");
        if(note==null || note.trim().length()<5 || note.trim().length()>1000)
            throw new IllegalArgumentException("Describe the delivery issue in 5–1000 characters.");
        if(persistence!=null){persistence.reportDeliveryIssue(orderId,note.trim(),actor);return;}
        Order order=orders.stream().filter(o->o.getId()==orderId).findFirst().orElseThrow(()->new IllegalArgumentException("Order was not found."));
        OrderAccess.memory(actor,order,true);
        if(order.getStatus()!=OrderStatus.DELIVERED)throw new IllegalStateException("Report non-receipt while awaiting acknowledgment.");
        if(java.util.Set.of("OPEN","REVIEWING").contains(order.getDeliveryIssueStatus()))throw new IllegalStateException("Your report is already awaiting staff review.");
        order.restoreAcknowledgment(order.getDeliveredBy(),order.getReceivedBy(),note.trim());
        order.restoreIssueReview("OPEN","",null);
    }
    public synchronized void reviewDeliveryIssue(int orderId,String outcome,String note,model.User actor) {
        DeliveryIssueReview.validate(actor,outcome,note);
        if(persistence!=null){persistence.reviewDeliveryIssue(orderId,outcome,note.trim(),actor);return;}
        Order order=orders.stream().filter(o->o.getId()==orderId).findFirst().orElseThrow(()->new IllegalArgumentException("Order was not found."));
        DeliveryIssueReview.checkOrder(order);
        if(outcome.equals("LOST"))try{RefundService.request(null,order,"ORDER:"+orderId,"LOST",RefundService.paidAmount(order),note.trim());}catch(java.sql.SQLException e){throw new IllegalStateException(e);}
        order.restoreIssueReview(outcome,note.trim(),actor.getId());
        if(outcome.equals("REDELIVERY")){order.setStatus(OrderStatus.OUT_FOR_DELIVERY);order.restoreCourier("",null);}
        if(outcome.equals("LOST"))order.setStatus(OrderStatus.LOST);
        ActivityLogger.record(actor,"Delivery issue reviewed",order.getControlNumber()+": "+outcome+" / "+note.trim());
    }
    public synchronized void cancelOrder(int orderId,String reason,model.User actor){
        if(actor==null||actor.getRole()!=model.UserRole.CUSTOMER||actor.getStatus()!=model.AccountStatus.ACTIVE||reason==null||reason.trim().length()<5||reason.trim().length()>1000)throw new IllegalArgumentException("An active customer and cancellation reason (5–1000 characters) are required.");
        if(persistence!=null){persistence.cancelOrder(orderId,reason.trim(),actor);return;}
        Order order=orders.stream().filter(o->o.getId()==orderId).findFirst().orElseThrow(()->new IllegalArgumentException("Order was not found."));
        if(order.getCustomerId()!=actor.getId()||order.getStatus()!=OrderStatus.CONFIRMED)throw new IllegalStateException("Only your order waiting for preparation can be cancelled.");
        for(CartItem item:order.getItems()){Product p=findProduct(item.getProduct().getId()).orElseThrow();if((long)p.getStock()+item.getQuantity()>Integer.MAX_VALUE)throw new IllegalStateException("Stock restoration would overflow.");}
        try{RefundService.request(null,order,"ORDER:"+orderId,"CANCELLATION",RefundService.paidAmount(order),reason.trim());}catch(java.sql.SQLException e){throw new IllegalStateException(e);}
        for(CartItem item:order.getItems()){Product p=findProduct(item.getProduct().getId()).orElseThrow();p.setStock(p.getStock()+item.getQuantity());}
        order.restoreIssueReview("CANCELLED",java.time.LocalDateTime.now()+" / "+reason.trim(),actor.getId());order.setStatus(OrderStatus.CANCELLED);
    }
}
