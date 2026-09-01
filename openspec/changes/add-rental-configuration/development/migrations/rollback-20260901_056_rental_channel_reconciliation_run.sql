-- Destructive rollback for disposable or explicitly approved pre-production schemas only.
DROP TABLE IF EXISTS `rental_channel_reconciliation_run`;
