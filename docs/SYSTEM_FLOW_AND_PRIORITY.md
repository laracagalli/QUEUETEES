# System flow and scheduling

## Customer registration

Create account → Customer Terms & Conditions → verify email → customer dashboard.

Staff receive their own Staff Terms & Conditions, verify email, then await administrator approval. Terms do not appear on ordinary logins. If signup was abandoned before acceptance, signing in resumes that incomplete registration; it cannot silently bypass acceptance. Existing accounts created before this requirement are preserved.

Signup OTP codes expire after five minutes. Five incorrect attempts disable that challenge. Successful code deliveries impose a one-minute resend cooldown, including when a verification window is reopened in the same application session. Password-reset challenges are replaced by account ID rather than Java object identity; changing passwords invalidates the account's other reset tokens.

## Orders and payments

Checkout reserves stock transactionally and creates a Pending payment order. Entering a GCash/card reference does not prove payment. Staff or administrators check the actual payment record and choose **Verify payment**, supplying the amount received and a unique transaction reference. The amount must equal the order total. Verification records evidence; it does not call a payment gateway or transfer money. The storefront never requests a CVV or full card number; external card payments use the last four digits and reference only.

Verified prepaid orders enter packing. Legacy Cash orders may be prepared for collection, but no money is inferred from Cash, order completion, or reference text. Cash collection must be recorded using Verify payment before a refund can be created. Existing historical orders are not automatically declared paid during migration.

## Packing queue

The policy is **Shortest Job First**, using total pieces as the packing-work estimate. Ten pieces of one product count as ten, not one product line.

Priority: total pieces ascending → placement time ascending → permanent queue number → order ID. An order already being prepared is not interrupted when a smaller order arrives.

| Arrival | Pieces | Packing rank |
|---|---:|---:|
| Customer A, first | 10 | 3 |
| Customer B, second | 1 | 1 |
| Customer C, third | 2 | 2 |

The data structure is Java `PriorityQueue<Order>`, a binary min-heap. Insertion and removal of the highest-priority order take O(log n); reading the head takes O(1). The heap is rebuilt from current waiting orders so changed eligibility is reflected. Total-piece comparisons use `long`; dates use `LocalDateTime`; IDs/queue numbers use `int`.

Database preparation decisions use a named scheduling lock and order rows, with `SUM(order_items.quantity)` and the same tie-breakers. This coordinates multiple app instances. UI Waiting positions and Select next use the same policy. Permanent control numbers identify orders and do not change when packing rank changes.

Staff queue/details and customer tracking refresh their state when payment verification changes, even when the order stage is still Confirmed. This updates packing eligibility and waiting positions immediately. The compact Update stage dialog uses a combo box for the next allowed stage; it does not bypass courier evidence or customer receipt acknowledgment.

Update stage reloads the selected order before acting. Unpaid orders lead to payment verification and then the next-stage confirmation. Orders waiting behind a smaller packing job receive an explanation. Out-for-delivery orders lead to the courier-proof action; delivered orders explain that customer acknowledgment is required. Empty selections and closed orders receive feedback instead of silently doing nothing.

Strict smaller-first scheduling can make large orders wait when small orders continually arrive. This follows the requested rule; a future maximum-wait/aging policy would need an explicit business decision.

## Delivery queue from Pasay

Priority: estimated straight-line city distance from Pasay ascending → placement time → queue number → order ID. The data structure is another `PriorityQueue<Order>` using a `double` distance comparator. City coordinates are looked up in a `HashMap`.

Dispatch considers ready Central-zone deliveries and remote deliveries that have arrived at their delivery point. The nearest ready order must be dispatched first. An order that is not packed or has not reached its remote delivery point does not block ready deliveries. Dispatched deliveries are shown in nearest-first order, but genuine courier arrivals can be recorded in a different order; the system cannot control traffic or actual travel times.

The warehouse origin is the approximate Pasay city coordinate published by [Pasay City Government](https://pasay.gov.ph/ating-lungsod/city-profile/): 14°32′ N, 121° E. Destination coordinates come from [GeoNames](https://download.geonames.org/export/dump/), CC BY 4.0, downloaded October 15, 2026. The snapshot covers all 698 cities/municipalities in the bundled service area. Distances use the Haversine formula and are clearly labeled estimates. They are not barangay/street routing distances. Legacy unassigned addresses require review.

Stages must advance one at a time: Confirmed → Preparing → Ready → remote Transfer/Arrival where applicable → Out for delivery → courier-confirmed Delivered → customer's receipt acknowledgment → Completed.

## Returns, refunds and account restrictions

Return requests require a completed owned order, valid proof, unreturned quantity, and submission within 30 days of recorded delivery (completion date is the fallback for legacy/pickup records). Refund limits use the verified amount received. Admins record actual refund references; the ledger does not transfer money.

Sales reports include verified payments on completed orders and refunds against that same cohort. Completed orders with unverified payment remain in operational history but are excluded from sales until their payment is verified.

The administrator dashboard includes an all-time sales summary with gross sales, recorded paid refunds and net sales, plus the verified completed-order count. Dashboard and report totals share the same calculation. Returning to Dashboard or choosing Refresh sales reloads the summary; View sales reports opens the full report. Cancellation and lost-parcel refunds remain listed separately in Order refunds.

Profile edits, password changes/reset, email verification, terms acceptance and administrator decisions update only the fields they own. They preserve current account status and cannot resurrect suspended accounts from old snapshots. Admin decisions verify the current operator and expected status atomically in MySQL.

Customer dashboards display their actual account details and set the signed-in session actor after signup verification. Warnings remain in account records and the customer's saved inbox. A warning is not automatic proof of misconduct. Warning receipt and review do not suspend or remove accounts.

## Follow-up flow checks

Back to signup on either role's Terms page restores the entered account and application fields, including the masked password, birthday, gender and address. Submitting edits updates the same incomplete account and returns to Terms; it does not create another account. Completed, verified or restricted accounts cannot be edited through this route. The temporary password transfer is cleared when leaving Terms; credentials are not written to a settings file. Login accepts email addresses up to the same 254-character limit as registration. Signup and checkout require 10 contact digits starting with 9 after +63.

MySQL cart changes and checkout recheck email verification and any required registration terms. Legacy delivery-address corrections require active staff/admin access checked against the database, including suspensions from another session. Cart quantities must be positive, and addition uses a `long` calculation before the stock check to prevent integer overflow. Password-reset challenges are invalidated immediately on the fifth incorrect six-digit code.

Checkout restores the registered full name, email, phone and the province/city/barangay/street from a recognized saved registration address. Street commas are preserved. Unrecognized legacy addresses require locality selection rather than guessed routing. Place Order shows validation feedback, reveals the missing field, and saves in a background worker with the button disabled until completion. Failed saves retain the cart; successful saves show the control number and open tracking. Payment remains pending administrator/staff verification.
