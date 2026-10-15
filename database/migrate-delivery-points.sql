-- Run once in Workbench against existing installations. Existing records are preserved.
USE queuetees;
ALTER TABLE orders
    ADD COLUMN address_province VARCHAR(120) NULL,
    ADD COLUMN address_city VARCHAR(120) NULL,
    ADD COLUMN address_barangay VARCHAR(160) NULL,
    ADD COLUMN address_street VARCHAR(100) NULL,
    MODIFY COLUMN status ENUM('CONFIRMED','PREPARING','READY_FOR_PICKUP','TRANSFERRING','AT_DELIVERY_POINT','OUT_FOR_DELIVERY','DELIVERED','COMPLETED') NOT NULL DEFAULT 'CONFIRMED';
