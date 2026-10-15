package service;
import database.Database;
import model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

/** Evidence-backed requests; a recorded refund never claims to transfer money through a gateway. */
public final class ReturnsService {
    public static final int RETURN_WINDOW_DAYS=30;
    private static Database database;
    private static final List<ReturnRequest> memory=new ArrayList<>();
    private ReturnsService() { }
    public static synchronized void useDatabase(Database db){database=db;}
    private static void operator(User user,boolean refund) {
        if(user==null || user.getStatus()!=AccountStatus.ACTIVE || user.getRole()==UserRole.CUSTOMER
                || refund && user.getRole()!=UserRole.ADMIN)throw new IllegalStateException("An active "+(refund?"administrator":"staff or administrator")+" account is required.");
    }
    private static void active(Connection c,User user)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT role FROM users WHERE id=? AND status='ACTIVE'")){
            s.setInt(1,user.getId());try(ResultSet r=s.executeQuery()){if(!r.next()||!r.getString(1).equals(user.getRole().name()))throw new IllegalStateException("Active account access is required.");}
        }
    }
    private static ReturnRequest row(ResultSet r)throws SQLException {
        return new ReturnRequest(r.getInt("id"),r.getInt("order_id"),r.getInt("customer_id"),r.getInt("product_id"),r.getInt("quantity"),r.getString("reason"),r.getBytes("proof"),r.getString("proof_name"),r.getString("status"),r.getObject("requested_at",LocalDateTime.class),r.getString("resolution"),r.getString("refund_reference"),r.getDouble("refund_amount"));
    }
    private static List<ReturnRequest> list(Connection c)throws SQLException {
        List<ReturnRequest> result=new ArrayList<>();try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM return_requests ORDER BY requested_at DESC,id DESC")){while(r.next())result.add(row(r));}return result;
    }
    private static List<ReturnRequest> owned(Connection c,int customer)throws SQLException {
        List<ReturnRequest> result=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM return_requests WHERE customer_id=? ORDER BY requested_at DESC,id DESC")){
            s.setInt(1,customer);try(ResultSet r=s.executeQuery()){while(r.next())result.add(row(r));}
        }return result;
    }
    public static synchronized List<ReturnRequest> requests(User user) {
        if(user==null || user.getStatus()!=AccountStatus.ACTIVE)throw new IllegalStateException("Active account access is required.");
        List<ReturnRequest> all=database==null?List.copyOf(memory):database.read(c->{active(c,user);return user.getRole()==UserRole.CUSTOMER?owned(c,user.getId()):list(c);});
        return all.stream().filter(r->user.getRole()!=UserRole.CUSTOMER || r.customerId()==user.getId()).toList();
    }
    public static byte[] proof(java.nio.file.Path path) throws java.io.IOException {
        if(java.nio.file.Files.size(path)>5*1024*1024)throw new IllegalArgumentException("Proof image must be 5 MB or smaller.");
        byte[] bytes=java.nio.file.Files.readAllBytes(path);validateProof(bytes);return bytes;
    }
    private static void validateProof(byte[] proof) {
        if(proof==null || proof.length==0 || proof.length>5*1024*1024)throw new IllegalArgumentException("Attach a JPEG or PNG proof image, up to 5 MB.");
        boolean png=proof.length>8&&proof[0]==(byte)137&&proof[1]==80&&proof[2]==78&&proof[3]==71;
        boolean jpeg=proof.length>3&&proof[0]==(byte)255&&proof[1]==(byte)216&&proof[2]==(byte)255;
        if(!png&&!jpeg)throw new IllegalArgumentException("Proof must be a JPEG or PNG image.");
        try { if(ImageIO.read(new ByteArrayInputStream(proof))==null)throw new IllegalArgumentException("Proof image cannot be decoded."); }
        catch(java.io.IOException ex){throw new IllegalArgumentException("Proof image cannot be decoded.");}
    }
    private static CartItem eligible(Order order,User user,int product,int quantity,String reason,List<ReturnRequest> requests) {
        if(user==null || user.getRole()!=UserRole.CUSTOMER || user.getStatus()!=AccountStatus.ACTIVE || order.getCustomerId()!=user.getId())throw new IllegalStateException("Only the order's customer may request a return.");
        if(order.getStatus()!=OrderStatus.COMPLETED)throw new IllegalStateException("Returns require a completed order.");
        LocalDateTime received=order.getCourierConfirmedAt()!=null?order.getCourierConfirmedAt():order.getCompletedAt();
        if(received==null || LocalDateTime.now().isAfter(received.plusDays(RETURN_WINDOW_DAYS)))throw new IllegalStateException("The 30-day return period from recorded delivery has ended.");
        if(reason==null || reason.trim().length()<10 || reason.length()>1000)throw new IllegalArgumentException("Explain the issue in 10–1000 characters.");
        CartItem item=order.getItems().stream().filter(i->i.getProduct().getId()==product).findFirst().orElseThrow(()->new IllegalArgumentException("Select an item from this order."));
        int reserved=requests.stream().filter(r->r.orderId()==order.getId()&&r.productId()==product&&!r.status().equals("REJECTED")).mapToInt(ReturnRequest::quantity).sum();
        if(quantity<=0 || quantity>item.getQuantity()-reserved)throw new IllegalArgumentException("Return quantity exceeds the unreturned purchased quantity.");
        return item;
    }
    public static synchronized ReturnRequest submit(User user,int orderId,int product,int quantity,String reason,byte[] proof,String filename) {
        if(user==null || user.getStatus()!=AccountStatus.ACTIVE || user.getRole()!=UserRole.CUSTOMER)throw new IllegalStateException("Active customer access is required.");
        validateProof(proof);
        if(filename==null || filename.isBlank() || filename.length()>255)throw new IllegalArgumentException("Proof filename is required (up to 255 characters).");
        Order order=StoreService.getInstance().getOrdersForCustomer(user.getId()).stream().filter(o->o.getId()==orderId).findFirst().orElseThrow(()->new IllegalArgumentException("Order was not found."));
        LocalDateTime now=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if(database==null){eligible(order,user,product,quantity,reason,memory);ReturnRequest request=new ReturnRequest(memory.size()+1,orderId,user.getId(),product,quantity,reason.trim(),proof,filename,"SUBMITTED",now,"","",0);memory.add(request);return request;}
        return database.transaction(c->{active(c,user);
            try(PreparedStatement s=c.prepareStatement("SELECT status FROM orders WHERE id=? AND customer_id=? FOR UPDATE")){
                s.setInt(1,orderId);s.setInt(2,user.getId());try(ResultSet r=s.executeQuery()){if(!r.next()||!r.getString(1).equals("COMPLETED"))throw new IllegalStateException("Returns require your completed order.");}
            }
            eligible(order,user,product,quantity,reason,list(c));
            try(PreparedStatement s=c.prepareStatement("INSERT INTO return_requests (order_id,customer_id,product_id,quantity,reason,proof,proof_name,requested_at,resolution) VALUES (?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                s.setInt(1,orderId);s.setInt(2,user.getId());s.setInt(3,product);s.setInt(4,quantity);s.setString(5,reason.trim());s.setBytes(6,proof);s.setString(7,filename);s.setObject(8,now);s.setString(9,"");s.executeUpdate();
                try(ResultSet r=s.getGeneratedKeys()){r.next();return new ReturnRequest(r.getInt(1),orderId,user.getId(),product,quantity,reason.trim(),proof,filename,"SUBMITTED",now,"","",0);}
            }
        });
    }
    private static ReturnRequest transition(ReturnRequest request,String next,String note,String reference,Order order) {
        boolean allowed=request.status().equals("SUBMITTED")&&(next.equals("APPROVED")||next.equals("REJECTED"))
            || request.status().equals("APPROVED")&&next.equals("RECEIVED") || request.status().equals("RECEIVED")&&next.equals("REFUNDED");
        if(!allowed)throw new IllegalStateException("The return request changed or this transition is not allowed.");
        if(note==null || note.trim().length()<5 || note.length()>1000)throw new IllegalArgumentException("Provide a review / resolution note (5–1000 characters).");
        double amount=0;
        if(next.equals("REFUNDED")){
            if(reference==null || !reference.trim().matches("[A-Za-z0-9][A-Za-z0-9 ./_-]{3,99}"))throw new IllegalArgumentException("Enter a refund transaction reference (4–100 characters).");
            amount=order.getItems().stream().filter(i->i.getProduct().getId()==request.productId()).findFirst().orElseThrow().getProduct().getPrice()*request.quantity();
        }
        String history=request.resolution().isBlank()?"":request.resolution()+"\n";
        return request.resolved(next,history+LocalDateTime.now()+" / "+next+": "+note.trim(),next.equals("REFUNDED")?reference.trim():"",amount);
    }
    private static void refund(Connection c,ReturnRequest request,Order order,User actor)throws SQLException{
        if(!request.status().equals("RECEIVED")&&!request.status().equals("REFUNDED"))return;
        double amount=model.ReturnPresentation.expected(order,request);
        RefundService.request(c,order,"RETURN:"+request.id(),"RETURN",amount,request.resolution());
        if(request.status().equals("REFUNDED")&&amount>0)RefundService.pay(c,"RETURN:"+request.id(),request.refundReference(),request.resolution().substring(Math.max(0,request.resolution().length()-1000)),actor);
    }
    public static synchronized ReturnRequest review(User user,int id,String next,String note,String reference) {
        operator(user,next.equals("REFUNDED"));
        if(database==null){for(int i=0;i<memory.size();i++)if(memory.get(i).id()==id){ReturnRequest r=memory.get(i);Order o=StoreService.getInstance().getOrders().stream().filter(v->v.getId()==r.orderId()).findFirst().orElseThrow();ReturnRequest updated=transition(r,next,note,reference,o);try{refund(null,updated,o,user);}catch(SQLException e){throw new IllegalStateException(e);}memory.set(i,updated);ActivityLogger.record(user,"Return "+next.toLowerCase(),"Request #"+id+": "+note);return updated;}throw new IllegalArgumentException("Return request was not found.");}
        return database.transaction(c->{active(c,user);ReturnRequest r;
            try(PreparedStatement s=c.prepareStatement("SELECT * FROM return_requests WHERE id=? FOR UPDATE")){s.setInt(1,id);try(ResultSet result=s.executeQuery()){if(!result.next())throw new IllegalArgumentException("Return request was not found.");r=row(result);}}
            Order order=StoreService.getInstance().getOrders().stream().filter(o->o.getId()==r.orderId()).findFirst().orElseThrow();ReturnRequest updated=transition(r,next,note,reference,order);
            refund(c,updated,order,user);
            try(PreparedStatement s=c.prepareStatement("UPDATE return_requests SET status=?,resolution=?,refund_reference=?,refund_amount=?,reviewed_at=?,reviewer_id=? WHERE id=?")){
                s.setString(1,updated.status());s.setString(2,updated.resolution());s.setString(3,updated.refundReference());s.setDouble(4,updated.refundAmount());s.setObject(5,LocalDateTime.now());s.setInt(6,user.getId());s.setInt(7,id);s.executeUpdate();
            }
            repository.JdbcActivityRepository.record(c,user,"Return "+next.toLowerCase(),"Request #"+id+": "+note);return updated;
        });
    }
}
