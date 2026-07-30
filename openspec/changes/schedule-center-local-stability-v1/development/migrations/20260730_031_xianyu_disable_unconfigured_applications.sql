-- Normalize legacy application rows to the admin-managed configuration rules.
-- An integration without both credentials cannot remain enabled.

UPDATE `xianyu_application`
SET `enabled` = b'0',
    `write_enabled` = b'0',
    `job_enabled` = b'0',
    `updater` = 'migration-031',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (
    `app_key` IS NULL
    OR TRIM(`app_key`) = ''
    OR `app_secret` IS NULL
    OR TRIM(`app_secret`) = ''
  );
