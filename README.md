## Getting Started

## Staff panel

### Staff registration and approval

Choose **Register as staff** on the login screen. The shared signup form collects the application and saves it as **Pending approval**. Staff registration does not grant access or open the customer email-verification screen.

Sign in as an administrator, open **Staff Approvals**, select a pending row and choose **Review application**. Review the personal details, then **Approve**, **Reject**, or **Cancel**. Approved staff use the normal login screen to enter the staff dashboard; pending and rejected accounts remain blocked. Search, status filters, approval counts and Refresh use the current account repository. Customer signup still requires email verification.

Application details and decisions use in-memory storage and reset when the application restarts. For a demonstration, register staff, return to login, sign in as `admin` with `Admin123!`, approve the application, log out, and sign in with the new staff credentials in the same running application.

Check: `java -Djava.awt.headless=true -cp "bin;src;lib/*" StaffRegistrationTest`.

- Sign in with an approved staff account to view its username, email and account status.
- Overview shows waiting orders, orders being prepared or ready, today's completions and recent orders.
- Search Order Queue, Order Details or Completed Orders, select a row and choose **View details** to review contact information, fulfillment, payment method, notes and line items.
- Use **Start preparing**, **Mark ready**, then **Complete order**. Preparation starts with the earliest waiting order. Orders already being prepared or awaiting collection can progress independently.
- Queue Status shows preparation, the next waiting queue number and ready orders. Staff pages refresh every two seconds while open.
- Completed Orders records completion time. Staff updates are shared with customer order tracking in the same application process.

**Queue indicators:** Order Queue and Order Details show a **Waiting #** column. The highlighted **1** is the next order to begin preparation; a dash means preparation has already started. Positions are calculated from the full waiting queue and remain unchanged by search or sorting. Multiple item rows from one order share its position. **Select next waiting** clears the search and selects the next eligible order.

Status text uses amber for Confirmed, blue for Preparing, green for Ready for pickup and purple for Completed, with text labels retained. Overview separates waiting, preparing, ready and today's completed counts, and shows the latest ten orders in a table with a direct details action.

### Completed-order reports

Open **Completed Orders**, choose all dates or an inclusive completion-date range, and optionally search/sort the table. Choose **Preview / print report** to open the preview inside the staff panel, review its pages, then **Print report**. Use **Back to orders** to return. Use a configured printer or Windows' **Microsoft Print to PDF**. Empty results and invalid date ranges cannot print; hover over the disabled button for the reason.

Reports include the centered Hiraya logo, print date/time, signed-in staff username/ID/email, filter scope, order details, totals and page numbers. They capture the current visible results. Keep `src` on the runtime classpath so the logo is available. Purchased names/prices are preserved for historical reports.

See [the staff requirements review](docs/STAFF_REQUIREMENTS_REVIEW.md) for paper coverage, remaining registration/approval/database work, and the status terminology mismatch.

Orders and accounts currently use in-memory storage; restarting the application clears session data. Payment information is simulated, and the existing ready-for-pickup status is also used for delivery orders awaiting fulfillment.

### Build and verify (PowerShell, JDK 21)

```powershell
$javaSources = @(Get-ChildItem src, tests -Recurse -Filter *.java | ForEach-Object FullName)
javac -encoding UTF-8 -cp 'lib/*' -d bin @javaSources
java '-Djava.awt.headless=true' -cp bin StaffWorkflowTest
java '-Djava.awt.headless=true' -cp 'bin;src' StaffReportTest
java '-Djava.awt.headless=true' -cp 'bin;src' gui.staff.StaffQueuePresentationTest
java -cp 'bin;src;lib/*' main
```

The regression test runs in a fresh process and checks checkout, FIFO preparation, stale updates, completion, customer tracking, table search and panel refresh.

## Java workspace

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).

## Customer queue dashboard

Track Order uses green and cream summary cards, a four-stage progress display, product thumbnails, order updates and receipt information. The order selector includes only the signed-in customer's orders and retains the selected ticket as it completes. Waiting orders ahead counts earlier confirmed orders waiting to start preparation; no unmeasured time estimate is shown. The page refreshes every two seconds while visible and stops its timer when removed. Copy queue number copies only the selected ticket.

Run the customer tracking checks with:

```powershell
java '-Djava.awt.headless=true' -cp 'bin;src' gui.customer.CustomerTrackingTest
```

### Forgot password
The login screen's Forgot Password link opens email, verification-code, and new-password steps. Codes expire after 10 minutes, allow at most five attempts, and cannot be reused. A new request invalidates the previous code, with a one-minute request cooldown. Passwords use the signup validation rules. Account role, approval status and email-verification flags are preserved.

The sender Gmail address and app password are configured directly in `src/backend/EmailService.java`, as in the original project. Registration and password-reset emails share these settings; no Windows environment variables are required. Real SMTP delivery was not exercised by the automated tests.

If the email service cannot sign in, check the sender address and app password in `EmailService.java`. Email verification shows an inline delivery error and retry action; code entry stays disabled until sending succeeds. `gui.auth.EmailAuthPresentationTest` uses an injected sender to check failure/retry behavior and render the screen at normal and minimum sizes without sending real email.

Users and password changes currently live in the existing in-memory repository and do not survive application restarts. Durable password recovery requires persistent user storage.

Check: `java -Djava.awt.headless=true -cp "bin;src;lib/*" PasswordResetTest`.
