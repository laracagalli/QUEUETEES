# Regional fulfillment

Verified orders with fewer total pieces begin packing first; arrival order breaks ties. The permanent Q-number identifies an order and is not its changing packing rank or delivery position. Preparation can overlap, and completed deliveries never block preparing another order. The main warehouse is in Pasay. See [the current scheduling rules](SYSTEM_FLOW_AND_PRIORITY.md).

After preparation:

- Pickup: ready for pickup at Central main hub → collected/completed.
- Central delivery: ready for dispatch → out for delivery → staff records delivery evidence → delivered / awaiting acknowledgment → customer selects Order Received → completed.
- Northern/Southern delivery: ready for dispatch → transferring to the assigned point → arrived at point → out for local delivery → staff records delivery evidence → delivered / awaiting acknowledgment → customer selects Order Received → completed.

Each ready order advances independently. For example, an earlier Northern order may remain in transfer while a later Central delivery or pickup completes. There is no rule requiring one region's deliveries to finish before another region can dispatch. Staff explicitly record arrival at the remote point; no arbitrary timer marks an order delivered. Completion requires collection or receipt, not dispatch.

Checkout uses province → city/municipality → barangay dropdowns plus house/street/subdivision text. Changing a parent selection resets its dependents. The backend rejects invalid or unsupported combinations before reserving stock or clearing the cart. Pickup does not require a delivery address.

The delivery board groups orders by delivery point. Order lists, details, customer tracking, history and receipts display the appropriate status and delivery point. Tracking shows only the stages applicable to that order. The preparation waiting count remains global and is explicitly labeled as preparation.

## Existing MySQL installations

Startup adds the four structured address columns and expands the status enum automatically, preserving existing orders. The database user needs ALTER permission for the first updated launch. If an administrator applies the change manually instead, use `database/migrate-delivery-points.sql` once against an old schema. Fresh installations use the updated `database/queuetees.sql`.

Older free-text delivery orders show Address requires review. Staff use View details → Review delivery address to choose a verified locality before dispatch. Completed or already-dispatched historical orders retain their original address. Existing pickup orders use the Central main hub.

Coverage defaults and snapshot provenance are documented in `src/data/README.md`. Hub street addresses and dispatch schedules are not yet configured.

## Checks

Compile sources and tests, then run `scripts/test-regression.ps1`. `gui.customer.RegionalDeliveryTest` tests regional independence, ready pickup collection, transfer stages, invalid-address stock/cart preservation and cascading address resets, and renders the changed screens into `bin/review`.

`scripts/test-mysql.ps1` runs against a separate temporary MySQL server on port 3307, testing persistence, regional transitions, completion dates, address review and repeated additive migration without touching the application database.

## Delivery priority

Packing uses total-piece priority. Ready deliveries are dispatched nearest to Pasay first using estimated city-level straight-line distance. Only Out for delivery orders enter the displayed dispatched queue. Staff can select the nearest dispatched destination from Order Queue. Genuine courier arrivals may still occur in a different order and are recorded independently. Staff evidence changes the order to Delivered; only its customer acknowledges receipt to complete delivery. No automatic delivery timers are used.
