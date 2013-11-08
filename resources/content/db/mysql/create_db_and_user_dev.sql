CREATE DATABASE cloud COLLATE = 'utf8_general_ci' CHARACTER SET = 'utf8';
CREATE USER 'dstack'@'%' IDENTIFIED BY 'dstack';
CREATE USER 'dstack'@'localhost' IDENTIFIED BY 'dstack';
GRANT ALL ON cloud.* TO 'dstack'@'%';
GRANT ALL ON cloud.* TO 'dstack'@'localhost';
