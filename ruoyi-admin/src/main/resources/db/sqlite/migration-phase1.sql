-- Existing SQLite databases created before Phase 1 require these columns.
ALTER TABLE sys_role ADD COLUMN data_scope CHAR(1) DEFAULT '1';
ALTER TABLE oa_incoming_doc ADD COLUMN template_id BIGINT DEFAULT NULL;
ALTER TABLE oa_outgoing_doc ADD COLUMN template_id BIGINT DEFAULT NULL;