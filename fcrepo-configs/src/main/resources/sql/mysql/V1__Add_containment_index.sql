-- Add a unique key of the fedora_id and transaction so assist in UPSERT.
ALTER TABLE containment_transactions ADD CONSTRAINT UNIQUE INDEX (fedora_id, transaction_id);