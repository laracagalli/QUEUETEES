package repository;

import database.Database;
import java.sql.*;
import java.util.*;
import model.*;

public final class JdbcUserRepository implements UserRepository {
    private final Database database;
    public JdbcUserRepository(Database database) { this.database = database; }
    @Override public int nextId() { return 0; } // MySQL assigns IDs when new users are inserted.
    @Override public Optional<User> findByEmailOrUsername(String identifier) {
        if (identifier == null) return Optional.empty();
        return database.read(c -> {
            try (PreparedStatement s = c.prepareStatement("SELECT * FROM users WHERE email=? OR username=? ORDER BY id LIMIT 1")) {
                s.setString(1, identifier.trim()); s.setString(2, identifier.trim());
                try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(user(r)) : Optional.empty(); }
            }
        });
    }
    @Override public List<User> findAll() {
        return database.read(c -> {
            List<User> users = new ArrayList<>();
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM users ORDER BY id")) {
                while (r.next()) users.add(user(r));
            }
            return users;
        });
    }
    @Override public void save(User user) {
        persist(user,null,null,null);
    }
    @Override public User reviseRegistration(User expected,User input){return database.transaction(c->{
        User saved;
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM users WHERE id=? FOR UPDATE")){
            s.setInt(1,expected.getId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Account not found.");saved=user(r);}
        }
        UserRepository.requireEditableRegistration(saved,expected);
        try(PreparedStatement s=c.prepareStatement("SELECT id FROM users WHERE id<>? AND (email IN (?,?) OR username IN (?,?)) LIMIT 1")){
            s.setInt(1,saved.getId());s.setString(2,input.getEmail());s.setString(3,input.getUsername());s.setString(4,input.getEmail());s.setString(5,input.getUsername());
            try(ResultSet r=s.executeQuery()){if(r.next())throw new IllegalArgumentException("That email or username is already registered.");}
        }
        try(PreparedStatement s=c.prepareStatement("UPDATE users SET username=?,email=?,password_hash=?,full_name=?,address=?,contact_number=?,gender=?,birthday=? WHERE id=?")){
            s.setString(1,input.getUsername());s.setString(2,input.getEmail());s.setString(3,input.getPasswordHash());s.setString(4,input.getFullName());s.setString(5,input.getAddress());
            s.setString(6,input.getContactNumber());s.setString(7,input.getGender());s.setObject(8,input.getBirthday());s.setInt(9,saved.getId());s.executeUpdate();
        }
        User revised=byId(c,saved.getId());JdbcActivityRepository.record(c,revised,"Registration updated","Updated incomplete signup details");return revised;
    });}
    @Override public User updateProfile(User user){return database.transaction(c->{
        try(PreparedStatement s=c.prepareStatement("UPDATE users SET username=?,profile_picture=? WHERE id=? AND status='ACTIVE'")){
            s.setString(1,user.getUsername());s.setString(2,user.getProfilePicture());s.setInt(3,user.getId());if(s.executeUpdate()!=1)throw new IllegalStateException("Account access changed.");
        }User saved=byId(c,user.getId());JdbcActivityRepository.record(c,saved,"Profile updated","Updated account profile");return saved;
    });}
    @Override public User updatePassword(int id,String expectedHash,String hash,boolean reset){return database.transaction(c->{
        try(PreparedStatement s=c.prepareStatement("UPDATE users SET password_hash=? WHERE id=? AND password_hash=?")){
            s.setString(1,hash);s.setInt(2,id);s.setString(3,expectedHash);if(s.executeUpdate()!=1)throw new IllegalStateException("Password changed. Start again.");
        }User saved=byId(c,id);JdbcActivityRepository.record(c,saved,reset?"Password reset":"Password changed","Updated account password");return saved;
    });}
    @Override public User markEmailVerified(int id){return database.transaction(c->{
        try(PreparedStatement s=c.prepareStatement("UPDATE users SET email_verified=TRUE WHERE id=? AND status IN ('ACTIVE','PENDING_APPROVAL')")){
            s.setInt(1,id);if(s.executeUpdate()!=1)throw new IllegalStateException("Account access changed.");
        }return byId(c,id);
    });}
    private static User byId(Connection c,int id)throws SQLException{try(PreparedStatement s=c.prepareStatement("SELECT * FROM users WHERE id=?")){
        s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Account not found.");return user(r);}
    }}
    @Override public User recordTerms(int id,String version,java.time.LocalDateTime at){return database.transaction(c->{
        try(PreparedStatement s=c.prepareStatement("UPDATE users SET terms_version=?,terms_accepted_at=? WHERE id=? AND status IN ('ACTIVE','PENDING_APPROVAL')")){
            s.setString(1,version);s.setObject(2,at);s.setInt(3,id);if(s.executeUpdate()!=1)throw new IllegalStateException("Account access changed.");
        }User saved=byId(c,id);JdbcActivityRepository.record(c,saved,"Terms accepted","Accepted "+saved.getRole()+" terms "+version);return saved;
    });}
    @Override public User updateStatus(User target,User actor,AccountStatus status,String action,String reason){return database.transaction(c->{
        try(PreparedStatement s=c.prepareStatement("SELECT id FROM users WHERE id=? AND role='ADMIN' AND status='ACTIVE' FOR UPDATE")){
            s.setInt(1,actor.getId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Administrator access changed.");}
        }
        try(PreparedStatement s=c.prepareStatement("UPDATE users SET status=? WHERE id=? AND status=? AND role<>'ADMIN'")){
            s.setString(1,status.name());s.setInt(2,target.getId());s.setString(3,target.getStatus().name());if(s.executeUpdate()!=1)throw new IllegalStateException("Account changed. Refresh first.");
        }User saved=byId(c,target.getId());JdbcActivityRepository.record(c,actor,action,saved.getUsername()+": "+reason);return saved;
    });}
    @Override public void saveWithActivity(User user,User actor,String action,String details) {
        persist(user,actor,action,details);
    }
    private void persist(User user,User actor,String action,String details) {
        int id = database.transaction(c -> {
            String values = "username=?,email=?,password_hash=?,role=?,email_verified=?,status=?,full_name=?,address=?,contact_number=?,gender=?,birthday=?,profile_picture=?,terms_version=?,terms_accepted_at=?,terms_required=?";
            String sql = user.getId() == 0 ? "INSERT INTO users SET " + values + ",registered_at=?" : "UPDATE users SET " + values + " WHERE id=?";
            try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                s.setString(1, user.getUsername()); s.setString(2, user.getEmail()); s.setString(3, user.getPasswordHash());
                s.setString(4, user.getRole().name()); s.setBoolean(5, user.isEmailVerified()); s.setString(6, user.getStatus().name());
                s.setString(7, user.getFullName()); s.setString(8, user.getAddress()); s.setString(9, user.getContactNumber());
                s.setString(10, user.getGender()); s.setObject(11, user.getBirthday()); s.setString(12, user.getProfilePicture());
                s.setString(13,user.getTermsVersion());s.setObject(14,user.getTermsAcceptedAt());
                s.setBoolean(15,user.isTermsRequired());
                if (user.getId() == 0) s.setObject(16, user.getRegisteredAt()); else s.setInt(16, user.getId());
                s.executeUpdate();
                if (action != null) JdbcActivityRepository.record(c,actor,action,details);
                if (user.getId() > 0) return user.getId();
                try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Missing generated user ID"); return keys.getInt(1); }
            }
        });
        if (user.getId() == 0) user.assignDatabaseId(id);
    }
    private static User user(ResultSet r) throws SQLException {
        User user = new User(r.getInt("id"), r.getString("username"), r.getString("email"), r.getString("password_hash"),
                UserRole.valueOf(r.getString("role")), r.getBoolean("email_verified"), AccountStatus.valueOf(r.getString("status")),
                r.getString("full_name"), r.getString("address"), r.getString("contact_number"), r.getString("gender"),
                r.getObject("birthday", java.time.LocalDate.class), r.getObject("registered_at", java.time.LocalDateTime.class));
        user.setProfilePicture(r.getString("profile_picture"));
        user.setTermsAcceptance(r.getString("terms_version"),r.getObject("terms_accepted_at",java.time.LocalDateTime.class));user.setTermsRequired(r.getBoolean("terms_required"));return user;
    }
}
