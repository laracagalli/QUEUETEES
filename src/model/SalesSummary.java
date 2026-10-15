package model;

import java.util.*;

/** All-time sales from verified completed orders and paid refunds for those orders. */
public record SalesSummary(int completedOrders,double grossSales,double recordedRefunds) {
    public double netSales(){return grossSales-recordedRefunds;}
    public static SalesSummary from(Collection<Order> orders,Collection<RefundRecord> refunds){
        Set<Integer> included=new HashSet<>();double gross=0;
        for(Order order:orders)if(order.getStatus()==OrderStatus.COMPLETED&&order.getPaymentStatus()==PaymentStatus.VERIFIED){
            included.add(order.getId());gross+=order.getPaidAmount();
        }
        double paidRefunds=refunds.stream().filter(r->included.contains(r.orderId())&&r.status().equals("PAID")).mapToDouble(RefundRecord::amount).sum();
        return new SalesSummary(included.size(),gross,paidRefunds);
    }
}
