DROP DATABASE cattle;
CREATE DATABASE cattle COLLATE = 'utf8_general_ci' CHARACTER SET = 'utf8';

CREATE USER 'cattle'@'%' IDENTIFIED BY 'cattle';
GRANT ALL ON cattle.* TO 'cattle'@'%';
