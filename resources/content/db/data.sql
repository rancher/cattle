--liquibase formatted sql

--changeset darren:sql1

INSERT INTO ACCOUNT(id, uuid, kind, name, state) VALUES(1, UUID(), 'admin', 'admin', 'active');
INSERT INTO ZONE(id, uuid, name, state) VALUES(1, UUID(), 'zone1', 'active');