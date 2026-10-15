package repository;

import java.util.Optional;
import model.User;

public interface UserRepository {
    Optional<User> findByEmailOrUsername(String identifier);
    void save(User user);
    java.util.List<User> findAll();
    static void requireEditableRegistration(User saved,User expected) {
        if(saved.getRole()==model.UserRole.ADMIN || saved.getRole()!=expected.getRole()
                || !saved.isTermsRequired() || saved.isEmailVerified() || saved.getTermsAcceptedAt()!=null
                || (saved.getStatus()!=model.AccountStatus.ACTIVE && saved.getStatus()!=model.AccountStatus.PENDING_APPROVAL)
                || !saved.getPasswordHash().equals(expected.getPasswordHash()))
            throw new IllegalStateException("Registration changed or is already complete. Please sign in again.");
    }
    static User revisedRegistration(User saved,User input) {
        User revised=new User(saved.getId(),input.getUsername(),input.getEmail(),input.getPasswordHash(),saved.getRole(),false,saved.getStatus(),
                input.getFullName(),input.getAddress(),input.getContactNumber(),input.getGender(),input.getBirthday(),saved.getRegisteredAt());
        revised.setTermsRequired(true);revised.setProfilePicture(saved.getProfilePicture());return revised;
    }
    default User reviseRegistration(User expected,User input) {
        User saved=findAll().stream().filter(v->v.getId()==expected.getId()).findFirst().orElseThrow();
        requireEditableRegistration(saved,expected);User revised=revisedRegistration(saved,input);
        saveWithActivity(revised,revised,"Registration updated","Updated incomplete signup details");return revised;
    }
    default User updateProfile(User user) {
        User saved=findAll().stream().filter(v->v.getId()==user.getId()).findFirst().orElseThrow(()->new IllegalStateException("Account not found."));
        if(saved.getStatus()!=model.AccountStatus.ACTIVE)throw new IllegalStateException("Account access changed.");
        saved.setUsername(user.getUsername());saved.setProfilePicture(user.getProfilePicture());saveWithActivity(saved,saved,"Profile updated","Updated account profile");return saved;
    }
    default User updatePassword(int id,String expectedHash,String hash,boolean reset) {
        User saved=findAll().stream().filter(v->v.getId()==id).findFirst().orElseThrow(()->new IllegalStateException("Account not found."));
        if(!java.util.Objects.equals(saved.getPasswordHash(),expectedHash))throw new IllegalStateException("Password changed. Start again.");
        saved.setPasswordHash(hash);saveWithActivity(saved,saved,reset?"Password reset":"Password changed","Updated account password");return saved;
    }
    default User markEmailVerified(int id) {
        User saved=findAll().stream().filter(v->v.getId()==id).findFirst().orElseThrow(()->new IllegalStateException("Account not found."));
        if(saved.getStatus()!=model.AccountStatus.ACTIVE && saved.getStatus()!=model.AccountStatus.PENDING_APPROVAL)throw new IllegalStateException("Account access changed.");
        saved.setEmailVerified(true);save(saved);return saved;
    }
    default User recordTerms(int id,String version,java.time.LocalDateTime at){User saved=findAll().stream().filter(v->v.getId()==id).findFirst().orElseThrow();
        if(saved.getStatus()!=model.AccountStatus.ACTIVE && saved.getStatus()!=model.AccountStatus.PENDING_APPROVAL)throw new IllegalStateException("Account access changed.");
        saved.setTermsAcceptance(version,at);saveWithActivity(saved,saved,"Terms accepted","Accepted "+saved.getRole()+" terms "+version);return saved;}
    default User updateStatus(User target,User actor,model.AccountStatus status,String action,String reason){
        User saved=findAll().stream().filter(v->v.getId()==target.getId()).findFirst().orElseThrow();
        if(saved.getStatus()!=target.getStatus())throw new IllegalStateException("Account changed. Refresh first.");
        saved.setStatus(status);saveWithActivity(saved,actor,action,saved.getUsername()+": "+reason);return saved;
    }
    default int nextId() { return findAll().stream().mapToInt(User::getId).max().orElse(0) + 1; }
    default void saveWithActivity(User user, User actor, String action, String details) {
        save(user); service.ActivityLogger.record(actor,action,details);
    }
}
