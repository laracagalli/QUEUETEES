package model;
import java.time.LocalDateTime;
public record ReturnRequest(int id,int orderId,int customerId,int productId,int quantity,String reason,
        byte[] proof,String proofName,String status,LocalDateTime requestedAt,String resolution,String refundReference,double refundAmount) {
    public ReturnRequest { proof=proof.clone(); }
    @Override public byte[] proof() { return proof.clone(); }
    public ReturnRequest resolved(String next,String note,String reference,double amount) {
        return new ReturnRequest(id,orderId,customerId,productId,quantity,reason,proof,proofName,next,requestedAt,note,reference,amount);
    }
}
