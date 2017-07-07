-- liquibase formatted sql

-- changeset darren:sql047

DELETE FROM config_item_status WHERE service_id IS NOT NULL;
DELETE FROM config_item_status WHERE agent_id IS NULL;
UPDATE config_item_status SET resource_id = agent_id, resource_type = 'agent_id' WHERE agent_id IS NOT NULL;
