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

/** Shared application state for the catalog, carts, and FIFO order queue. */
public final class StoreService {
    private static final StoreService INSTANCE = new StoreService();

    private final List<Product> products = new ArrayList<>();
    private final Map<Integer, List<CartItem>> carts = new LinkedHashMap<>();
    private final List<Order> orders = new ArrayList<>();
    private final AtomicInteger productIds = new AtomicInteger(1);
    private final AtomicInteger orderIds = new AtomicInteger(1);
    private final AtomicInteger queueNumbers = new AtomicInteger(1);

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
    }

    public static StoreService getInstance() { return INSTANCE; }

    public synchronized Product addProduct(String name, String category, String subcategory,
                                           double price, int stock, String imagePath) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Product name is required.");
        if (price < 0 || stock < 0) throw new IllegalArgumentException("Price and stock cannot be negative.");
        Product product = new Product(productIds.getAndIncrement(), name.trim(), category,
                subcategory, price, stock, imagePath == null ? "" : imagePath.trim());
        products.add(product);
        return product;
    }

    public synchronized List<Product> getProducts() { return new ArrayList<>(products); }

    public synchronized Optional<Product> findProduct(int id) {
        return products.stream().filter(product -> product.getId() == id).findFirst();
    }

    public synchronized void addToCart(int customerId, int productId) {
        addToCart(customerId, productId, 1);
    }

    public synchronized void addToCart(int customerId, int productId, int amount) {
        if (amount < 1) throw new IllegalArgumentException("Quantity must be at least 1.");
        Product product = findProduct(productId).orElseThrow(() -> new IllegalArgumentException("Product was not found."));
        List<CartItem> cart = carts.computeIfAbsent(customerId, key -> new ArrayList<>());
        CartItem existing = cart.stream().filter(item -> item.getProduct().getId() == productId).findFirst().orElse(null);
        int quantity = existing == null ? 0 : existing.getQuantity();
        if (quantity + amount > product.getStock()) {
            throw new IllegalStateException("Only " + Math.max(0, product.getStock() - quantity)
                    + " more of this item can be added.");
        }
        if (existing == null) cart.add(new CartItem(product, amount));
        else existing.setQuantity(quantity + amount);
    }

    public synchronized List<CartItem> getCart(int customerId) {
        return new ArrayList<>(carts.getOrDefault(customerId, new ArrayList<>()));
    }

    public synchronized void removeFromCart(int customerId, int productId) {
        carts.computeIfAbsent(customerId, key -> new ArrayList<>())
                .removeIf(item -> item.getProduct().getId() == productId);
    }

    public synchronized Order checkout(int customerId, CheckoutDetails details) {
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

    public synchronized List<Order> getOrders() { return new ArrayList<>(orders); }

    public synchronized List<Order> getOrdersForCustomer(int customerId) {
        return orders.stream().filter(order -> order.getCustomerId() == customerId).collect(Collectors.toList());
    }

    public synchronized List<Order> getActiveOrders() {
        return orders.stream().filter(order -> order.getStatus() != OrderStatus.COMPLETED).collect(Collectors.toList());
    }

    public synchronized List<Order> getCompletedOrders() {
        return orders.stream().filter(order -> order.getStatus() == OrderStatus.COMPLETED).collect(Collectors.toList());
    }

    public synchronized Order advanceOrder(int orderId) {
        Order order = orders.stream().filter(item -> item.getId() == orderId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order was not found."));
        return advanceOrder(orderId, order.getStatus());
    }

    public synchronized Order advanceOrder(int orderId, OrderStatus expectedStatus) {
        Order order = orders.stream().filter(item -> item.getId() == orderId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order was not found."));
        if (order.getStatus() != expectedStatus)
            throw new IllegalStateException("This order has changed. Review its current status and try again.");
        if (order.getStatus() == OrderStatus.COMPLETED)
            throw new IllegalStateException("This order is already completed.");
        // Start preparation in arrival order; ready orders can await collection independently.
        if (order.getStatus() == OrderStatus.CONFIRMED && orders.stream().anyMatch(earlier ->
                earlier.getStatus() == OrderStatus.CONFIRMED && earlier.getQueueNumber() < order.getQueueNumber()))
            throw new IllegalStateException("Start the earliest waiting order first.");
        switch (order.getStatus()) {
            case CONFIRMED: order.setStatus(OrderStatus.PREPARING); break;
            case PREPARING: order.setStatus(OrderStatus.READY_FOR_PICKUP); break;
            case READY_FOR_PICKUP: order.setStatus(OrderStatus.COMPLETED); break;
            case COMPLETED: break;
        }
        return order;
    }
}
