-- Defense accounts matching the existing QUEUETEES login defaults.
-- Re-running this file keeps existing accounts and their current passwords unchanged.
USE queuetees;
START TRANSACTION;
INSERT INTO users (username,email,password_hash,role,email_verified,status,full_name,address,contact_number,gender,profile_picture)
VALUES ('admin','admin@queuetees.local','120000:mS8MeklCXoUYyuS/RkELRQ==:IU80Ow7IMnMnsBiPGT1DQHI7AJ6ktYgBoxsPIca3zP0=','ADMIN',TRUE,'ACTIVE','','','','','')
ON DUPLICATE KEY UPDATE id=id;
INSERT INTO users (username,email,password_hash,role,email_verified,status,full_name,address,contact_number,gender,profile_picture)
VALUES ('staff','staff@queuetees.local','120000:uHom/vdGuqrAkIiDyKjMPw==:7C1i5tE4EPNhXs0CcE1j0P60QxCxTzk4ffUapihsxpI=','STAFF',TRUE,'ACTIVE','','','','','')
ON DUPLICATE KEY UPDATE id=id;
INSERT INTO users (username,email,password_hash,role,email_verified,status,full_name,address,contact_number,gender,profile_picture)
VALUES ('verifiedcustomer','verified@queuetees.local','120000:zFeyv/AcrhMCrxi3MeIIhQ==:CejMt8xRi3U4/fU9mEaP+J6UJx1x8T/f+66RIo4xqvQ=','CUSTOMER',TRUE,'ACTIVE','','','','','')
ON DUPLICATE KEY UPDATE id=id;
COMMIT;
SELECT id,username,email,role,email_verified,status FROM users WHERE username IN ('admin','staff','verifiedcustomer');
