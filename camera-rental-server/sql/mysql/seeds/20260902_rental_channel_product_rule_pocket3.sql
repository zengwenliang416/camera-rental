-- 2026-09-02 渠道商品规则：大疆 Pocket3 商品精确映射 P3 型号（小疆同学店铺）
-- 背景：闲鱼商品 890661155567（大疆Pocket3数码设备租赁）此前无 CREATE_RENTAL 规则，
--       订单 2925 备货状态停留在 WAITING_MODEL / PRODUCT_RULE_NOT_CONFIGURED，发货被阻塞。
-- 已于 2026-09-02 直接在生产执行，备份见 /opt/camera-rental/backups/rule-p3-20260902/。
INSERT INTO rental_channel_product_rule
    (tenant_id, shop_id, xianyu_item_id, xgj_product_id, product_title_snapshot,
     handling_policy, mapping_mode, single_device_model_id, enabled, rule_note,
     lock_version, creator, updater)
VALUES
    (1, 3, '890661155567', '1313066259729029', '大疆Pocket3数码设备租赁',
     'CREATE_RENTAL', 'SINGLE', 19, b'1',
     'Pocket3 商品精确映射 P3 型号，修复订单 2925 发货阻塞',
     0, 'system', 'system');
