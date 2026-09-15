package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Order {
    private final int id;
    private final int queueNumber;
    private final int customerId;
    private final String customerName;
    private final List<CartItem> items;
    private final LocalDateTime placedAt;
    private final CheckoutDetails checkoutDetails;
    private OrderStatus status;
    private LocalDateTime completedAt;

    public Order(int id, int queueNumber, int customerId, String customerName,
                 List<CartItem> items, LocalDateTime placedAt, CheckoutDetails checkoutDetails) {
        this.id = id;
        this.queueNumber = queueNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.items = new ArrayList<>();
        for (CartItem item : items) {
            // Preserve purchased names and prices for history and printed reports.
            Product product = item.getProduct();
            Product purchased = new Product(product.getId(), product.getName(), product.getCategory(),
                    product.getSubcategory(), product.getPrice(), product.getStock(), product.getImagePath());
            this.items.add(new CartItem(purchased, item.getQuantity()));
        }
        this.placedAt = placedAt;
        this.checkoutDetails = checkoutDetails;
        this.status = OrderStatus.CONFIRMED;
    }

    public int getId() { return id; }
    public int getQueueNumber() { return queueNumber; }
    public int getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public List<CartItem> getItems() { return Collections.unmodifiableList(items); }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public CheckoutDetails getCheckoutDetails() { return checkoutDetails; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setStatus(OrderStatus status) {
        if (status == null) throw new IllegalArgumentException("Order status is required.");
        if (status == OrderStatus.COMPLETED && this.status != OrderStatus.COMPLETED)
            completedAt = LocalDateTime.now();
        this.status = status;
    }
    public int getItemCount() { return items.stream().mapToInt(CartItem::getQuantity).sum(); }
    public double getTotal() { return items.stream().mapToDouble(CartItem::getSubtotal).sum(); }
}
