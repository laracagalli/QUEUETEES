# Customer delivery issues and returns

The storefront offers delivery only. Pickup options have been removed from checkout; historical pickup records remain readable.

## Non-receipt

When delivery is recorded, the order awaits customer acknowledgment. A customer can report non-receipt instead. The report is marked Awaiting staff review; duplicate pending reports are blocked.

Staff open View order → Review non-receipt report. They can mark it Investigating, arrange redelivery, or record a response to the customer. Staff cannot acknowledge receipt on the customer's behalf. Redelivery moves the order back to Out for delivery and clears the previous courier reference. Fresh delivery evidence is required before the customer can acknowledge it. The tracking page shows the report's progress and the staff response. If the package arrives, the customer's acknowledgment resolves the report.

## Returns and refunds

Customers select purchased items and quantities from completed orders and attach photo evidence. Quantities already covered by a pending or successful request are unavailable for another request. Rejected quantities can be requested again.

1. Awaiting review: staff review the reason and proof.
2. Approved / send item back: staff must provide return instructions in the review note; the customer sends back the approved items.
3. Returned / awaiting refund: staff record receipt and inspection of returned merchandise.
4. Refunded: an administrator records the already-paid or simulated refund's reference and payment details. The amount is computed from purchased prices and the returned quantity. No payment gateway transfers money.

The Returns table separately shows Expected refund and Refund paid. Approval does not mean money was refunded. View request / proof shows the next step, retained review notes, evidence, amount and reference. Both returns and history refresh while open.

Order History shows return progress and full or partial returned/refunded outcomes. Original delivery completion dates and purchased totals remain intact for receipts; the history status is derived from the associated return requests. Returned damaged stock is not automatically added back to inventory.

Staff can also confirm a package lost after investigating non-receipt. The order closes as Lost; its recorded prepaid amount becomes Refund pending. No physical return is required, and lost merchandise is not restored to stock.

## Cancellation and shared refund records

Customers can cancel only Confirmed orders from Track Order. The backend rechecks ownership, active account access and order status under the same order-row lock used by preparation. It restores reserved quantities once, records the reason/time, and removes the order from active queues. A cancellation racing with preparation permits only one action to succeed.

GCash reference and masked Card ending records represent prepaid prototype payments for the full order amount. Cash or unspecified payments are treated as unpaid before completion, so cancellation/loss creates no refund for them. Completed historical orders are treated as payment recorded; no real gateway verification is performed.

Returns → Cancellation / lost-delivery refunds shows order-level pending and paid refunds to customers and operators. Only admin can record payment, with a unique reference and payment note. Returned-item refunds remain in their return-review flow, but use the same ledger. Total reserved/paid refunds cannot exceed the order's recorded payment; paid sources cannot be paid again. Existing recorded return refunds are imported into the ledger without changing receipts.

Sales Reports show gross sales, recorded refunds and net sales for the all-time completed-order cohort. Refunds against those same orders are deducted regardless of when they were recorded. Cancelled/lost orders never enter gross sales; their refunds are separately visible in Order refunds. Printed reports include this basis. Original receipt totals and completion dates remain intact.
