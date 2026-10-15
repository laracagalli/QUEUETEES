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
    private CheckoutDetails checkoutDetails;
    private OrderStatus status;
    private LocalDateTime completedAt;
    private String controlNumber = newControlNumber();
    private String courierReference = "";
    private LocalDateTime courierConfirmedAt;
    private Integer deliveredBy;
    private Integer receivedBy;
    private String deliveryIssue = "";
    private String deliveryIssueStatus = "";
    private String deliveryIssueResponse = "";
    private Integer deliveryIssueReviewer;
    private PaymentStatus paymentStatus=PaymentStatus.PENDING;
    private double paidAmount;
    private String paymentReference="";
    public PaymentStatus getPaymentStatus(){return paymentStatus;}
    public double getPaidAmount(){return paidAmount;}
    public String getPaymentReference(){return paymentReference;}
    public boolean canStartPacking(){return paymentStatus==PaymentStatus.VERIFIED || paymentStatus==PaymentStatus.CASH_ON_DELIVERY;}
    public Order restorePayment(PaymentStatus status,double amount,String reference){paymentStatus=status;paidAmount=amount;paymentReference=reference==null?"":reference;return this;}
    public Order copyPaymentFrom(Order source){return restorePayment(source.paymentStatus,source.paidAmount,source.paymentReference);}
    public String getDeliveryIssueStatus() { return deliveryIssueStatus.isBlank()&&!deliveryIssue.isBlank()?"OPEN":deliveryIssueStatus; }
    public String getDeliveryIssueResponse() { return deliveryIssueResponse; }
    public Integer getDeliveryIssueReviewer() { return deliveryIssueReviewer; }
    public Order restoreIssueReview(String status,String response,Integer reviewer) {
        deliveryIssueStatus=status==null?"":status;deliveryIssueResponse=response==null?"":response;deliveryIssueReviewer=reviewer;return this;
    }
    public Integer getDeliveredBy() { return deliveredBy; }
    public Integer getReceivedBy() { return receivedBy; }
    public String getDeliveryIssue() { return deliveryIssue; }
    public Order restoreAcknowledgment(Integer deliveredBy, Integer receivedBy, String issue) {
        this.deliveredBy=deliveredBy; this.receivedBy=receivedBy; this.deliveryIssue=issue==null?"":issue; return this;
    }
    public static String newControlNumber() { return "QT-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0,12).toUpperCase(java.util.Locale.ROOT); }
    public String getControlNumber() { return controlNumber; }
    public Order restoreControlNumber(String number) { if(number!=null && !number.isBlank())controlNumber=number;return this; }
    public String getCourierReference() { return courierReference; }
    public LocalDateTime getCourierConfirmedAt() { return courierConfirmedAt; }
    public Order restoreCourier(String reference,LocalDateTime at) { courierReference=reference==null?"":reference;courierConfirmedAt=at;return this; }

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
                    product.getSubcategory(), product.getPrice(), product.getStock(), product.getImagePath(), product.getSize());
            this.items.add(new CartItem(purchased, item.getQuantity()));
        }
        this.placedAt = placedAt;
        this.checkoutDetails = checkoutDetails;
        this.status = OrderStatus.CONFIRMED;
        if(checkoutDetails.getPaymentMethod()!=null && checkoutDetails.getPaymentMethod().equalsIgnoreCase("Cash"))paymentStatus=PaymentStatus.CASH_ON_DELIVERY;
    }

    public int getId() { return id; }
    public static Order restore(int id, int queueNumber, int customerId, String customerName, List<CartItem> items,
                                LocalDateTime placedAt, CheckoutDetails details, OrderStatus status, LocalDateTime completedAt) {
        Order order = new Order(id, queueNumber, customerId, customerName, items, placedAt, details);
        order.status = status; order.completedAt = completedAt; return order;
    }
    public int getQueueNumber() { return queueNumber; }
    public int getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public List<CartItem> getItems() { return Collections.unmodifiableList(items); }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public CheckoutDetails getCheckoutDetails() { return checkoutDetails; }
    public void assignDeliveryAddress(DeliveryAddress address) {
        if (checkoutDetails.isPickup() || checkoutDetails.getDeliveryPoint() != DeliveryPoint.UNASSIGNED
                || status.ordinal() >= OrderStatus.OUT_FOR_DELIVERY.ordinal())
            throw new IllegalStateException("Only unassigned orders awaiting dispatch can have their address reviewed.");
        checkoutDetails = CheckoutDetails.structured(checkoutDetails.getFullName(), checkoutDetails.getEmail(),
                checkoutDetails.getContactNumber(), checkoutDetails.getFulfillmentMethod(), address,
                checkoutDetails.getPaymentMethod(), checkoutDetails.getNotes());
    }
    public OrderStatus getStatus() { return status; }
    public String getStatusLabel() {
        return status == OrderStatus.READY_FOR_PICKUP && !checkoutDetails.isPickup() ? "Ready for dispatch" : status.getLabel();
    }
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
