-- SpecNav executable audit entry.
-- Run from the repository root with the mysql client so the production migration
-- remains the single deployment source of truth.
-- The sourced production migration contains the required CREATE, ALTER, and
-- INSERT statements; this wrapper intentionally does not duplicate them.
SOURCE camera-rental-server/sql/mysql/migrations/20260731_032_rental_delivery_tracking.sql;
