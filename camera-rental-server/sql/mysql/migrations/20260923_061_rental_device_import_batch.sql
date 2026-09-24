-- Atomic import receipts: tenant/user isolation, locked retries, no QR payload storage.
CREATE TABLE IF NOT EXISTS rental_device_import_batch (
  id varchar(36) NOT NULL,
  tenant_id bigint NOT NULL,
  user_id bigint NOT NULL,
  request_json mediumtext NOT NULL,
  preview_json mediumtext NOT NULL,
  result_json text NULL,
  expires_at datetime NOT NULL,
  creator varchar(64) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  KEY idx_device_import_owner (tenant_id, user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
