DROP TABLE IF EXISTS rental_historical_reconciliation_failure;
DROP TABLE IF EXISTS rental_historical_reconciliation_run;
DROP TABLE IF EXISTS xianyu_order;

CREATE TABLE xianyu_order (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rental_order_id BIGINT,
    conversion_status VARCHAR(32),
    preparation_status VARCHAR(64),
    updater VARCHAR(64) NOT NULL DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE rental_historical_reconciliation_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    dry_run BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'READY',
    start_after_id BIGINT NOT NULL DEFAULT 0,
    end_id_inclusive BIGINT NOT NULL,
    cursor_after_id BIGINT NOT NULL DEFAULT 0,
    batch_size INT NOT NULL,
    resume_count INT NOT NULL DEFAULT 0,
    scanned_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    created_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    unchanged_count INT NOT NULL DEFAULT 0,
    conflict_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    review_required_count INT NOT NULL DEFAULT 0,
    last_failed_order_id BIGINT,
    last_error_code VARCHAR(128),
    execution_token VARCHAR(64),
    lease_until TIMESTAMP,
    heartbeat_at TIMESTAMP,
    started_at TIMESTAMP,
    paused_at TIMESTAMP,
    finished_at TIMESTAMP,
    creator VARCHAR(64) NOT NULL DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) NOT NULL DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE rental_historical_reconciliation_failure (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    run_id BIGINT NOT NULL,
    channel_order_id BIGINT NOT NULL,
    cursor_before_id BIGINT NOT NULL DEFAULT 0,
    attempt_no INT NOT NULL,
    error_code VARCHAR(128) NOT NULL,
    creator VARCHAR(64) NOT NULL DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) NOT NULL DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
