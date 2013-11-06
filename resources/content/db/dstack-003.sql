--liquibase formatted sql

--changeset darren:sql3
INSERT INTO `cloud`.`disk_offering` (id, name, uuid, display_text, created, use_local_storage, type, disk_size) VALUES (17, 'tinyOffering', UUID(), 'tinyOffering', NOW(), 1, 'Service', 0);
INSERT INTO `cloud`.`service_offering` (id, cpu, speed, ram_size) VALUES (17, 1, 100, 100);
INSERT INTO `cloud`.`disk_offering` (id, name, uuid, display_text, created, type, disk_size) VALUES (18, 'tinyDiskOffering', UUID(), 'tinyDiskOffering', NOW(), 'Disk', 1073741824);
INSERT INTO `cloud`.`configuration` (instance, name,value) VALUE('DEFAULT','router.ram.size', '100');
INSERT INTO `cloud`.`configuration` (instance, name,value) VALUE('DEFAULT','router.cpu.mhz','100');
INSERT INTO `cloud`.`configuration` (instance, name,value) VALUE('DEFAULT','console.ram.size','100');
INSERT INTO `cloud`.`configuration` (instance, name,value) VALUE('DEFAULT','console.cpu.mhz', '100');
INSERT INTO `cloud`.`configuration` (instance, name,value) VALUE('DEFAULT','ssvm.ram.size','100');
INSERT INTO `cloud`.`configuration` (instance, name,value) VALUE('DEFAULT','ssvm.cpu.mhz','100');
INSERT INTO `cloud`.`configuration` (instance, name, value) VALUE('DEFAULT', 'system.vm.use.local.storage', 'true');
INSERT INTO `cloud`.`configuration` (instance, name, value) VALUE('DEFAULT', 'expunge.workers', '3');
INSERT INTO `cloud`.`configuration` (instance, name, value) VALUE('DEFAULT', 'expunge.delay', '60');
INSERT INTO `cloud`.`configuration` (instance, name, value) VALUE('DEFAULT', 'expunge.interval', '60');
INSERT INTO `cloud`.`configuration` (instance, name, value) VALUE('DEFAULT', 'enable.ec2.api', 'true');
INSERT INTO `cloud`.`configuration` (instance, name, value) VALUE('DEFAULT', 'enable.s3.api', 'true');
-- INSERT INTO `cloud`.`configuration` (instance, name, value) VALUE('DEFAULT', 'host', '192.168.56.1');
-- INSERT INTO `cloud`.`configuration` (instance, name, value) VALUE('DEFAULT', 'management.network.cidr', '192.168.56.0/24');
-- INSERT INTO `cloud`.`configuration` (instance, name, value) VALUE('DEFAULT', 'secstorage.allowed.internal.sites', '192.168.56.0/8');
UPDATE `cloud`.`configuration` SET value='10' where name = 'storage.overprovisioning.factor';
UPDATE `cloud`.`configuration` SET value='10' where name = 'cpu.overprovisioning.factor';
UPDATE `cloud`.`configuration` SET value='10' where name = 'mem.overprovisioning.factor';
UPDATE `cloud`.`vm_template` SET unique_name="tiny Linux",name="tiny Linux",url="http://people.apache.org/~bhaisaab/vms/ttylinux_pv.vhd",checksum="046e134e642e6d344b34648223ba4bc1",display_text="tiny Linux" where id=5;
