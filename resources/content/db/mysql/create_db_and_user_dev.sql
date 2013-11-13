CREATE DATABASE dstack COLLATE = 'utf8_general_ci' CHARACTER SET = 'utf8';
CREATE USER 'dstack'@'%' IDENTIFIED BY 'dstack';
CREATE USER 'dstack'@'localhost' IDENTIFIED BY 'dstack';
GRANT ALL ON dstack.* TO 'dstack'@'%';
GRANT ALL ON dstack.* TO 'dstack'@'localhost';
