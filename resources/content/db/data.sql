--liquibase formatted sql

--changeset darren:sql1

INSERT INTO account(id, uuid, kind, name, state) VALUES(1, 'admin', 'admin', 'admin', 'active');
INSERT INTO account(id, uuid, kind, name, state) VALUES(2, 'system', 'system', 'system', 'inactive');
INSERT INTO zone(id, uuid, name, state) VALUES(1, UUID(), 'zone1', 'active');
