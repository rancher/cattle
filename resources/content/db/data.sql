-- liquibase formatted sql

-- changeset darren:sql1

INSERT INTO account(uuid, kind, name, state) VALUES('admin', 'admin', 'admin', 'active');
INSERT INTO account(uuid, kind, name, state) VALUES('system', 'system', 'system', 'inactive');
INSERT INTO account(uuid, kind, name, state) VALUES('superadmin', 'superadmin', 'superadmin', 'inactive');
INSERT INTO zone(id, uuid, kind, name, state) VALUES(1, 'zone1', 'zone', 'zone1', 'active');
