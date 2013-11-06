CREATE DATABASE dstack COLLATE = 'utf8_general_ci' CHARACTER SET = 'utf8';
CREATE USER 'dstack'@'%' IDENTIFIED BY 'dstack';
GRANT SELECT, UPDATE, INSERT, DELETE ON dstack.* TO 'dstack'@'%';
