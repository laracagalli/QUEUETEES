# Administrator usage warnings

The system records suspicious activity as observations for human review. Five consecutive failed sign-in attempts create a flag. Administrators can also select a customer or staff account in User Management and choose **Flag for review**, entering a specific observation. A flag alone does not notify or suspend the user.

In **Review usage warnings**, select an open incident and choose **Send warning**. A confirmation previews the observed event and exact user message. The table shows **Sent** after the warning is saved, and the Activity Log records the action. Each incident can send one warning. Marking an incident reviewed does not cancel its pending user notice.

Customers and staff receive a JOptionPane in their dashboard on their next successful sign-in or within about five seconds while already signed in. The notice uses this approved text:

> An administrator has flagged activity on your account. Please follow the system rules. Further misuse may result in permanent suspension.

Every customer dashboard has a mail icon beside Log Out. Its badge shows the number of unread admin messages. Clicking the icon opens an inbox; each entry is labeled **Message from admin**, followed by its sent date and Read/Unread status. Selecting a message marks only that message read and reduces the badge. Read messages remain in history, including after an incident is marked reviewed or the app restarts. There is no delete action.

Closing a customer's warning popup records popup delivery separately and does not clear the inbox unread count. Staff continue to acknowledge their existing warning popup when they close it. Delivery and read status are persisted in MySQL and scoped to the intended account. Connection interruptions leave unacknowledged notices available for retry. The inbox stores the approved user warning text, not internal administrator review notes.

For confirmed misuse, select the account in User Management and choose **Permanently suspend**. Enter a reason and confirm the action. This uses the existing `BANNED` account status, blocks sign-in, and cannot be reversed with **Unsuspend**. Reasons are recorded in the Activity Log. The approved JOptionPane text is:

> Your account has been permanently suspended due to confirmed misuse of the system. Please contact the administrator for details.

An already-open customer or staff dashboard checks account status every five seconds and returns to login when access is revoked. A permanently suspended account sees the same message on subsequent sign-in attempts after password verification. Temporary suspension continues to use its existing message and can still be reversed with **Unsuspend**.

Restart the application after rebuilding. Startup adds warning delivery, read status, sent date, message text, and popup delivery columns to existing `security_incidents` tables without deleting records. New installations include the columns in `database/queuetees.sql`. Legacy sent messages with no stored send timestamp use their incident date; their text falls back to the approved warning message.
