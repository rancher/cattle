CREATE DATABASE IF NOT EXISTS cattle COLLATE = 'utf8_general_ci' CHARACTER SET = 'utf8';
CREATE DATABASE IF NOT EXISTS cattle_base COLLATE = 'utf8_general_ci' CHARACTER SET = 'utf8';
GRANT ALL ON cattle.* TO 'cattle'@'%' IDENTIFIED BY 'cattle';
GRANT ALL ON cattle.* TO 'cattle'@'localhost' IDENTIFIED BY 'cattle';
GRANT ALL ON cattle_base.* TO 'cattle'@'%' IDENTIFIED BY 'cattle';
GRANT ALL ON cattle_base.* TO 'cattle'@'localhost' IDENTIFIED BY 'cattle';