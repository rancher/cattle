CREATE DATABASE cattle COLLATE = 'utf8_general_ci' CHARACTER SET = 'utf8';
CREATE USER 'cattle'@'%' IDENTIFIED BY 'cattle';
CREATE USER 'cattle'@'localhost' IDENTIFIED BY 'cattle';
GRANT ALL ON cattle.* TO 'cattle'@'%';
GRANT ALL ON cattle.* TO 'cattle'@'localhost';
