DROP TABLE IF EXISTS rental_device_assignment;
DROP TABLE IF EXISTS xianyu_order;

CREATE TABLE xianyu_order (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    xianyu_item_id VARCHAR(64),
    xgj_product_id VARCHAR(64),
    xgj_sku_id VARCHAR(64),
    rental_order_id BIGINT,
    conversion_status VARCHAR(32),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE rental_device_assignment (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rental_order_id BIGINT NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
