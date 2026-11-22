-- Ordres de provisioning (anciennement jobs) et temporalite des comptes :
-- order_id = ordre de creation, end_order_id = ordre de suppression planifie a la date de fin.
ALTER TABLE account RENAME COLUMN job_id TO order_id;
ALTER TABLE account ADD COLUMN start_date DATE;
ALTER TABLE account ADD COLUMN end_date DATE;
ALTER TABLE account ADD COLUMN end_order_id UUID;
