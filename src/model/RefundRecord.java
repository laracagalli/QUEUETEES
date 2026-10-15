package model;
import java.time.LocalDateTime;
public record RefundRecord(String source,int orderId,String kind,double amount,String status,String reference,String note,LocalDateTime requestedAt,LocalDateTime paidAt) { }
