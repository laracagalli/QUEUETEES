package service;
import database.Database;
import model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Reviewable observations. Flags are not proof of malicious intent or illegality. */
public final class SecurityIncidents {
    public record Incident(int id,int userId,String username,String reason,LocalDateTime reportedAt,boolean resolved,String resolution) { }
    public record WarningMessage(int id,LocalDateTime sentAt,String message,boolean read) { }
    public static final String WARNING_MESSAGE = "An administrator has flagged activity on your account. Please follow the system rules. Further misuse may result in permanent suspension.";
    public static final String PERMANENT_SUSPENSION_MESSAGE = "Your account has been permanently suspended due to confirmed misuse of the system. Please contact the administrator for details.";
    private static final Set<Integer> warned = new HashSet<>(), acknowledged = new HashSet<>();
    private static final Set<Integer> popupSeen=new HashSet<>();
    private static final Map<Integer,LocalDateTime> sentTimes=new HashMap<>();
    private static Database database;
    private static final List<Incident> memory=new ArrayList<>();
    private static final Map<String,Integer> failures=new LinkedHashMap<>();
    public static synchronized void useDatabase(Database db){database=db;}
    public static synchronized void failedLogin(User user) {
        String key=String.valueOf(user.getId());int count=failures.merge(key,1,Integer::sum);
        if(count==5)flag(user,"Five consecutive failed sign-in attempts for this account.");
        if(failures.size()>1000)failures.remove(failures.keySet().iterator().next());
    }
    public static synchronized void successfulLogin(User user){failures.remove(String.valueOf(user.getId()));}
    public static synchronized void flag(User user,String reason) {
        String username=user==null?"Unknown actor":user.getUsername();int userId=user==null?0:user.getId();
        if(database==null){memory.add(new Incident(memory.size()+1,userId,username,reason,LocalDateTime.now(),false,""));return;}
        database.transaction(c->{try(PreparedStatement s=c.prepareStatement("INSERT INTO security_incidents (user_id,username,reason,reported_at,resolution) VALUES (?,?,?,?,?)")){
            if(userId==0)s.setNull(1,Types.INTEGER);else s.setInt(1,userId);s.setString(2,username);s.setString(3,reason);s.setObject(4,LocalDateTime.now());s.setString(5,"");s.executeUpdate();
        }return null;});
    }
    public static synchronized List<Incident> incidents() {
        if(database==null)return List.copyOf(memory);
        return database.read(c->{List<Incident> result=new ArrayList<>();try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM security_incidents ORDER BY reported_at DESC,id DESC")){
            while(r.next())result.add(new Incident(r.getInt("id"),r.getInt("user_id"),r.getString("username"),r.getString("reason"),r.getObject("reported_at",LocalDateTime.class),r.getBoolean("resolved"),r.getString("resolution")));
        }return result;});
    }
    public static synchronized void sendWarning(User admin,int id) {
        if(admin==null || admin.getRole()!=UserRole.ADMIN || admin.getStatus()!=AccountStatus.ACTIVE)
            throw new IllegalStateException("Administrator access is required.");
        if(database==null){
            Incident incident=memory.stream().filter(v->v.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Warning not found."));
            if(incident.resolved() || incident.userId()<=0)throw new IllegalStateException("Select an open warning linked to a user.");
            if(!warned.add(id))throw new IllegalStateException("A warning has already been sent for this incident.");
            sentTimes.put(id,LocalDateTime.now());
            ActivityLogger.record(admin,"Usage warning sent",incident.username()+": incident #"+id);return;
        }
        database.transaction(c->{
            try(PreparedStatement s=c.prepareStatement("SELECT id FROM users WHERE id=? AND role='ADMIN' AND status='ACTIVE'")){
                s.setInt(1,admin.getId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Administrator access is required.");}
            }
            try(PreparedStatement s=c.prepareStatement("UPDATE security_incidents i JOIN users u ON u.id=i.user_id SET i.warning_sent=TRUE,i.warning_sent_at=?,i.warning_message=? WHERE i.id=? AND i.resolved=FALSE AND i.warning_sent=FALSE AND u.role<>'ADMIN' AND u.status='ACTIVE'")){
                s.setObject(1,LocalDateTime.now());s.setString(2,WARNING_MESSAGE);s.setInt(3,id);if(s.executeUpdate()!=1)throw new IllegalStateException("Warning already sent, reviewed, or account is not active. Refresh first.");
            }
            repository.JdbcActivityRepository.record(c,admin,"Usage warning sent","Incident #"+id);return null;
        });
    }
    public static synchronized List<Integer> pendingWarnings(int userId) {
        if(database==null)return memory.stream().filter(v->v.userId()==userId && warned.contains(v.id()) && !acknowledged.contains(v.id())).map(Incident::id).toList();
        return database.read(c->{List<Integer> ids=new ArrayList<>();
            try(PreparedStatement s=c.prepareStatement("SELECT id FROM security_incidents WHERE user_id=? AND warning_sent=TRUE AND warning_read=FALSE ORDER BY id")){
                s.setInt(1,userId);try(ResultSet r=s.executeQuery()){while(r.next())ids.add(r.getInt(1));}
            }return ids;
        });
    }
    public static synchronized List<WarningMessage> warningMessages(int userId) {
        if(database==null)return memory.stream().filter(i->i.userId()==userId && warned.contains(i.id()))
                .map(i->new WarningMessage(i.id(),sentTimes.getOrDefault(i.id(),i.reportedAt()),WARNING_MESSAGE,acknowledged.contains(i.id())))
                .sorted(Comparator.comparing(WarningMessage::sentAt).reversed().thenComparing(Comparator.comparingInt(WarningMessage::id).reversed())).toList();
        return database.read(c->{List<WarningMessage> result=new ArrayList<>();
            try(PreparedStatement s=c.prepareStatement("SELECT id,COALESCE(warning_sent_at,reported_at) AS sent_at,warning_message,warning_read FROM security_incidents WHERE user_id=? AND warning_sent=TRUE ORDER BY sent_at DESC,id DESC")){
                s.setInt(1,userId);try(ResultSet r=s.executeQuery()){while(r.next()){
                    String body=r.getString("warning_message");result.add(new WarningMessage(r.getInt("id"),r.getObject("sent_at",LocalDateTime.class),body==null || body.isBlank()?WARNING_MESSAGE:body,r.getBoolean("warning_read")));
                }}
            }return result;
        });
    }
    public static synchronized List<Integer> pendingPopupWarnings(int userId) {
        if(database==null)return pendingWarnings(userId).stream().filter(id->!popupSeen.contains(id)).toList();
        return database.read(c->{List<Integer> ids=new ArrayList<>();
            try(PreparedStatement s=c.prepareStatement("SELECT id FROM security_incidents WHERE user_id=? AND warning_sent=TRUE AND warning_read=FALSE AND warning_popup_seen=FALSE ORDER BY id")){
                s.setInt(1,userId);try(ResultSet r=s.executeQuery()){while(r.next())ids.add(r.getInt(1));}
            }return ids;
        });
    }
    public static synchronized void acknowledgePopup(int userId,int id) {
        if(database==null){if(memory.stream().anyMatch(i->i.id()==id && i.userId()==userId) && warned.contains(id))popupSeen.add(id);return;}
        database.transaction(c->{try(PreparedStatement s=c.prepareStatement("UPDATE security_incidents SET warning_popup_seen=TRUE WHERE id=? AND user_id=? AND warning_sent=TRUE")){
            s.setInt(1,id);s.setInt(2,userId);s.executeUpdate();
        }return null;});
    }
    public static synchronized Set<Integer> warnedIncidentIds() {
        if(database==null)return Set.copyOf(warned);
        return database.read(c->{Set<Integer> ids=new HashSet<>();
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT id FROM security_incidents WHERE warning_sent=TRUE")){while(r.next())ids.add(r.getInt(1));}
            return ids;
        });
    }
    public static synchronized void acknowledgeWarning(int userId,int id) {
        if(database==null){if(memory.stream().anyMatch(v->v.id()==id && v.userId()==userId) && warned.contains(id))acknowledged.add(id);return;}
        database.transaction(c->{try(PreparedStatement s=c.prepareStatement("UPDATE security_incidents SET warning_read=TRUE WHERE id=? AND user_id=? AND warning_sent=TRUE")){
            s.setInt(1,id);s.setInt(2,userId);s.executeUpdate();
        }return null;});
    }
    public static synchronized void resolve(User admin,int id,String explanation) {
        if(admin==null || admin.getRole()!=UserRole.ADMIN || admin.getStatus()!=AccountStatus.ACTIVE)throw new IllegalStateException("Administrator access is required.");
        if(explanation==null || explanation.trim().length()<5 || explanation.length()>1000)throw new IllegalArgumentException("Enter a review note (5–1000 characters).");
        if(database==null){for(int i=0;i<memory.size();i++){Incident v=memory.get(i);if(v.id()==id){if(v.resolved())throw new IllegalStateException("This incident is already reviewed.");memory.set(i,new Incident(v.id(),v.userId(),v.username(),v.reason(),v.reportedAt(),true,explanation.trim()));ActivityLogger.record(admin,"Incident reviewed","#"+id+": "+explanation);return;}}throw new IllegalArgumentException("Incident not found.");}
        database.transaction(c->{try(PreparedStatement s=c.prepareStatement("SELECT id FROM users WHERE id=? AND role='ADMIN' AND status='ACTIVE'")){s.setInt(1,admin.getId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Administrator access is required.");}}
            try(PreparedStatement s=c.prepareStatement("UPDATE security_incidents SET resolved=TRUE,resolution=?,reviewer_id=? WHERE id=? AND resolved=FALSE")){s.setString(1,explanation.trim());s.setInt(2,admin.getId());s.setInt(3,id);if(s.executeUpdate()!=1)throw new IllegalStateException("This incident has changed. Refresh first.");}
            repository.JdbcActivityRepository.record(c,admin,"Incident reviewed","#"+id+": "+explanation);return null;});
    }
}
