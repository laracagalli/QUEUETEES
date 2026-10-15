import model.*;
import repository.*;
import service.*;
import java.util.*;
import java.time.*;

public final class SystemHolesTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void blocked(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Unsafe action succeeded");}
    static class SnapshotUsers implements UserRepository {
        final Map<Integer,User> rows=new LinkedHashMap<>();
        static User copy(User u){User v=new User(u.getId(),u.getUsername(),u.getEmail(),u.getPasswordHash(),u.getRole(),u.isEmailVerified(),u.getStatus());v.setTermsAcceptance(u.getTermsVersion(),u.getTermsAcceptedAt());v.setTermsRequired(u.isTermsRequired());return v;}
        public void save(User u){rows.put(u.getId(),copy(u));}
        public Optional<User> findByEmailOrUsername(String text){return rows.values().stream().filter(u->u.getEmail().equalsIgnoreCase(text)||u.getUsername().equalsIgnoreCase(text)).findFirst().map(SnapshotUsers::copy);}
        public List<User> findAll(){return rows.values().stream().map(SnapshotUsers::copy).toList();}
    }
    public static void main(String[] args)throws Exception {
        SnapshotUsers users=new SnapshotUsers();AuthService auth=new AuthService(users);
        User admin=new User(1,"admin","admin@example.com",PasswordUtil.hashPassword("Admin123!"),UserRole.ADMIN,true,AccountStatus.ACTIVE);
        User customer=new User(2,"customer","customer@example.com",PasswordUtil.hashPassword("Customer123!"),UserRole.CUSTOMER,true,AccountStatus.ACTIVE);users.save(admin);users.save(customer);
        User stale=users.findByEmailOrUsername("customer").orElseThrow();
        String[] code={null};long[] now={System.currentTimeMillis()};Clock clock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return Instant.ofEpochMilli(now[0]);}};
        PasswordResetService reset=new PasswordResetService(users,(email,otp)->code[0]=otp,clock);
        String old=reset.request(customer.getEmail()),oldCode=code[0];now[0]+=61000;String token=reset.request(customer.getEmail());blocked(()->reset.verify(old,oldCode));reset.verify(token,code[0]);
        auth.permanentlySuspendUser(admin,2,"Confirmed repeated misuse");blocked(()->auth.updateUser(stale));reset.reset(token,"Changed123!".toCharArray(),"Changed123!".toCharArray());
        check(auth.currentAccountStatus(customer)==AccountStatus.BANNED,"Password reset preserves permanent suspension");blocked(()->auth.verifyEmail(stale));
        User pending=new User(3,"pending","pending@example.com",PasswordUtil.hashPassword("Customer123!"),UserRole.CUSTOMER,false,AccountStatus.ACTIVE);pending.setTermsRequired(true);users.save(pending);
        check(auth.login("pending","Customer123!".toCharArray()).getStatus()==AuthStatus.REGISTRATION_INCOMPLETE,"Abandoned signup cannot bypass terms");blocked(()->auth.verifyEmail(pending));
        auth.acceptTerms(pending,true);auth.verifyEmail(pending);check(auth.login("pending","Customer123!".toCharArray()).isSuccess(),"Completed registration permits ordinary login");
        auth.emailCodeSent(pending);check(!auth.canResendEmail(pending),"OTP resend cooldown");for(int i=0;i<4;i++)check(!auth.emailCodeFailed(pending),"Attempts remain below five");check(auth.emailCodeFailed(pending),"Fifth incorrect OTP exhausts challenge");
        System.out.println("PASS: suspension-safe updates, reset replacement invalidation, signup terms enforcement and OTP limits");
    }
}
