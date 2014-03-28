CREATE DATABASE cattle COLLATE = 'utf8_general_ci' CHARACTER SET = 'utf8';
CREATE USER 'cattle'@'%' IDENTIFIED BY 'cattle';
GRANT SELECT, UPDATE, INSERT, DELETE ON cattle.* TO 'cattle'@'%';
