CREATE DATABASE cattle COLLATE = 'utf8_general_ci' CHARACTER SET = 'utf8';
CREATE DATABASE cattle_base COLLATE = 'utf8_general_ci' CHARACTER SET = 'utf8';
CREATE USER 'cattle'@'%' IDENTIFIED BY 'cattle';
CREATE USER 'cattle'@'localhost' IDENTIFIED BY 'cattle';
GRANT ALL ON cattle.* TO 'cattle'@'%';
GRANT ALL ON cattle.* TO 'cattle'@'localhost';
GRANT ALL ON cattle_base.* TO 'cattle'@'%';
GRANT ALL ON cattle_base.* TO 'cattle'@'localhost';
