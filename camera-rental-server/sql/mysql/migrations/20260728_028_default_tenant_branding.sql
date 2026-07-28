-- Align the default tenant and administrator display name with the rental brand.
-- The predicates keep this migration idempotent and avoid touching custom tenants.

UPDATE `system_tenant`
SET `name` = '捷租达',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 1
  AND `name` = '芋道源码'
  AND `deleted` = b'0';

UPDATE `system_users`
SET `nickname` = '捷租达',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 1
  AND `tenant_id` = 1
  AND `username` = 'admin'
  AND `nickname` = '芋道源码'
  AND `deleted` = b'0';
