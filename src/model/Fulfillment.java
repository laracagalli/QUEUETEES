package model;

/** Shared transition rules for memory and JDBC storage. Delivery never depends on other orders. */
public final class Fulfillment {
    private Fulfillment() { }
    public static OrderStatus[] stages(Order order) {
        if (order.getCheckoutDetails().isPickup()) return new OrderStatus[]{OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, OrderStatus.COMPLETED};
        DeliveryPoint point = order.getCheckoutDetails().getDeliveryPoint();
        if (point == DeliveryPoint.CENTRAL || point == DeliveryPoint.UNASSIGNED)
            return new OrderStatus[]{OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.COMPLETED};
        return new OrderStatus[]{OrderStatus.CONFIRMED,OrderStatus.PREPARING,OrderStatus.READY_FOR_PICKUP,OrderStatus.TRANSFERRING,OrderStatus.AT_DELIVERY_POINT,OrderStatus.OUT_FOR_DELIVERY,OrderStatus.DELIVERED,OrderStatus.COMPLETED};
    }
    public static boolean canConfirmReceipt(Order order) {
        return order.getStatus() == OrderStatus.DELIVERED
            || order.getCheckoutDetails().isPickup() && order.getStatus() == OrderStatus.READY_FOR_PICKUP;
    }
    public static OrderStatus next(Order order, boolean receipt) {
        CheckoutDetails details = order.getCheckoutDetails();
        if (receipt) {
            if (!canConfirmReceipt(order)) throw new IllegalStateException("Order cannot be completed at this stage.");
            return OrderStatus.COMPLETED;
        }
        return switch (order.getStatus()) {
            case CONFIRMED -> OrderStatus.PREPARING;
            case PREPARING -> OrderStatus.READY_FOR_PICKUP;
            case READY_FOR_PICKUP -> {
                if (details.isPickup()) yield OrderStatus.COMPLETED;
                if (details.getDeliveryPoint() == DeliveryPoint.UNASSIGNED)
                    throw new IllegalStateException("This older order needs an address assignment before dispatch.");
                yield details.getDeliveryPoint() == DeliveryPoint.CENTRAL ? OrderStatus.OUT_FOR_DELIVERY : OrderStatus.TRANSFERRING;
            }
            case TRANSFERRING -> OrderStatus.AT_DELIVERY_POINT;
            case AT_DELIVERY_POINT -> OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> throw new IllegalStateException("Record courier delivery evidence first.");
            case DELIVERED -> throw new IllegalStateException("Waiting for the customer to acknowledge receipt.");
            case COMPLETED -> throw new IllegalStateException("This order is already completed.");
            case CANCELLED, LOST -> throw new IllegalStateException("This order is closed.");
        };
    }
}
