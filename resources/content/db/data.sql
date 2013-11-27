--liquibase formatted sql

--changeset darren:sql1

INSERT INTO account(id, uuid, kind, name, state) VALUES(1, UUID(), 'admin', 'admin', 'active');
INSERT INTO zone(id, uuid, name, state) VALUES(1, UUID(), 'zone1', 'active');