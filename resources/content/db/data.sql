-- liquibase formatted sql

-- changeset darren:sql1

INSERT INTO account(id, uuid, kind, name, state) VALUES(1, 'admin', 'admin', 'admin', 'active');
INSERT INTO account(id, uuid, kind, name, state) VALUES(2, 'system', 'system', 'system', 'inactive');
INSERT INTO account(id, uuid, kind, name, state) VALUES(3, 'superadmin', 'superadmin', 'superadmin', 'inactive');
INSERT INTO zone(id, uuid, kind, name, state) VALUES(1, 'zone1', 'zone', 'zone1', 'active');
