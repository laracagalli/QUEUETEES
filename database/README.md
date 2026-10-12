# QUEUETEES MySQL storage

The existing screens and workflows now use JDBC storage when launching `main`.
The in-memory mode remains available for automated tests or demonstrations using
`-Dqueuetees.storage=memory`.

## Run on your Workbench connection

1. Run `database/queuetees.sql` in Workbench if the seven tables have not been created.
2. Run `database/seed-defense-users.sql` to insert the defense accounts immediately.
3. Put the MySQL `root` password in ignored `database/database.local.properties`
   using `password=YOUR_MYSQL_PASSWORD`. This is the Workbench password, not a
   QueueTees login password. Then run **Run QueueTees** in VS Code; it connects
   without a password or progress popup.
4. Sign in with one of the existing defense accounts below.

| Username | QueueTees password | Role |
| --- | --- | --- |
| `admin` | `Admin123!` | Administrator |
| `staff` | `Staff123!` | Approved staff |
| `verifiedcustomer` | `Customer123!` | Verified customer |

All three accounts are active and email verified when first inserted. The seed
file contains salted PBKDF2 hashes compatible with `PasswordUtil`, not plaintext
passwords. Re-running it does not overwrite existing passwords or account states.
Startup also inserts missing defense accounts. It seeds the existing 23-product
catalog only when the products table is empty, preserving saved stock thereafter.

`database/application.properties` matches the supplied connection:
`127.0.0.1:3306`, database `queuetees`, username `root`.
Set `QUEUETEES_DB_PASSWORD` locally or create ignored `database/database.local.properties`
with `password=...`. The environment variable takes precedence. Do not commit that
local file. Java properties files use backslash escaping: write a literal backslash
as `\\`. A blank `password=` means the MySQL account has no password; use it only
if that is how your account is configured. Connection failures show an error;
the application does not switch to temporary storage.

## Stored data

| Table | Data |
| --- | --- |
| `users` | Registration/profile fields, password hashes, email verification, roles and approval/account states |
| `products` | Catalog prices, stock and image paths |
| `cart_items` | Customer cart quantities |
| `orders` | Queue number, checkout details, status, placement/completion dates |
| `order_items` | Purchased product name/price/image snapshots and quantities |
| `activity_logs` | Meaningful staff/admin actions and historical identities |
| `verification_challenges` | Reserved for future persistent verification challenges |

Verification-code requests remain temporary, matching the current workflow;
verified-email flags and reset passwords are saved. Checkout validates stock and
saves stock changes, order/item snapshots and cart removal in one transaction.
Preparation still enforces FIFO and stale-status checks across app instances.
Sales totals remain computed from purchased prices. Queue numbers continue
increasing after restart. Picture paths reference the same local/classpath assets.

The schema and account seed were executed against a separate MySQL 8.0 test
instance. Integration checks cover account updates, password reset persistence,
ID allocation, restart hydration, carts, checkout rollback/concurrent stock,
FIFO, historical prices/dates and audit records. The user's actual root connection
requires their locally configured password; no Workbench password was retrieved.

## Build and check (PowerShell)

```powershell
$javaSources = @(Get-ChildItem src, tests -Recurse -Filter *.java | ForEach-Object FullName)
& 'C:\Program Files\Java\jdk-24\bin\javac.exe' --release 21 -encoding UTF-8 -cp 'lib/*' -d bin @javaSources
& './scripts/test-mysql.ps1'
```

The test script uses a fresh data directory under ignored `bin/mysql-tests` and
port **3307**. It refuses to use an occupied port and shuts down only its own
server. It does not change the user's MySQL instance on port **3306**. JDK and MySQL
installation paths in the script match this machine; adjust them on another PC.

Connector/J 8.4.0 is bundled in `lib/mysql-connector-j-8.4.0.jar` for JDBC. SHA-256:
`d77962877d010777cff997015da90ee689f0f4bb76848340e1488f2b83332af5`.
Source: Maven Central, `com.mysql:mysql-connector-j:8.4.0`.
MySQL's driver documentation is at
[Connector/J installation](https://dev.mysql.com/doc/connector-j/en/connector-j-installing-maven.html).
The driver JAR includes its licensing information.
