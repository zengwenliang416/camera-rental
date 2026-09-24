-- Separate device count from channel pricing units; preserve explicit and fulfilled quantities.
SET @quantity_source_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='rental_order_item' AND column_name='quantity_source');
SET @quantity_source_ddl = IF(@quantity_source_exists=0,
  'ALTER TABLE rental_order_item ADD COLUMN quantity_source varchar(16) NOT NULL DEFAULT ''LEGACY'' COMMENT ''Device quantity provenance'' AFTER quantity',
  'SELECT 1');
PREPARE quantity_source_stmt FROM @quantity_source_ddl;
EXECUTE quantity_source_stmt;
DEALLOCATE PREPARE quantity_source_stmt;
CREATE TABLE IF NOT EXISTS rental_device_quantity_correction (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id bigint NOT NULL, rental_order_id bigint NOT NULL, rental_order_item_id bigint NOT NULL,
 old_quantity int NOT NULL, new_quantity int NOT NULL, channel_goods_quantity int NOT NULL,
 reason varchar(64) NOT NULL, create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_quantity_correction (tenant_id,rental_order_item_id,reason)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Only untouched, system-created, single-line, unassigned and unshipped pricing copies qualify.
-- Lock channel -> order -> item as existing quantity and fulfillment services do.
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SELECT xo.id, ro.id, i.id
FROM xianyu_order xo JOIN rental_order ro ON ro.channel_order_id=xo.id AND ro.tenant_id=xo.tenant_id
JOIN rental_order_item i ON i.rental_order_id=ro.id AND i.tenant_id=ro.tenant_id
WHERE xo.deleted=0 AND ro.deleted=0 AND i.deleted=0
 AND ro.source_type='XIANYU' AND ro.status='PENDING_ALLOCATION' AND ro.settled_at IS NULL
 AND xo.rental_order_id=ro.id AND xo.order_status='12' AND xo.consign_time IS NULL
 AND xo.cancel_time IS NULL AND COALESCE(xo.refund_status,0)<>5 AND xo.pay_amount>=0
 AND i.quantity_source='LEGACY' AND i.quantity>1 AND i.quantity=xo.goods_quantity
 AND i.creator='system' AND i.updater='system' AND i.create_time=i.update_time
 AND (SELECT COUNT(*) FROM rental_order_item other_item WHERE other_item.rental_order_id=ro.id AND other_item.tenant_id=ro.tenant_id AND other_item.deleted=0)=1
 AND NOT EXISTS(SELECT 1 FROM rental_device_assignment a WHERE a.rental_order_id=ro.id AND a.tenant_id=ro.tenant_id AND a.deleted=0)
 AND NOT EXISTS(SELECT 1 FROM rental_device_shipment s WHERE s.channel_order_id=xo.id AND s.tenant_id=xo.tenant_id AND s.deleted=0)
ORDER BY xo.id,ro.id,i.id FOR UPDATE;

INSERT INTO rental_device_quantity_correction
(tenant_id,rental_order_id,rental_order_item_id,old_quantity,new_quantity,channel_goods_quantity,reason)
SELECT i.tenant_id,ro.id,i.id,i.quantity,1,xo.goods_quantity,'UNTOUCHED_CHANNEL_PRICING_COPY'
FROM xianyu_order xo JOIN rental_order ro ON ro.channel_order_id=xo.id AND ro.tenant_id=xo.tenant_id
JOIN rental_order_item i ON i.rental_order_id=ro.id AND i.tenant_id=ro.tenant_id
WHERE xo.deleted=0 AND ro.deleted=0 AND i.deleted=0
 AND ro.source_type='XIANYU' AND ro.status='PENDING_ALLOCATION' AND ro.settled_at IS NULL
 AND xo.rental_order_id=ro.id AND xo.order_status='12' AND xo.consign_time IS NULL
 AND xo.cancel_time IS NULL AND COALESCE(xo.refund_status,0)<>5 AND xo.pay_amount>=0
 AND i.quantity_source='LEGACY' AND i.quantity>1 AND i.quantity=xo.goods_quantity
 AND i.creator='system' AND i.updater='system' AND i.create_time=i.update_time
 AND (SELECT COUNT(*) FROM rental_order_item other_item WHERE other_item.rental_order_id=ro.id AND other_item.tenant_id=ro.tenant_id AND other_item.deleted=0)=1
 AND NOT EXISTS(SELECT 1 FROM rental_device_assignment a WHERE a.rental_order_id=ro.id AND a.tenant_id=ro.tenant_id AND a.deleted=0)
 AND NOT EXISTS(SELECT 1 FROM rental_device_shipment s WHERE s.channel_order_id=xo.id AND s.tenant_id=xo.tenant_id AND s.deleted=0)
ON DUPLICATE KEY UPDATE reason=rental_device_quantity_correction.reason;
UPDATE rental_order_item i
JOIN (SELECT rental_order_id,tenant_id FROM rental_order_item WHERE deleted=0
      GROUP BY rental_order_id,tenant_id HAVING COUNT(*)=1) single_item
 ON single_item.rental_order_id=i.rental_order_id AND single_item.tenant_id=i.tenant_id
JOIN rental_order ro ON ro.id=i.rental_order_id AND ro.tenant_id=i.tenant_id
JOIN xianyu_order xo ON xo.id=ro.channel_order_id AND xo.tenant_id=ro.tenant_id
JOIN rental_device_quantity_correction c ON c.rental_order_item_id=i.id AND c.tenant_id=i.tenant_id
 AND c.reason='UNTOUCHED_CHANNEL_PRICING_COPY' AND c.old_quantity=i.quantity
SET i.quantity=1,i.quantity_source='DEFAULT',i.update_time=NOW()
WHERE xo.deleted=0 AND ro.deleted=0 AND i.deleted=0
 AND ro.source_type='XIANYU' AND ro.status='PENDING_ALLOCATION' AND ro.settled_at IS NULL
 AND xo.rental_order_id=ro.id AND xo.order_status='12' AND xo.consign_time IS NULL
 AND xo.cancel_time IS NULL AND COALESCE(xo.refund_status,0)<>5 AND xo.pay_amount>=0
 AND i.quantity_source='LEGACY' AND i.quantity>1 AND i.quantity=xo.goods_quantity
 AND i.creator='system' AND i.updater='system' AND i.create_time=i.update_time
 AND NOT EXISTS(SELECT 1 FROM rental_device_assignment a WHERE a.rental_order_id=ro.id AND a.tenant_id=ro.tenant_id AND a.deleted=0)
 AND NOT EXISTS(SELECT 1 FROM rental_device_shipment s WHERE s.channel_order_id=xo.id AND s.tenant_id=xo.tenant_id AND s.deleted=0);
COMMIT;
