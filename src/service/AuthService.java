package service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import model.AccountStatus;
import model.User;
import model.UserRole;
import repository.UserRepository;

public class AuthService {
    public static final String TERMS_VERSION = "2026-10-14";
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final java.util.Map<Integer,Long> emailResendAt=new java.util.HashMap<>();
    private final java.util.Map<Integer,Integer> emailAttempts=new java.util.HashMap<>();
    public synchronized boolean canResendEmail(User user){return System.currentTimeMillis()>=emailResendAt.getOrDefault(user.getId(),0L);}
    public synchronized void emailCodeSent(User user){emailResendAt.put(user.getId(),System.currentTimeMillis()+60000);emailAttempts.put(user.getId(),0);}
    public synchronized boolean emailCodeFailed(User user){return emailAttempts.merge(user.getId(),1,Integer::sum)>=5;}

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordResetService = new PasswordResetService(userRepository,
                backend.EmailService::sendPasswordResetEmail);
    }

    public PasswordResetService passwordResets() {
        return passwordResetService;
    }
    public boolean hasAcceptedTerms(User user) {
        if(user==null)return false;
        return userRepository.findByEmailOrUsername(user.getEmail()).filter(saved->saved.getId()==user.getId())
                .filter(saved->TERMS_VERSION.equals(saved.getTermsVersion()) && saved.getTermsAcceptedAt()!=null).isPresent();
    }
    public void acceptTerms(User user,boolean agreed) {
        if(!agreed)throw new IllegalArgumentException("Please check the agreement box before continuing.");
        if(user==null)throw new IllegalStateException("An account is required.");
        User saved=userRepository.findByEmailOrUsername(user.getEmail()).filter(v->v.getId()==user.getId())
                .orElseThrow(()->new IllegalStateException("Account was not found."));
        if(saved.getStatus()!=AccountStatus.ACTIVE && saved.getStatus()!=AccountStatus.PENDING_APPROVAL)
            throw new IllegalStateException("This account cannot continue. Please contact the administrator.");
        if(hasAcceptedTerms(saved)){
            user.setTermsAcceptance(saved.getTermsVersion(),saved.getTermsAcceptedAt());return;
        }
        saved=userRepository.recordTerms(saved.getId(),TERMS_VERSION,java.time.LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        user.setTermsAcceptance(saved.getTermsVersion(),saved.getTermsAcceptedAt());
    }

    public LoginResult login(String identifier, char[] passwordChars) {
        String cleanIdentifier = identifier == null ? "" : identifier.trim();
        String password = passwordChars == null ? "" : new String(passwordChars);

        try {
            if (cleanIdentifier.isEmpty() || password.isEmpty()) {
                return LoginResult.failure(
                        AuthStatus.EMPTY_FIELDS,
                        "Please enter your email/username and password.");
            }

            Optional<User> optionalUser = userRepository.findByEmailOrUsername(cleanIdentifier);
            if (optionalUser == null || !optionalUser.isPresent()) {
                return invalidCredentials();
            }

            User user = optionalUser.get();

            if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                SecurityIncidents.failedLogin(user);
                return invalidCredentials();
            }

            if (user.getStatus() == AccountStatus.SUSPENDED) {
                return LoginResult.failure(AuthStatus.ACCOUNT_SUSPENDED, "This account is suspended.");
            }

            if (user.getStatus() == AccountStatus.BANNED) {
                return LoginResult.failure(AuthStatus.ACCOUNT_BANNED, SecurityIncidents.PERMANENT_SUSPENSION_MESSAGE);
            }

            if (user.getStatus() == AccountStatus.REJECTED) {
                return LoginResult.failure(AuthStatus.ACCOUNT_REJECTED, "This account registration was rejected.");
            }
            if(user.isTermsRequired() && !hasAcceptedTerms(user))return LoginResult.failure(AuthStatus.REGISTRATION_INCOMPLETE,"Finish your account registration by accepting your terms.",user);

            // QueueTees requirement: customers must verify their email before customer
            // access.
            if (user.getRole() == UserRole.CUSTOMER && !user.isEmailVerified()) {
                return LoginResult.failure(
                        AuthStatus.EMAIL_NOT_VERIFIED,
                        "Please verify your email before logging in.",
                        user);
            }

            // QueueTees requirement: staff must verify their email before logging in.
            if (user.getRole() == UserRole.STAFF && !user.isEmailVerified()) {
                return LoginResult.failure(
                        AuthStatus.EMAIL_NOT_VERIFIED,
                        "Please verify your email before logging in.",
                        user);
            }

            // QueueTees requirement: staff must be approved by an administrator first.
            if (user.getRole() == UserRole.STAFF && user.getStatus() != AccountStatus.ACTIVE) {
                return LoginResult.failure(
                        AuthStatus.STAFF_NOT_APPROVED,
                        "Your staff account is still waiting for administrator approval.");
            }

            if (user.getStatus() != AccountStatus.ACTIVE) {
                return LoginResult.failure(
                        AuthStatus.INVALID_CREDENTIALS,
                        "This account cannot log in right now.");
            }

            ActivityLogger.setActor(user);
            SecurityIncidents.successfulLogin(user);
            ActivityLogger.record(user, "Logged in", "Signed in successfully");
            return LoginResult.success(user);
        } finally {
            if (passwordChars != null) {
                java.util.Arrays.fill(passwordChars, '\0');
            }
        }
    }

    private LoginResult invalidCredentials() {
        return LoginResult.failure(
                AuthStatus.INVALID_CREDENTIALS,
                "Invalid email/username or password.");
    }

    public void verifyEmail(User user) {
        User current=userRepository.findAll().stream().filter(v->v.getId()==user.getId()).findFirst().orElseThrow(()->new IllegalStateException("Account not found."));
        if(current.isTermsRequired() && !hasAcceptedTerms(current))throw new IllegalStateException("Accept your registration terms before verifying email.");
        User saved=userRepository.markEmailVerified(user.getId());user.setEmailVerified(saved.isEmailVerified());user.setStatus(saved.getStatus());
    }

    // ==========================================
    // NEW METHOD: Persist user profile updates
    // ==========================================
    public void updateUser(User user) {
        if (user != null) {
            User saved=userRepository.updateProfile(user);user.setStatus(saved.getStatus());user.setPasswordHash(saved.getPasswordHash());
        }
    }

    public RegistrationResult registerCustomer(
            String email, String fullname, String username, char[] passwordChars,
            String address, String contactnum, String gender, LocalDate birthday) {
        return register(email, fullname, username, passwordChars, address, contactnum, gender, birthday,
                UserRole.CUSTOMER);
    }

    public RegistrationResult registerStaff(
            String email, String fullname, String username, char[] passwordChars,
            String address, String contactnum, String gender, LocalDate birthday) {
        return register(email, fullname, username, passwordChars, address, contactnum, gender, birthday,
                UserRole.STAFF);
    }

    private void requireAdministrator(User actor) {
        if (actor == null || actor.getRole() != UserRole.ADMIN || actor.getStatus() != AccountStatus.ACTIVE
                || userRepository.findByEmailOrUsername(actor.getEmail()).filter(saved -> saved.getId() == actor.getId()
                    && saved.getRole() == UserRole.ADMIN && saved.getStatus() == AccountStatus.ACTIVE).isEmpty())
            throw new IllegalStateException("An active administrator account is required.");
    }

    public java.util.List<User> getStaffApplications(User actor) {
        requireAdministrator(actor);
        return userRepository.findAll().stream().filter(u -> u.getRole() == UserRole.STAFF)
                .collect(java.util.stream.Collectors.toList());
    }

    // ==========================================
    // NEW METHOD: Fetch customers for Admin UI
    // ==========================================
    public java.util.List<User> getCustomers(User actor) {
        requireAdministrator(actor);
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .collect(java.util.stream.Collectors.toList());
    }
    public java.util.List<User> getManagedUsers(User actor) {
        requireAdministrator(actor);
        return userRepository.findAll().stream().filter(u->u.getRole()!=UserRole.ADMIN).toList();
    }

    public void changePassword(User user, char[] currentPassword, char[] newPassword, char[] confirmPassword) {
        String current = currentPassword == null ? "" : new String(currentPassword);
        String next = newPassword == null ? "" : new String(newPassword);
        String confirm = confirmPassword == null ? "" : new String(confirmPassword);
        try {
            if (current.isEmpty() || next.isEmpty() || confirm.isEmpty())
                throw new IllegalArgumentException("Please fill in all password fields.");
            if (!PasswordUtil.verifyPassword(current, user.getPasswordHash()))
                throw new IllegalArgumentException("Current password is incorrect.");
            String validationMessage = PasswordUtil.getPasswordValidationMessage(next);
            if (validationMessage != null)
                throw new IllegalArgumentException(validationMessage + ".");
            if (!next.equals(confirm))
                throw new IllegalArgumentException("New passwords do not match.");
            User saved=userRepository.updatePassword(user.getId(),user.getPasswordHash(),PasswordUtil.hashPassword(next),false);
            user.setPasswordHash(saved.getPasswordHash());user.setStatus(saved.getStatus());
        } finally {
            if (currentPassword != null) java.util.Arrays.fill(currentPassword, '\0');
            if (newPassword != null) java.util.Arrays.fill(newPassword, '\0');
            if (confirmPassword != null) java.util.Arrays.fill(confirmPassword, '\0');
        }
    }

    public void suspendUser(User actor, int targetId, boolean suspend) {
        suspendUser(actor,targetId,suspend,"Administrator account review");
    }
    public void suspendUser(User actor, int targetId, boolean suspend,String reason) {
        requireAdministrator(actor);
        User target = userRepository.findAll().stream().filter(u -> u.getId() == targetId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (target.getRole() == UserRole.ADMIN)
            throw new IllegalStateException("Administrator accounts cannot be suspended.");
        if (suspend && target.getStatus() == AccountStatus.SUSPENDED)
            throw new IllegalStateException("This account is already suspended.");
        if (!suspend && target.getStatus() != AccountStatus.SUSPENDED)
            throw new IllegalStateException("This account is not suspended.");
        if(suspend && target.getStatus()!=AccountStatus.ACTIVE)throw new IllegalStateException("Only active accounts can be suspended. Pending accounts require approval.");
        if(reason==null || reason.trim().length()<5 || reason.length()>1000)throw new IllegalArgumentException("Provide a reason (5–1000 characters).");
        userRepository.updateStatus(target,actor,suspend?AccountStatus.SUSPENDED:AccountStatus.ACTIVE,suspend?"Account suspended":"Account reactivated",reason.trim());
    }

    public AccountStatus currentAccountStatus(User user) {
        return userRepository.findByEmailOrUsername(user.getEmail()).filter(saved->saved.getId()==user.getId())
                .map(User::getStatus).orElse(AccountStatus.REJECTED);
    }

    public void permanentlySuspendUser(User actor,int targetId,String reason) {
        requireAdministrator(actor);
        User target=userRepository.findAll().stream().filter(u->u.getId()==targetId).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Account was not found."));
        if(target.getRole()==UserRole.ADMIN || (target.getStatus()!=AccountStatus.ACTIVE && target.getStatus()!=AccountStatus.SUSPENDED))
            throw new IllegalStateException("Only active or temporarily suspended customer/staff accounts can be permanently suspended.");
        if(reason==null || reason.trim().length()<5 || reason.length()>1000)
            throw new IllegalArgumentException("Provide a reason (5–1000 characters).");
        userRepository.updateStatus(target,actor,AccountStatus.BANNED,"Account permanently suspended",reason.trim());
    }

    public void reviewStaff(User actor, int staffId, boolean approve) {
        requireAdministrator(actor);
        User staff = userRepository.findAll().stream().filter(u -> u.getId() == staffId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Staff account was not found."));
        if (staff.getRole() != UserRole.STAFF || staff.getStatus() != AccountStatus.PENDING_APPROVAL)
            throw new IllegalStateException("Only pending staff applications can be reviewed. Refresh and try again.");
        userRepository.updateStatus(staff,actor,approve?AccountStatus.ACTIVE:AccountStatus.REJECTED,approve?"Staff approved":"Staff rejected","Staff application review");
        // Send email notification to staff in background
        final String staffEmail = staff.getEmail();
        final String staffUsername = staff.getUsername();
        if(!Boolean.getBoolean("queuetees.suppressNotifications")) new Thread(() -> {
            try {
                if (approve) backend.EmailService.sendStaffApprovalEmail(staffEmail, staffUsername);
                else backend.EmailService.sendStaffRejectionEmail(staffEmail, staffUsername);
            } catch (Exception ignored) { }
        }, "staff-notify").start();
    }

    private synchronized RegistrationResult register(
            String email,
            String fullname,
            String username,
            char[] passwordChars,
            String address,
            String contactnum,
            String gender,
            LocalDate birthday, UserRole role) {
        return register(email,fullname,username,passwordChars,address,contactnum,gender,birthday,role,null);
    }

    public RegistrationResult reviseRegistration(User draft,String email,String fullname,String username,char[] password,
            String address,String contact,String gender,LocalDate birthday) {
        return register(email,fullname,username,password,address,contact,gender,birthday,draft.getRole(),draft);
    }

    private synchronized RegistrationResult register(String email,String fullname,String username,char[] passwordChars,
            String address,String contactnum,String gender,LocalDate birthday,UserRole role,User draft) {

        String cleanEmail = email == null ? "" : email.trim();
        String cleanUsername = username == null ? "" : username.trim();
        String cleanFullname = fullname == null ? "" : fullname.trim();
        String password = passwordChars == null ? "" : new String(passwordChars);

        try {
            if (cleanEmail.isEmpty() || cleanUsername.isEmpty() || cleanFullname.isEmpty() || password.isEmpty()) {
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS, "Please fill in all required fields.");
            }

            if (!RegistrationValidation.validEmail(cleanEmail)) {
                return RegistrationResult.failure(AuthStatus.INVALID_EMAIL, "Invalid Email.");
            }

            if (userRepository.findByEmailOrUsername(cleanEmail).filter(u->draft==null || u.getId()!=draft.getId()).isPresent()) {
                return RegistrationResult.failure(AuthStatus.DUPLICATE_EMAIL, "That email is already registered.");
            }

            if (userRepository.findByEmailOrUsername(cleanUsername).filter(u->draft==null || u.getId()!=draft.getId()).isPresent()) {
                return RegistrationResult.failure(AuthStatus.DUPLICATE_USERNAME, "That username is already taken.");
            }

            String passwordMessage = PasswordUtil.getPasswordValidationMessage(password);
            if (passwordMessage != null) {
                return RegistrationResult.failure(AuthStatus.WEAK_PASSWORD, passwordMessage + ".");
            }

            if (gender == null || gender.isEmpty()) {
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS, "Please select a gender.");
            }

            if (birthday == null) {
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS, "Please enter your birthday.");
            }

            String ageError = RegistrationValidation.birthdayError(birthday);
            if (!ageError.isEmpty()) {
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS,
                        ageError);
            }
            if (!RegistrationValidation.addressError(address).isEmpty())
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS, RegistrationValidation.addressError(address));

            if (!RegistrationValidation.contactError(contactnum).isEmpty()) {
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS,
                        RegistrationValidation.contactError(contactnum));
            }

            int newId = userRepository.nextId();
            User newUser = new User(
                    newId,
                    cleanUsername,
                    cleanEmail,
                    PasswordUtil.hashPassword(password),
                    role,
                    false,
                    role == UserRole.STAFF ? AccountStatus.PENDING_APPROVAL : AccountStatus.ACTIVE,
                    cleanFullname, address == null ? "" : address.trim(),
                    contactnum == null ? "" : contactnum.trim(), gender, birthday);

            newUser.setTermsRequired(true);
            if(draft!=null)return RegistrationResult.success(userRepository.reviseRegistration(draft,newUser));
            userRepository.save(newUser);
            return RegistrationResult.success(newUser);

        } finally {
            if (passwordChars != null) {
                java.util.Arrays.fill(passwordChars, '\0');
            }
        }
    }
}
