# Investigation Report: Change Password + Staff Email OTP

## Summary

**Password storage:** PBKDF2-SHA256 with a random 16-byte salt, 120,000 iterations.
Stored as `iterations:base64(salt):base64(hash)` in `User.passwordHash` (a `volatile String`).
`PasswordUtil.hashPassword/verifyPassword` handle all crypto. Passwords are NOT stored in plain text.

**No `changePassword` method exists in `AuthService`.** Only `PasswordResetService.reset()` (the forgot-password flow) changes a password today. The profile panels have no "Change Password" button at all.

**Staff registration currently skips email verification entirely.** The `staffRegistration` boolean in `SignupFrame` controls flow branching. When `true`, successful registration jumps straight to a "pending approval" dialog then `LoginFrame` — `EmailAuthFrame` is never opened for staff. The email-verify step in `AuthService.login()` only fires for `UserRole.CUSTOMER`, so a staff user with `emailVerified = false` can log in as soon as an admin approves the account.

---

## Feature 1 — Change Password

### Where each profile panel lives

| Role | File | Line range | Current state |
|------|------|-----------|---------------|
| Customer | `src/gui/customer/CustomerProfilePanel.java` | 1–200 | Shows avatar + username only. No editable fields, no "Change Password" button. |
| Staff | `src/gui/staff/StaffProfilePanel.java` | 1–50 | Shows 5 read-only fields (username, email, role, status, email-verified). No "Change Password" button. |
| Admin | `src/gui/admin/AdminDashboardPanel.java` | ~350–480 (`createProfileCardUI`, `showEditProfileDialog`) | Has an "Edit Profile" dialog that lets admin change username and profile picture only. No "Change Password" button. |

### Profile is displayed inside dashboards

- **Customer:** `CustomerDashboardPanel.java` (`src/gui/customer/CustomerDashboardPanel.java`) line ~54 — `content.add(profileContainer, "profile")`. The `profileContainer` holds a `CustomerProfilePanel(currentUser)`.
- **Staff:** `StaffDashboardPanel.java` line ~43 — `content.add(new StaffProfilePanel(user), "profile")`.
- **Admin:** `AdminDashboardPanel.java` line ~62 — `content.add(createProfilePanel(user), "profile")`.

### AuthService — no `changePassword` method

`src/service/AuthService.java` — methods present:
- `login()` (line 20)
- `verifyEmail()` (line 80)
- `updateUser()` (line 86)  ← persists any in-memory User change
- `registerCustomer()` / `registerStaff()` (lines 91–99)
- `reviewStaff()` (line 116)
- `getStaffApplications()` / `getCustomers()` (lines 107, 120)
- `passwordResets()` → returns `PasswordResetService` (line 17)

**No `changePassword(User, oldPassword, newPassword)` method exists.** One must be added.

### Password infrastructure already available

| File | Purpose |
|------|---------|
| `src/service/PasswordUtil.java` | `hashPassword(String)` → hashed string; `verifyPassword(String, String)` → boolean; `getPasswordValidationMessage(String)` → null if valid |
| `src/service/PasswordResetService.java` | Full reset flow with OTP. `reset()` calls `user.setPasswordHash(PasswordUtil.hashPassword(value))` — exact pattern to copy. |
| `src/model/User.java` line 47 | `setPasswordHash(String)` — public setter, already used by `PasswordResetService`. |
| `src/gui/auth/ForgotPasswordPanel.java` | Complete 3-step OTP UI (email → verify 6-digit code → new password with real-time validation). Can be adapted as a "Change Password" dialog. |

### What needs to be added for Change Password

1. **`AuthService.changePassword(User actor, char[] oldPassword, char[] newPassword, char[] confirmPassword)`**
   - Verify `oldPassword` against `actor.getPasswordHash()` using `PasswordUtil.verifyPassword()`.
   - Validate `newPassword` with `PasswordUtil.getPasswordValidationMessage()`.
   - Check `newPassword` == `confirmPassword`.
   - Call `actor.setPasswordHash(PasswordUtil.hashPassword(new String(newPassword)))`.
   - Zero out char arrays in `finally`.

2. **Add "Change Password" section to each profile panel:**
   - `CustomerProfilePanel.java` — add a "Change Password" button that opens a modal dialog.
   - `StaffProfilePanel.java` — same.
   - `AdminDashboardPanel.java` `showEditProfileDialog()` — add a "Change Password" button alongside the existing "Edit Profile" dialog (or a second dialog).

3. **A shared `ChangePasswordDialog` (new file: `src/gui/components/ChangePasswordDialog.java`):**
   - Three `JPasswordField`s: current password, new password, confirm new password.
   - Real-time validation using `PasswordUtil.getPasswordValidationMessage()` (mirror the checkmarks in `ForgotPasswordPanel`).
   - On confirm: call `authService.changePassword(user, ...)`.
   - Display success/error message.
   - Pattern: follow `ForgotPasswordPanel` stage-3 UI (lines 150–220 of `ForgotPasswordPanel.java`).

---

## Feature 2 — Staff Email Verification on Registration

### Current staff registration flow

1. **Entry point:** `LoginFrame.java` line ~360:
   ```java
   if (inputUser.equals("Rstaff") && inputPass.equals("Rstaff123!")) {
       dispose();
       new SignupFrame(authService, true).setVisible(true); // staffRegistration = true
       return;
   }
   ```
   This is a hard-coded backdoor credential, not an admin-issued invite.

2. **`SignupFrame(authService, true)` flow** (`src/gui/auth/SignupFrame.java`):
   - `staffRegistration` field (line 45) is `true`.
   - `handleRegistration()` (line ~400) calls `authService.registerStaff(...)`.
   - `AuthService.register()` (line ~175) creates the `User` with:
     - `emailVerified = false`
     - `status = AccountStatus.PENDING_APPROVAL`
   - Back in `SignupFrame.handleRegistration()` at line ~430:
     ```java
     if (staffRegistration) {
         JOptionPane.showMessageDialog(…"pending administrator approval"…);
         dispose();
         new LoginFrame(authService).setVisible(true);
         return;   // <-- EmailAuthFrame is NEVER opened for staff
     }
     // Only customers reach EmailAuthFrame:
     new EmailAuthFrame(authService, result.getUser()).setVisible(true);
     ```

3. **Login check for staff email verification** (`AuthService.login()` line ~53):
   ```java
   if (user.getRole() == UserRole.CUSTOMER && !user.isEmailVerified()) { … }
   ```
   The guard is **CUSTOMER-only**. A staff with `emailVerified = false` passes this check and only fails the `PENDING_APPROVAL` check (separate concern). Once an admin approves the account, the staff can log in with an unverified email forever.

### EmailAuthFrame — reusable, but customer-only wired today

`src/gui/auth/EmailAuthFrame.java`:
- Constructor `EmailAuthFrame(AuthService authService, User user)` (line 38) — accepts any `User`, not customer-specific.
- On successful verification it calls `authService.verifyEmail(user)` then opens `new CustomerFrame(...)` (line ~250) — **this must be changed** for staff to navigate to a "pending approval" message instead of a customer frame.
- `EmailAuthFrame` uses `EmailService::sendOtpEmail` via the `CodeSender` functional interface — the same email infrastructure works for staff.

### EmailService + OtpUtil

| File | Method | Used by |
|------|--------|---------|
| `src/backend/EmailService.java` | `sendOtpEmail(String email, String code)` | `EmailAuthFrame` for customer registration |
| `src/backend/EmailService.java` | `sendPasswordResetEmail(String email, String code)` | `PasswordResetService` for forgot-password |
| `src/backend/OtpUtil.java` | `generateOTP()` → 6-digit string | `PasswordResetService` |

`EmailAuthFrame` does **not** use `OtpUtil` — it generates the OTP inline with `SecureRandom` (line ~178 of `EmailAuthFrame.java`). Both approaches are identical in output; using `OtpUtil` would be cleaner but is not required.

### What needs to be added for Staff Email Verification

1. **`EmailAuthFrame` must support a post-verification destination other than `CustomerFrame`.**
   Current hard-coded jump at line ~250:
   ```java
   new CustomerFrame(authService, user).setVisible(true);
   ```
   Replace with a `Runnable onVerified` callback passed in via a new constructor:
   ```java
   public EmailAuthFrame(AuthService authService, User user, Runnable onVerified)
   ```
   Keep the existing two-arg constructor as a convenience wrapper that passes `() -> new CustomerFrame(authService, user).setVisible(true)` for backwards compatibility.

2. **`SignupFrame.handleRegistration()` staff branch** — instead of going straight to `LoginFrame`, open `EmailAuthFrame` with a staff-specific `onVerified` callback:
   ```java
   if (staffRegistration) {
       dispose();
       new EmailAuthFrame(authService, result.getUser(), () -> {
           JOptionPane.showMessageDialog(null,
               "Email verified. Your account is pending administrator approval.",
               "QueueTees", JOptionPane.INFORMATION_MESSAGE);
           new LoginFrame(authService).setVisible(true);
       }).setVisible(true);
       return;
   }
   ```

3. **`AuthService.login()` staff email check** — add a parallel check:
   ```java
   if (user.getRole() == UserRole.STAFF && !user.isEmailVerified()) {
       return LoginResult.failure(AuthStatus.EMAIL_NOT_VERIFIED,
           "Please verify your email before logging in.", user);
   }
   ```
   This check should sit **before** the `STAFF_NOT_APPROVED` check so a staff can re-enter `EmailAuthFrame` from `LoginFrame` (same path as customers, already handled at line ~360 of `LoginFrame.handleLogin()`).

4. **`LoginFrame.handleLogin()`** — the `EMAIL_NOT_VERIFIED` path already opens `EmailAuthFrame` for any role (line ~360):
   ```java
   if (result.getStatus() == AuthStatus.EMAIL_NOT_VERIFIED && result.getUser() != null) {
       dispose();
       new EmailAuthFrame(authService, result.getUser()).setVisible(true);
       return;
   }
   ```
   Once `EmailAuthFrame` accepts a callback, this line needs to pass a staff-specific callback when the user is staff:
   ```java
   User u = result.getUser();
   Runnable dest = u.getRole() == UserRole.STAFF
       ? () -> { /* "pending approval" dialog + LoginFrame */ }
       : () -> new CustomerFrame(authService, u).setVisible(true);
   new EmailAuthFrame(authService, u, dest).setVisible(true);
   ```

---

## Shared Infrastructure Summary

| Component | Location | Reusable for both features? |
|-----------|----------|-----------------------------|
| `PasswordUtil` | `src/service/PasswordUtil.java` | Yes — hash, verify, validate |
| `PasswordResetService` | `src/service/PasswordResetService.java` | Pattern reference only (no direct reuse needed) |
| `EmailService.sendOtpEmail` | `src/backend/EmailService.java` | Yes — staff email OTP |
| `EmailAuthFrame` | `src/gui/auth/EmailAuthFrame.java` | Yes — add `Runnable onVerified` callback |
| `ForgotPasswordPanel` stage-3 UI | `src/gui/auth/ForgotPasswordPanel.java` | Yes — copy the real-time validation checkmark pattern |
| `VerificationCodeInput` | `src/gui/components/VerificationCodeInput.java` | Yes — reuse in `ChangePasswordDialog` if OTP step is wanted |

---

## Conclusions and Recommendations

### Feature 1 — Change Password

- Add `AuthService.changePassword(User, char[], char[], char[])` — 15 lines of logic.
- Create `src/gui/components/ChangePasswordDialog.java` with a current-password field + new-password + confirm, real-time validation, call `authService.changePassword()`. Reuse the ForgotPasswordPanel stage-3 visual pattern (checkmark labels).
- Wire a "Change Password" button into each of the three profile panels:
  - `CustomerProfilePanel.java` (currently the simplest — just avatar + name, needs the most addition)
  - `StaffProfilePanel.java` (has 5 read-only fields, add button below)
  - `AdminDashboardPanel.java` `createProfileCardUI()` / `showEditProfileDialog()` (add button next to "Edit Profile")
- No new service classes needed beyond the `changePassword` method.

### Feature 2 — Staff Email OTP

- Add a `Runnable onVerified` callback parameter to `EmailAuthFrame` — minimal change, backwards-compatible.
- Change `SignupFrame` staff branch to open `EmailAuthFrame` instead of `LoginFrame`.
- Add an email-verified check for `UserRole.STAFF` in `AuthService.login()`.
- Update `LoginFrame.handleLogin()` to pass a role-appropriate `onVerified` callback.
- No new email infrastructure needed — `EmailService.sendOtpEmail` already exists.

### Risk note

`EmailAuthFrame.verifyCode()` currently calls `new CustomerFrame(authService, user).setVisible(true)` with no null guard on `authService`. Switching to a callback cleanly removes this coupling and makes the class role-agnostic.
