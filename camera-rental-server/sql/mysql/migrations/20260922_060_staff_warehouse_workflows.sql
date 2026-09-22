SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS rental_staff_issue (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id bigint NOT NULL,
 request_key varchar(128) NULL, rental_order_id bigint NULL, device_id bigint NULL, title varchar(100) NOT NULL,
 note varchar(1000) NOT NULL DEFAULT '', status varchar(16) NOT NULL DEFAULT 'OPEN',
 owner_id bigint NULL, revision int NOT NULL DEFAULT 0,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', KEY idx_staff_issue_queue(tenant_id,status,id), UNIQUE KEY uk_staff_issue_request(tenant_id,request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rental_staff_stocktake (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id bigint NOT NULL, warehouse_code varchar(64) NOT NULL,
 status varchar(16) NOT NULL DEFAULT 'OPEN', idempotency_key varchar(100) NOT NULL,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', UNIQUE KEY uk_staff_stocktake_request(tenant_id,idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rental_staff_stocktake_line (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id bigint NOT NULL, stocktake_id bigint NOT NULL,
 device_id bigint NOT NULL, device_no varchar(64) NOT NULL, original_warehouse varchar(64) DEFAULT NULL,
 expected bit(1) NOT NULL DEFAULT b'1', scanned bit(1) NOT NULL DEFAULT b'0', adjusted bit(1) NOT NULL DEFAULT b'0',
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', UNIQUE KEY uk_staff_stocktake_device(tenant_id,stocktake_id,device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rental_staff_photo (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id bigint NOT NULL, device_id bigint NOT NULL,
 assignment_id bigint NOT NULL, file_config_id bigint NOT NULL, object_path varchar(512) NOT NULL,
 file_id bigint NULL, confirmed bit(1) NOT NULL DEFAULT b'0',
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', KEY idx_staff_photo_assignment(tenant_id,assignment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rental_staff_inspection (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id bigint NOT NULL, device_id bigint NOT NULL,
 assignment_id bigint NOT NULL, idempotency_key varchar(100) NOT NULL,
 checklist_json text NOT NULL, photo_ids_json text NOT NULL, passed bit(1) NOT NULL, note varchar(512) DEFAULT NULL,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', UNIQUE KEY uk_staff_inspection_request(tenant_id,idempotency_key),
 KEY idx_staff_inspection_device(tenant_id,device_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rental_staff_inspection_template (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id bigint NOT NULL, model_code varchar(64) NOT NULL,
 checklist_json text NOT NULL,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', UNIQUE KEY uk_staff_inspection_model(tenant_id,model_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rental_shipment_attempt (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id bigint NOT NULL, channel_order_id bigint NOT NULL,
 idempotency_key varchar(100) NOT NULL, request_hash varchar(64) NOT NULL, status varchar(24) NOT NULL,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', UNIQUE KEY uk_shipment_attempt_order(tenant_id,channel_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
