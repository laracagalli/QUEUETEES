# Staff feature review

Source: **QUEUETEES DRAFT 1 (MAIN).pdf**, supplied by the user. Relevant sections: transactions (page 4), scope (page 5), limitations (page 6), and staff flowchart (page 12). The paper is a requirements reference; the user's current request separately adds completed-order report printing and GUI refinement.

## Coverage and remaining gaps

| Paper requirement | Current implementation | Assessment |
| --- | --- | --- |
| Approved staff can log in (pp. 5, 12) | `AuthService.login` checks staff account status before access. | Implemented for existing accounts. |
| Register staff and approve/reject applications (pp. 5, 12) | Signup calls customer registration. Admin Staff Approvals is a placeholder table with a disabled review action. | Missing end-to-end registration and approval workflow; requires authentication and admin changes. |
| Store personal information (pp. 5, 12) | `User` stores username/email/role/status, without the paper's separate name fields, contact, address, gender or birthday. | Model and registration storage need expansion. Printed staff identity currently uses the actual signed-in username, ID and email. |
| Ordered queue, order details, processing and status updates (pp. 4, 5, 12) | Queue, detail dialog, status board and progression actions use shared `StoreService` orders. | Implemented. Details include items, contact, fulfillment, notes and simulated payment method. |
| Chronological/FCFS handling (pp. 3, 6) | Earliest waiting order must start preparation first. Started orders can advance independently. | Start order is enforced. If the intended rule requires one order to finish before another starts, that stricter rule still needs to be agreed and implemented. |
| Pending -> Processing -> Completed (pp. 4, 12) | Confirmed -> Preparing -> Ready for pickup -> Completed. | Terminology differs. Pending maps to Confirmed; Processing spans Preparing and Ready. Ready is an additional fulfillment stage, including delivery orders. Update the paper or adopt one consistent state model across customer/staff/admin. |
| Local database and records across sessions (pp. 4-6, 12) | Store and user repositories are in memory. | Missing persistent local database. Restarting clears session orders; print reports cannot retrieve previous sessions. |
| Logout (p. 12) | Confirmed logout returns to login and disposes the staff window. | Implemented. Staff refresh timer stops when removed. |

The paper assigns inventory editing, low-stock alerts, customer security and sales analytics to administrators (pp. 4-5). They do not need to be added to the staff panel merely to match this draft. Staff can print their operational completed-order report under the user's new request.

## Implemented in this update

- Three-column queue board with counts, next waiting ticket, customer and fulfillment details, and direct order review.
- Consistent staff typography, search controls and action buttons.
- Read-only profile, activity and order summary text cannot acquire typing focus or show a text cursor. Search and completion-date filters remain editable.
- Completed Orders filters by inclusive completion date and literal text search. Preview uses the displayed rows in their current sort order.
- Paginated report with centered Hiraya logo, print timestamp including timezone offset, signed-in staff username/ID/email, filter scope, queue numbers, customer names, completion timestamps, fulfillment/payment methods, item quantities, total order values, report ID and page numbers.
- Preview and printer share one renderer and a stable snapshot. No records, invalid date ranges and missing staff identity block printing. Print errors are shown, and printer work runs off the Swing event thread.
- Purchased product names and prices are copied at checkout so catalog edits do not rewrite historical totals.

The paper's physical receipt-printer exclusion (p. 6) concerns checkout receipts. This update adds an ordinary operational report through the system print dialog, as requested. It does not add receipt-printer hardware integration.

## Verification

`StaffWorkflowTest` covers checkout, preparation order, stale updates, completion, customer tracking, literal search and refreshing panels. `StaffReportTest` covers report pagination, totals, immutable snapshots, historical prices, printer rendering, date filters and read-only controls. Tests render staff panels and report pages under `bin/review` for visual inspection. Physical printer output remains a manual environment check.

## Next priorities

1. Implement the staff application and administrator approval/rejection flow, including the required personal fields.
2. Persist users, catalog, carts, orders and transactions in a local database.
3. Reconcile status names and the precise FCFS policy between the paper and all application roles.
