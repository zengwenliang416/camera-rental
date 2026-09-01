-- Disposable or pre-production rollback for migration 055 only.
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `rental_historical_reconciliation_failure`;
DROP TABLE IF EXISTS `rental_historical_reconciliation_run`;

