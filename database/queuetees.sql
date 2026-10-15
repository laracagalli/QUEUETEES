-- QUEUETEES schema based on src/model and src/service.
-- Run this entire file in MySQL Workbench against MySQL 8.0.16 or later.
-- This creates tables only; it does not connect the Java app or insert test users.
-- IF NOT EXISTS preserves existing tables; it does not upgrade old table definitions.

CREATE DATABASE IF NOT EXISTS queuetees
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE queuetees;

-- One account table covers customers, staff applications, staff and administrators.
CREATE TABLE IF NOT EXISTS users (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('CUSTOMER', 'STAFF', 'ADMIN') NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('ACTIVE', 'PENDING_APPROVAL', 'SUSPENDED', 'BANNED', 'REJECTED')
        NOT NULL DEFAULT 'ACTIVE',
    full_name VARCHAR(200) NOT NULL DEFAULT '',
    address TEXT NOT NULL,
    contact_number VARCHAR(30) NOT NULL DEFAULT '',
    gender VARCHAR(50) NOT NULL DEFAULT '',
    birthday DATE NULL,
    profile_picture VARCHAR(1024) NOT NULL DEFAULT '',
    terms_version VARCHAR(30) NOT NULL DEFAULT '',
    terms_accepted_at DATETIME(6) NULL,
    terms_required BOOLEAN NOT NULL DEFAULT FALSE,
    registered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role_status (role, status),
    CONSTRAINT chk_users_verified CHECK (email_verified IN (0, 1))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(100) NOT NULL,
    subcategory VARCHAR(100) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image_path VARCHAR(1024) NOT NULL DEFAULT '',
    size VARCHAR(20) NOT NULL DEFAULT 'M',
    PRIMARY KEY (id),
    KEY idx_products_category (category, subcategory),
    KEY idx_products_stock (stock),
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_stock CHECK (stock >= 0)
) ENGINE=InnoDB;

-- An empty cart has no rows. There is one row per customer/product combination.
CREATE TABLE IF NOT EXISTS cart_items (
    customer_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (customer_id, product_id),
    KEY idx_cart_product (product_id),
    CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_cart_quantity CHECK (quantity > 0)
) ENGINE=InnoDB;

-- Checkout contact, address and payment display are snapshots, not live profile fields.
-- payment_method holds existing values such as "Card ending 1234" or "GCash ref. ...".
-- Do not store full card numbers or CVVs.
CREATE TABLE IF NOT EXISTS orders (
    id INT NOT NULL AUTO_INCREMENT,
    queue_number INT NOT NULL,
    control_number VARCHAR(20) NULL,
    courier_reference VARCHAR(100) NULL,
    courier_confirmed_at DATETIME(6) NULL,
    delivered_by INT NULL,
    received_by INT NULL,
    delivery_issue VARCHAR(1000) NOT NULL DEFAULT '',
    delivery_issue_status VARCHAR(30) NOT NULL DEFAULT '',
    delivery_issue_response TEXT NULL,
    delivery_issue_reviewer INT NULL,
    customer_id INT NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    checkout_full_name VARCHAR(200) NOT NULL,
    checkout_email VARCHAR(254) NOT NULL DEFAULT '',
    checkout_contact_number VARCHAR(30) NOT NULL,
    fulfillment_method VARCHAR(30) NOT NULL,
    delivery_address TEXT NOT NULL,
    address_province VARCHAR(120) NULL,
    address_city VARCHAR(120) NULL,
    address_barangay VARCHAR(160) NULL,
    address_street VARCHAR(100) NULL,
    payment_method VARCHAR(255) NOT NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_reference VARCHAR(100) NULL,
    UNIQUE KEY uq_orders_payment_reference (payment_reference),
    notes TEXT NOT NULL,
    status ENUM('CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP', 'TRANSFERRING', 'AT_DELIVERY_POINT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'LOST')
        NOT NULL DEFAULT 'CONFIRMED',
    placed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_orders_queue (queue_number),
    KEY idx_orders_customer_date (customer_id, placed_at),
    KEY idx_orders_status_queue (status, queue_number),
    KEY idx_orders_completed (status, completed_at),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_orders_queue CHECK (queue_number > 0),
    CONSTRAINT chk_orders_completion CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL AND completed_at >= placed_at)
        OR (status <> 'COMPLETED' AND completed_at IS NULL)
    )
) ENGINE=InnoDB;

-- Preserve the purchased product information for order history and printed reports.
-- Live catalog edits must not rewrite these snapshot columns.
CREATE TABLE IF NOT EXISTS order_items (
    id INT NOT NULL AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_category VARCHAR(100) NOT NULL,
    product_subcategory VARCHAR(100) NOT NULL,
    product_image_path VARCHAR(1024) NOT NULL DEFAULT '',
    product_size VARCHAR(20) NOT NULL DEFAULT 'M',
    product_stock_at_purchase INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_order_product (order_id, product_id),
    KEY idx_order_items_product (product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_price CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_stock CHECK (product_stock_at_purchase >= 0)
) ENGINE=InnoDB;

-- Retain historical username/role even when an account's username changes.
-- Only explicit meaningful staff/admin actions are written here by the app.
CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NULL,
    username VARCHAR(100) NOT NULL,
    role ENUM('STAFF', 'ADMIN') NOT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT NOT NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_activity_date (occurred_at, id),
    KEY idx_activity_user_date (user_id, occurred_at),
    KEY idx_activity_role_action (role, action),
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- Supports persistence of the currently in-memory email/reset challenges.
-- Integration must store token/code digests, enforce expiry, invalidate older codes,
-- check the five-attempt limit and one-minute resend cooldown, and consume once.
CREATE TABLE IF NOT EXISTS verification_challenges (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    purpose ENUM('EMAIL_VERIFICATION', 'PASSWORD_RESET') NOT NULL,
    token_hash BINARY(32) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    attempts TINYINT NOT NULL DEFAULT 0,
    requested_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NOT NULL,
    verified_at DATETIME(6) NULL,
    consumed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_verification_token (token_hash),
    KEY idx_verification_user_purpose (user_id, purpose, requested_at),
    KEY idx_verification_expiry (expires_at),
    CONSTRAINT fk_verification_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_verification_attempts CHECK (attempts BETWEEN 0 AND 5),
    CONSTRAINT chk_verification_expiry CHECK (expires_at > requested_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS return_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    customer_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    reason TEXT NOT NULL,
    proof LONGBLOB NOT NULL,
    proof_name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    requested_at DATETIME(6) NOT NULL,
    reviewed_at DATETIME(6) NULL,
    reviewer_id INT NULL,
    resolution TEXT NOT NULL,
    refund_reference VARCHAR(100) NOT NULL DEFAULT '',
    refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    FOREIGN KEY(order_id) REFERENCES orders(id),
    FOREIGN KEY(customer_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS security_incidents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    username VARCHAR(100) NOT NULL,
    reason TEXT NOT NULL,
    reported_at DATETIME(6) NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolution TEXT NOT NULL,
    reviewer_id INT NULL,
    warning_sent BOOLEAN NOT NULL DEFAULT FALSE,
    warning_read BOOLEAN NOT NULL DEFAULT FALSE,
    warning_sent_at DATETIME(6) NULL,
    warning_message TEXT NULL,
    warning_popup_seen BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

SHOW TABLES;
