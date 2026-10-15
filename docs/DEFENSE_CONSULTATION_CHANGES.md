# Defense consultation changes

Implemented alongside the three independently operating delivery points. Preparation remains FIFO; delivery completion never blocks another region's ready order.

| Area | Implemented behavior |
| --- | --- |
| Registration email | Shared syntax validation rejects missing/multiple @ signs, malformed local parts, invalid domain labels and missing letter TLDs before opening OTP. Live label: Invalid Email. OTP verifies mailbox ownership; syntax alone does not establish deliverability. |
| Age | Backend and date picker enforce ages 18–120 inclusive. Future birthdays and ages over 120 are rejected. No ID approval, as requested. |
| Full address | Sign-up uses the covered province/city/barangay directory plus required house/street. Checkout independently validates its structured delivery address. |
| Track Order | Exact title Track Order. Only active orders appear in the selector. No queue-position count. Random permanent QT control numbers identify orders across customer, staff and admin screens. Internal sequence remains available to enforce production FIFO. |
| Product sizes | XS, S, M, L, XL, XXL and One Size are supported. Each size is a separate catalog/inventory record with its own product ID and stock. The shop groups size variants under one product card. Unstocked sizes are disabled for purchase. Cart and historical line items preserve the purchased size. |
| Existing inventory | Existing clothing stock defaults to M; accessories use One Size. No stock is fabricated for other sizes. Admin adds another size variant through Add Product, then replenishes that exact variant. Confirm these prototype defaults against actual inventory before real use. |
| History | Completed orders move out of Track Order into Order History. Customers can view full order details, purchased items/sizes/prices, fulfillment, payment display, notes, courier reference and receipt/print preview. |
| Delivery receipt | No timer guesses delivery. Staff record a courier delivery-confirmation reference; recording valid confirmation changes an out-for-delivery order to Delivered / awaiting acknowledgment and saves the delivery time/reference and staff identity. The customer completes it using Order Received; non-receipt can be reported while acknowledgment remains pending. This is manual recording of courier evidence, not a live courier API integration. Customers can select Order Received only for their own Delivered orders, without entering courier proof. Ready pickups can still be collected directly. |
| Returns/refunds | Customer selects a purchased item/size and quantity, explains the issue and attaches a decoded JPEG/PNG image (up to 5 MB). Completed ownership is required; cumulative requests cannot exceed purchased quantity. Staff approve/reject, then record receipt of the returned item. Admin records an already-issued or simulated refund reference, using the purchased price. Proof, statuses and references persist in MySQL. No payment gateway transfer is performed. |
| Stock | Manual decreases are rejected by service and JDBC repository. The + control only increases the pending quantity; Replenish submits a separate confirmed positive adjustment. Normal checkout still deducts purchased stock. Returning damaged merchandise does not silently replenish inventory. |
| User Management | One panel contains customers and staff, with role/status columns and suspension/reactivation reasons. Pending accounts cannot be activated by suspension controls. Staff Approvals retains application decisions but no suspension controls. Counts distinguish actual active/suspended accounts from pending verification/approval; no total-customer count is mislabeled as active/suspended. |
| Usage warnings | Five consecutive failed logins for a known account, prohibited stock adjustments and admin-reported observations generate reviewable warnings. Dashboard/activity log show unresolved counts. Admin records the review outcome. Flags do not automatically establish malicious intent, illegality or an account suspension. |
| Activity log | Combined text, role, action and inclusive date-range filters. Invalid date ranges show no printable records. Summaries include all matching records across pages. Database mode persists the log. |
| Dashboards | Admin and staff Overview labels are Dashboard. Admin adds actionable counts for returns, courier confirmations and usage warnings, with returns/user-management shortcuts and scrollable dashboard content. |
| Pagination | Ten rows per table page. Filtering and sorting operate on the complete matching result. Row actions map back to the correct model identity. Reports contain all filtered records; screen pagination does not cap reports at ten rows. |
| Reports | Preview images are scrollable. Catalog/detail reports include product pictures and sizes. Completed reports include purchased item descriptions and thumbnail images. Customer order receipt previews include full order facts. Natural paper pagination remains supported. |
| Staff stage shortcut | Update stage records completed work up to an applicable stage with one confirmation. It preserves FIFO and stale-update checks. Remote transfer and arrival require separate confirmations; shortcuts cannot complete delivery or bypass courier proof. |

## Running the updated app

Close QueueTees and run it again using Run QueueTees in VS Code. Startup adds missing database columns/tables and the unique control-number index without deleting existing records. The database account needs ALTER/CREATE permissions for the first updated launch. Keep src on the runtime classpath (or package its data and image resources into the JAR).

## Verification

Compile all source/test Java files and run scripts/test-regression.ps1. The consultation acceptance test covers email/age boundaries, size stock, random controls, completed history, courier proof, evidence/ownership/quantity checks for returns, historical refund amounts, warnings, date filtering and pagination, including cart removal from a later page.

scripts/test-mysql.ps1 uses an isolated temporary server on port 3307. It verifies stock concurrency, database upgrades, random-control/size persistence, courier proof, retained return images, refund state/reference/amount, and incident review. It does not modify the application database. Tests suppress staff-notification emails.
