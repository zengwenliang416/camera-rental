-- Ensure V1 XianGuanJia read-only product and after-sale handlers are visible in Yudao infra jobs.

INSERT INTO `infra_job` (
  `name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
  `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `updater`
)
SELECT '闲管家商品增量同步', 1, 'xianyuProductSyncJob', '', '0 0/10 * * * ?', 1, 5000, 0, 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1
  FROM `infra_job`
  WHERE `handler_name` = 'xianyuProductSyncJob'
    AND `deleted` = b'0'
);

INSERT INTO `infra_job` (
  `name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
  `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `updater`
)
SELECT '闲管家售后增量同步', 1, 'xianyuAfterSaleSyncJob', '', '0 0/10 * * * ?', 1, 5000, 0, 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1
  FROM `infra_job`
  WHERE `handler_name` = 'xianyuAfterSaleSyncJob'
    AND `deleted` = b'0'
);
