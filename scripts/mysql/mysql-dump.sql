-- MySQL dump 10.13  Distrib 5.7.18, for Linux (x86_64)
--
-- Host: localhost    Database: cattle
-- ------------------------------------------------------
-- Server version	5.7.18-0ubuntu0.16.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `DATABASECHANGELOG`
--

DROP TABLE IF EXISTS `DATABASECHANGELOG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DATABASECHANGELOG` (
  `ID` varchar(255) NOT NULL,
  `AUTHOR` varchar(255) NOT NULL,
  `FILENAME` varchar(255) NOT NULL,
  `DATEEXECUTED` datetime NOT NULL,
  `ORDEREXECUTED` int(11) NOT NULL,
  `EXECTYPE` varchar(10) NOT NULL,
  `MD5SUM` varchar(35) DEFAULT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `COMMENTS` varchar(255) DEFAULT NULL,
  `TAG` varchar(255) DEFAULT NULL,
  `LIQUIBASE` varchar(20) DEFAULT NULL,
  `CONTEXTS` varchar(255) DEFAULT NULL,
  `LABELS` varchar(255) DEFAULT NULL,
  `DEPLOYMENT_ID` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DATABASECHANGELOG`
--

LOCK TABLES `DATABASECHANGELOG` WRITE;
/*!40000 ALTER TABLE `DATABASECHANGELOG` DISABLE KEYS */;
INSERT INTO `DATABASECHANGELOG` VALUES ('dump1','rancher (generated)','db/core-200.xml','2017-08-14 10:59:45',1,'EXECUTED','7:c3b5dc72a4a405e7a3601317f5661e18','createTable tableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump2','rancher (generated)','db/core-200.xml','2017-08-14 10:59:45',2,'EXECUTED','7:bc321510d2cf7512925e8cc6cdbd0a4f','createTable tableName=agent','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump3','rancher (generated)','db/core-200.xml','2017-08-14 10:59:45',3,'EXECUTED','7:cda28103f639b83f49d9975917f13a4b','createTable tableName=audit_log','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump4','rancher (generated)','db/core-200.xml','2017-08-14 10:59:45',4,'EXECUTED','7:16124743c538017e595c02fa03716948','createTable tableName=auth_token','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump5','rancher (generated)','db/core-200.xml','2017-08-14 10:59:45',5,'EXECUTED','7:ab4b0cf66be9ce7dc1888396a0535a32','createTable tableName=catalog','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump6','rancher (generated)','db/core-200.xml','2017-08-14 10:59:46',6,'EXECUTED','7:acb5880becfe044aef354a7479dd64fe','createTable tableName=catalog_category','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump7','rancher (generated)','db/core-200.xml','2017-08-14 10:59:46',7,'EXECUTED','7:c906de4395a972e70daff9c33b1acd96','createTable tableName=catalog_file','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump8','rancher (generated)','db/core-200.xml','2017-08-14 10:59:46',8,'EXECUTED','7:5658d5d73f44b9ddf094e15bc8805080','createTable tableName=catalog_label','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump9','rancher (generated)','db/core-200.xml','2017-08-14 10:59:46',9,'EXECUTED','7:5e2af0672088f4b1275d88bb918e24de','createTable tableName=catalog_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump10','rancher (generated)','db/core-200.xml','2017-08-14 10:59:46',10,'EXECUTED','7:ccd4eea4e1382a97c6cd324c9ab7dddc','createTable tableName=catalog_template_category','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump11','rancher (generated)','db/core-200.xml','2017-08-14 10:59:46',11,'EXECUTED','7:56fc8abb0bc807bad83b9193c36e1bc1','createTable tableName=catalog_version','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump12','rancher (generated)','db/core-200.xml','2017-08-14 10:59:46',12,'EXECUTED','7:b6055f5260b731e0a19d7f2bcde2717b','createTable tableName=catalog_version_label','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump13','rancher (generated)','db/core-200.xml','2017-08-14 10:59:46',13,'EXECUTED','7:4f5ee71604d252e6f8a5aed50bc80d4a','createTable tableName=certificate','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump14','rancher (generated)','db/core-200.xml','2017-08-14 10:59:46',14,'EXECUTED','7:364cfd40a943adf9332b1c2cb90ba891','createTable tableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump15','rancher (generated)','db/core-200.xml','2017-08-14 10:59:47',15,'EXECUTED','7:c91375ceddfe29ffe830dfe0033038b1','createTable tableName=credential','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump16','rancher (generated)','db/core-200.xml','2017-08-14 10:59:47',16,'EXECUTED','7:e6d928e1ed4b5e50f349cc29e8e9aaa0','createTable tableName=data','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump17','rancher (generated)','db/core-200.xml','2017-08-14 10:59:47',17,'EXECUTED','7:a7cc5c7ff2ba37be34d99b3242891150','createTable tableName=deployment_unit','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump18','rancher (generated)','db/core-200.xml','2017-08-14 10:59:47',18,'EXECUTED','7:03251532046197286d6bcd6e8dafbde4','createTable tableName=dynamic_schema','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump19','rancher (generated)','db/core-200.xml','2017-08-14 10:59:47',19,'EXECUTED','7:f596429c5907e1e5caf27aa908ec8a8f','createTable tableName=dynamic_schema_role','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump20','rancher (generated)','db/core-200.xml','2017-08-14 10:59:47',20,'EXECUTED','7:0e7ff60ce8a77cb1052fcf820b7b79bd','createTable tableName=external_event','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump21','rancher (generated)','db/core-200.xml','2017-08-14 10:59:47',21,'EXECUTED','7:4cc207f44c02a504796d57032927b1a2','createTable tableName=generic_object','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump22','rancher (generated)','db/core-200.xml','2017-08-14 10:59:47',22,'EXECUTED','7:dfda1a4c8fedd83f60d208b6e93a66d9','createTable tableName=ha_membership','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump23','rancher (generated)','db/core-200.xml','2017-08-14 10:59:47',23,'EXECUTED','7:bf6a840d6d20affbe75dcb3ac1c51040','createTable tableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump24','rancher (generated)','db/core-200.xml','2017-08-14 10:59:47',24,'EXECUTED','7:cc8c0f84aedd150492710ebc1300e77a','createTable tableName=host_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump25','rancher (generated)','db/core-200.xml','2017-08-14 10:59:48',25,'EXECUTED','7:19318f6ce9ec8d9d5ec034cd8b603fef','createTable tableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump26','rancher (generated)','db/core-200.xml','2017-08-14 10:59:48',26,'EXECUTED','7:284ad8708711bff66bafb7b3bc781235','createTable tableName=key_value','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump27','rancher (generated)','db/core-200.xml','2017-08-14 10:59:48',27,'EXECUTED','7:056cb003f6e0927037d57df1400d5116','createTable tableName=machine_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump28','rancher (generated)','db/core-200.xml','2017-08-14 10:59:48',28,'EXECUTED','7:748739ad5584abf0adf0e2bbb0577971','createTable tableName=mount','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump29','rancher (generated)','db/core-200.xml','2017-08-14 10:59:48',29,'EXECUTED','7:0129a1de2bf1303adc69f6b0fab78643','createTable tableName=network','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump30','rancher (generated)','db/core-200.xml','2017-08-14 10:59:48',30,'EXECUTED','7:e1f22d1f63be723ca06b6da6e2502243','createTable tableName=network_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump31','rancher (generated)','db/core-200.xml','2017-08-14 10:59:48',31,'EXECUTED','7:c8fae8860bdfd90e275a9d629569a7a9','createTable tableName=process_execution','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump32','rancher (generated)','db/core-200.xml','2017-08-14 10:59:48',32,'EXECUTED','7:ede42f4645c8e976ebfa3bd7dcd88fff','createTable tableName=process_instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump33','rancher (generated)','db/core-200.xml','2017-08-14 10:59:48',33,'EXECUTED','7:55857181a84cc108a9489c4ee0fa736e','createTable tableName=project_member','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump34','rancher (generated)','db/core-200.xml','2017-08-14 10:59:49',34,'EXECUTED','7:e6ff0fd4ab3b3e29483ef0f311eefb54','createTable tableName=resource_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump35','rancher (generated)','db/core-200.xml','2017-08-14 10:59:49',35,'EXECUTED','7:ebbc1765df2332c6e40f339b80ff2652','createTable tableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump36','rancher (generated)','db/core-200.xml','2017-08-14 10:59:49',36,'EXECUTED','7:ae8bf337ba3e1067e8dd029e46835c49','createTable tableName=scheduled_upgrade','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump37','rancher (generated)','db/core-200.xml','2017-08-14 10:59:49',37,'EXECUTED','7:e38458ce11b285b99fc39230f5946b71','createTable tableName=secret','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump38','rancher (generated)','db/core-200.xml','2017-08-14 10:59:49',38,'EXECUTED','7:f33d512f85bd028d2cb090e443de6fee','createTable tableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump39','rancher (generated)','db/core-200.xml','2017-08-14 10:59:49',39,'EXECUTED','7:9f8d18fc9f960c22e8806f710b4e7f30','createTable tableName=service_event','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump40','rancher (generated)','db/core-200.xml','2017-08-14 10:59:49',40,'EXECUTED','7:2baa5ab3652a58a7e9bbea3ad1618a2f','createTable tableName=service_log','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump41','rancher (generated)','db/core-200.xml','2017-08-14 10:59:49',41,'EXECUTED','7:ba19c934f2c70b18169c98ba677acb38','createTable tableName=setting','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump42','rancher (generated)','db/core-200.xml','2017-08-14 10:59:49',42,'EXECUTED','7:771f069a83311681f8f77c2fc23e734e','createTable tableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump43','rancher (generated)','db/core-200.xml','2017-08-14 10:59:50',43,'EXECUTED','7:1a0e28a5096e051803558afd08b5e941','createTable tableName=storage_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump44','rancher (generated)','db/core-200.xml','2017-08-14 10:59:50',44,'EXECUTED','7:326bb17afbb4fabbfe90fca9eae62a49','createTable tableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump45','rancher (generated)','db/core-200.xml','2017-08-14 10:59:50',45,'EXECUTED','7:052baabe93b6b7b545a6e5f52f4e5a09','createTable tableName=storage_pool_host_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump46','rancher (generated)','db/core-200.xml','2017-08-14 10:59:50',46,'EXECUTED','7:0b850be5d1a450dba8545289fdbf47a7','createTable tableName=subnet','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump47','rancher (generated)','db/core-200.xml','2017-08-14 10:59:50',47,'EXECUTED','7:11fd8574725d0064a31c8d1eb3af385b','createTable tableName=ui_challenge','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump48','rancher (generated)','db/core-200.xml','2017-08-14 10:59:50',48,'EXECUTED','7:b7af3a5d097f1dd5072bd3b7f6583005','createTable tableName=user_preference','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump49','rancher (generated)','db/core-200.xml','2017-08-14 10:59:50',49,'EXECUTED','7:70b53d7afa5583d32d1083b5a33ecd6f','createTable tableName=volume','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump50','rancher (generated)','db/core-200.xml','2017-08-14 10:59:50',50,'EXECUTED','7:a8aeec6dd88409160d1e8ef95ed45ffc','createTable tableName=volume_storage_pool_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump51','rancher (generated)','db/core-200.xml','2017-08-14 10:59:50',51,'EXECUTED','7:97138b6920276810e3beec725866e1b9','createTable tableName=volume_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump52','rancher (generated)','db/core-200.xml','2017-08-14 10:59:50',52,'EXECUTED','7:dc9ef476f62f1edb158dc2cbfc333ed6','addUniqueConstraint constraintName=idx_account_uuid, tableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump53','rancher (generated)','db/core-200.xml','2017-08-14 10:59:51',53,'EXECUTED','7:e4d169fc5f37644ddcf492cf377215ac','addUniqueConstraint constraintName=idx_agent_uuid, tableName=agent','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump54','rancher (generated)','db/core-200.xml','2017-08-14 10:59:51',54,'EXECUTED','7:1005e368c1b917f358be6c41a32f39e9','addUniqueConstraint constraintName=idx_auth_token_key, tableName=auth_token','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump55','rancher (generated)','db/core-200.xml','2017-08-14 10:59:51',55,'EXECUTED','7:a230bbcd3681a36ce02e9377ae5f7274','addUniqueConstraint constraintName=idx_cert_data_uuid, tableName=certificate','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump56','rancher (generated)','db/core-200.xml','2017-08-14 10:59:51',56,'EXECUTED','7:e039b026eb7a2f097e6bd2fc5123e6f2','addUniqueConstraint constraintName=idx_cluster_membership_uuid, tableName=ha_membership','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump57','rancher (generated)','db/core-200.xml','2017-08-14 10:59:51',57,'EXECUTED','7:ed21ecf2257baef8acee78afa4b0f0a4','addUniqueConstraint constraintName=idx_cluster_uuid, tableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump58','rancher (generated)','db/core-200.xml','2017-08-14 10:59:51',58,'EXECUTED','7:eb7886ddf499f0c91c279f5f3f2ac186','addUniqueConstraint constraintName=idx_credential_uuid, tableName=credential','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump59','rancher (generated)','db/core-200.xml','2017-08-14 10:59:51',59,'EXECUTED','7:128b4ef004e988cfea93a31059294997','addUniqueConstraint constraintName=idx_data_name, tableName=data','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump60','rancher (generated)','db/core-200.xml','2017-08-14 10:59:51',60,'EXECUTED','7:3963582c8c167c0d83cd3116d7e20912','addUniqueConstraint constraintName=idx_deployment_unit_uuid, tableName=deployment_unit','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump61','rancher (generated)','db/core-200.xml','2017-08-14 10:59:51',61,'EXECUTED','7:b3bd011eb551aefb8b6aaf2f8388eedc','addUniqueConstraint constraintName=idx_dynamic_schema_uuid, tableName=dynamic_schema','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump62','rancher (generated)','db/core-200.xml','2017-08-14 10:59:51',62,'EXECUTED','7:b5cfd46ecfff7c560bad2fd03d7e9e49','addUniqueConstraint constraintName=idx_environment_uuid, tableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump63','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',63,'EXECUTED','7:54d6efcb6135e1b9e4ce13a9e9c643c5','addUniqueConstraint constraintName=idx_external_event_uuid, tableName=external_event','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump64','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',64,'EXECUTED','7:c6d77f1d32cbe6fc6dd18aa80ffa1c2b','addUniqueConstraint constraintName=idx_generic_object_uuid, tableName=generic_object','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump65','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',65,'EXECUTED','7:372cebf015be60682e2b29afea25b18b','addUniqueConstraint constraintName=idx_host_template_uuid, tableName=host_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump66','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',66,'EXECUTED','7:053a0e19ad33e7188efe567665d50fb6','addUniqueConstraint constraintName=idx_host_uuid, tableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump67','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',67,'EXECUTED','7:2734de68ecce8d415892a4a2d1014509','addUniqueConstraint constraintName=idx_instance_uuid, tableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump68','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',68,'EXECUTED','7:7715a8739e82e6196897e49a83a734ba','addUniqueConstraint constraintName=idx_machine_driver_uuid, tableName=machine_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump69','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',69,'EXECUTED','7:ab2cefa4916f98c2a238e4af647edd12','addUniqueConstraint constraintName=idx_mount_uuid, tableName=mount','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump70','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',70,'EXECUTED','7:64a9bc201fedc31919916fade0234964','addUniqueConstraint constraintName=idx_network_driver_uuid, tableName=network_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump71','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',71,'EXECUTED','7:4b47442183e3c96516bdedd571c161a0','addUniqueConstraint constraintName=idx_network_uuid, tableName=network','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump72','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',72,'EXECUTED','7:6d4cdc759e3865fad531c0273ab941fe','addUniqueConstraint constraintName=idx_pool_item2, tableName=resource_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump73','rancher (generated)','db/core-200.xml','2017-08-14 10:59:52',73,'EXECUTED','7:10432e1ef5ce9e780d0b84a486979311','addUniqueConstraint constraintName=idx_process_execution__uuid, tableName=process_execution','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump74','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',74,'EXECUTED','7:3c7cae64fb5c26a103e2a4c271b17c02','addUniqueConstraint constraintName=idx_project_member_uuid, tableName=project_member','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump75','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',75,'EXECUTED','7:ce74fbf536358e31dcd76bf09030f1e8','addUniqueConstraint constraintName=idx_resource_pool_uuid, tableName=resource_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump76','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',76,'EXECUTED','7:506c7c48030d2673775b444d0c5b08b5','addUniqueConstraint constraintName=idx_revision_uuid, tableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump77','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',77,'EXECUTED','7:195b3f28a97e065eed07f592bf874710','addUniqueConstraint constraintName=idx_scheduled_upgrade_uuid, tableName=scheduled_upgrade','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump78','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',78,'EXECUTED','7:30613504b36885f55aec1e3facbfaba2','addUniqueConstraint constraintName=idx_secret_uuid, tableName=secret','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump79','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',79,'EXECUTED','7:3b336366396230993e15499d937737fd','addUniqueConstraint constraintName=idx_service_event_uuid, tableName=service_event','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump80','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',80,'EXECUTED','7:163f088d946d43e56314b746523cf6bc','addUniqueConstraint constraintName=idx_service_uuid, tableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump81','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',81,'EXECUTED','7:a33780f5b977436b73abc108372da671','addUniqueConstraint constraintName=idx_storage_driver_uuid, tableName=storage_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump82','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',82,'EXECUTED','7:1414d974977d180efe4f90c686f2cf2a','addUniqueConstraint constraintName=idx_storage_pool_host_map_uuid, tableName=storage_pool_host_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump83','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',83,'EXECUTED','7:17dc1738baad40884cd5db55cea1e304','addUniqueConstraint constraintName=idx_storage_pool_uuid, tableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump84','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',84,'EXECUTED','7:5e50bca2434e89fa14ffebc1c3a60e96','addUniqueConstraint constraintName=idx_subnet_uuid, tableName=subnet','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump85','rancher (generated)','db/core-200.xml','2017-08-14 10:59:53',85,'EXECUTED','7:e38fadfd3e33e6cb3ab5717ee90ce978','addUniqueConstraint constraintName=idx_user_preference_uuid, tableName=user_preference','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump86','rancher (generated)','db/core-200.xml','2017-08-14 10:59:54',86,'EXECUTED','7:3901c5e94e4b09110c21fe560468e428','addUniqueConstraint constraintName=idx_volume_storage_pool_map_uuid, tableName=volume_storage_pool_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump87','rancher (generated)','db/core-200.xml','2017-08-14 10:59:54',87,'EXECUTED','7:1b329571549e7c0ba32f3995d4afef74','addUniqueConstraint constraintName=idx_volume_template_uuid, tableName=volume_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump88','rancher (generated)','db/core-200.xml','2017-08-14 10:59:54',88,'EXECUTED','7:4533a1ac9939ae985a3df0b474921317','addUniqueConstraint constraintName=idx_volume_uuid, tableName=volume','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump89','rancher (generated)','db/core-200.xml','2017-08-14 10:59:54',89,'EXECUTED','7:3c4054589e4b6259cd62a35cf1a5d1fe','addUniqueConstraint constraintName=key, tableName=auth_token','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump90','rancher (generated)','db/core-200.xml','2017-08-14 10:59:54',90,'EXECUTED','7:868f025b1efa649d1c89ca09ab3a4d95','addUniqueConstraint constraintName=token, tableName=ui_challenge','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump91','rancher (generated)','db/core-200.xml','2017-08-14 10:59:54',91,'EXECUTED','7:5d8ef5a5f8883bbb2b6cb9c5b150c151','addUniqueConstraint constraintName=uix_key_value_name, tableName=key_value','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump92','rancher (generated)','db/core-200.xml','2017-08-14 10:59:54',92,'EXECUTED','7:a0ea11b0426c2641067717774703906f','addForeignKeyConstraint baseTableName=auth_token, constraintName=auth_token_ibfk_1, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump93','rancher (generated)','db/core-200.xml','2017-08-14 10:59:55',93,'EXECUTED','7:618a09f7dd171ea0bfc284d83d4db2d9','addForeignKeyConstraint baseTableName=account, constraintName=fk_account__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump94','rancher (generated)','db/core-200.xml','2017-08-14 10:59:55',94,'EXECUTED','7:f843206e53671f7b766c95335284df52','addForeignKeyConstraint baseTableName=agent, constraintName=fk_agent__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump95','rancher (generated)','db/core-200.xml','2017-08-14 10:59:55',95,'EXECUTED','7:3de160241bf8d9a7e685ebb1582b709e','addForeignKeyConstraint baseTableName=agent, constraintName=fk_agent__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump96','rancher (generated)','db/core-200.xml','2017-08-14 10:59:55',96,'EXECUTED','7:5e0f96452f7e0eb3de662388990e6aea','addForeignKeyConstraint baseTableName=agent, constraintName=fk_agent__resource_account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump97','rancher (generated)','db/core-200.xml','2017-08-14 10:59:56',97,'EXECUTED','7:6a1e18bae6ba591f06288a3dfda04d42','addForeignKeyConstraint baseTableName=audit_log, constraintName=fk_audit_log__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump98','rancher (generated)','db/core-200.xml','2017-08-14 10:59:56',98,'EXECUTED','7:cdd98487dfece8bba71f7592c3964ac0','addForeignKeyConstraint baseTableName=audit_log, constraintName=fk_audit_log__authenticated_as_account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump99','rancher (generated)','db/core-200.xml','2017-08-14 10:59:56',99,'EXECUTED','7:e46d51381d1c8e046126d2faa1b64a9b','addForeignKeyConstraint baseTableName=auth_token, constraintName=fk_auth_token__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump100','rancher (generated)','db/core-200.xml','2017-08-14 10:59:56',100,'EXECUTED','7:1a18f0adf693a066fcc702c43dde0acb','addForeignKeyConstraint baseTableName=catalog_file, constraintName=fk_catalog_file__version_id, referencedTableName=catalog_version','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump101','rancher (generated)','db/core-200.xml','2017-08-14 10:59:57',101,'EXECUTED','7:3a19eda0ca6db3bfdb5190028982345d','addForeignKeyConstraint baseTableName=catalog_label, constraintName=fk_catalog_label__template_id, referencedTableName=catalog_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump102','rancher (generated)','db/core-200.xml','2017-08-14 10:59:57',102,'EXECUTED','7:da42522822a77608d4a3d6966dfa2aa3','addForeignKeyConstraint baseTableName=catalog_template_category, constraintName=fk_catalog_t_catalog__category_id, referencedTableName=catalog_category','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump103','rancher (generated)','db/core-200.xml','2017-08-14 10:59:57',103,'EXECUTED','7:09636783313a94b2795cca66bceec79e','addForeignKeyConstraint baseTableName=catalog_template_category, constraintName=fk_catalog_t_category__template_id, referencedTableName=catalog_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump104','rancher (generated)','db/core-200.xml','2017-08-14 10:59:57',104,'EXECUTED','7:ec0721604bcd4ec4d108c9de1ad6c42e','addForeignKeyConstraint baseTableName=catalog_template, constraintName=fk_catalog_template__catalog_id, referencedTableName=catalog','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump105','rancher (generated)','db/core-200.xml','2017-08-14 10:59:58',105,'EXECUTED','7:83bb86136167a0dc9ac909a8617b05ab','addForeignKeyConstraint baseTableName=catalog_version, constraintName=fk_catalog_template__template_id, referencedTableName=catalog_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump106','rancher (generated)','db/core-200.xml','2017-08-14 10:59:58',106,'EXECUTED','7:a4f79c566b857420412b88d8ad5bfebb','addForeignKeyConstraint baseTableName=catalog_version_label, constraintName=fk_catalog_v_l__version_id, referencedTableName=catalog_version','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump107','rancher (generated)','db/core-200.xml','2017-08-14 10:59:58',107,'EXECUTED','7:25c4498eabba600f3022a6e36cebddc5','addForeignKeyConstraint baseTableName=certificate, constraintName=fk_cert_data__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump108','rancher (generated)','db/core-200.xml','2017-08-14 10:59:59',108,'EXECUTED','7:97be23419ac4868061924d6a1d268b4c','addForeignKeyConstraint baseTableName=certificate, constraintName=fk_certificate__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump109','rancher (generated)','db/core-200.xml','2017-08-14 10:59:59',109,'EXECUTED','7:33b288251d6c715087c36e5f0f4ec393','addForeignKeyConstraint baseTableName=cluster, constraintName=fk_cluster__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump110','rancher (generated)','db/core-200.xml','2017-08-14 10:59:59',110,'EXECUTED','7:bb2c6d2b76097c6ea6bf7eed9148687a','addForeignKeyConstraint baseTableName=cluster, constraintName=fk_cluster__network_id, referencedTableName=network','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump111','rancher (generated)','db/core-200.xml','2017-08-14 10:59:59',111,'EXECUTED','7:4ff7d8e8b39818b93f15d7eafd741240','addForeignKeyConstraint baseTableName=credential, constraintName=fk_credential__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump112','rancher (generated)','db/core-200.xml','2017-08-14 11:00:00',112,'EXECUTED','7:9851adc933f060375a662a93cad85d63','addForeignKeyConstraint baseTableName=credential, constraintName=fk_credential__registry_id, referencedTableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump113','rancher (generated)','db/core-200.xml','2017-08-14 11:00:00',113,'EXECUTED','7:f4aa92bce225a57e241decf250365886','addForeignKeyConstraint baseTableName=deployment_unit, constraintName=fk_deployment_unit__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump114','rancher (generated)','db/core-200.xml','2017-08-14 11:00:00',114,'EXECUTED','7:05ffa759e23eee2c9b31fb9ae2106b20','addForeignKeyConstraint baseTableName=deployment_unit, constraintName=fk_deployment_unit__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump115','rancher (generated)','db/core-200.xml','2017-08-14 11:00:00',115,'EXECUTED','7:4e47581bc8337c283b12005d52bef58a','addForeignKeyConstraint baseTableName=deployment_unit, constraintName=fk_deployment_unit__environment_id, referencedTableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump116','rancher (generated)','db/core-200.xml','2017-08-14 11:00:01',116,'EXECUTED','7:0d0eeedfe8aa9b8d62c4dba359e1be13','addForeignKeyConstraint baseTableName=deployment_unit, constraintName=fk_deployment_unit__host_id, referencedTableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump117','rancher (generated)','db/core-200.xml','2017-08-14 11:00:01',117,'EXECUTED','7:f70a3ed1bf08e76ddb0ec72d4ee28288','addForeignKeyConstraint baseTableName=deployment_unit, constraintName=fk_deployment_unit__revision_id, referencedTableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump118','rancher (generated)','db/core-200.xml','2017-08-14 11:00:01',118,'EXECUTED','7:3fa296dbd79e7630dcf5e338c77aa3e4','addForeignKeyConstraint baseTableName=deployment_unit, constraintName=fk_deployment_unit__service_id, referencedTableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump119','rancher (generated)','db/core-200.xml','2017-08-14 11:00:02',119,'EXECUTED','7:903d52c6dae4049a74cd2e3a12e390c2','addForeignKeyConstraint baseTableName=deployment_unit, constraintName=fk_deployment_unit_requested_revision_id, referencedTableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump120','rancher (generated)','db/core-200.xml','2017-08-14 11:00:02',120,'EXECUTED','7:b20ff2d4d939affba2dfb52a8dce92b7','addForeignKeyConstraint baseTableName=dynamic_schema, constraintName=fk_dynamic_schema__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump121','rancher (generated)','db/core-200.xml','2017-08-14 11:00:02',121,'EXECUTED','7:8641e1640a2ed05ac4d85c203e42738b','addForeignKeyConstraint baseTableName=dynamic_schema, constraintName=fk_dynamic_schema__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump122','rancher (generated)','db/core-200.xml','2017-08-14 11:00:03',122,'EXECUTED','7:bd4684797dbddc00e2d0068d6e376e49','addForeignKeyConstraint baseTableName=dynamic_schema, constraintName=fk_dynamic_schema__service_id, referencedTableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump123','rancher (generated)','db/core-200.xml','2017-08-14 11:00:03',123,'EXECUTED','7:ba5774913cfcd281e505f9e38da8ab12','addForeignKeyConstraint baseTableName=dynamic_schema_role, constraintName=fk_dynamic_schema_role_dynamic_schema_id, referencedTableName=dynamic_schema','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump124','rancher (generated)','db/core-200.xml','2017-08-14 11:00:03',124,'EXECUTED','7:b8b2d6afd9c4b18d302a9ddfb4fdf2a1','addForeignKeyConstraint baseTableName=stack, constraintName=fk_environment__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump125','rancher (generated)','db/core-200.xml','2017-08-14 11:00:03',125,'EXECUTED','7:2fccf07b3810d6cbc7bb4b2850bae213','addForeignKeyConstraint baseTableName=stack, constraintName=fk_environment_environment_id, referencedTableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump126','rancher (generated)','db/core-200.xml','2017-08-14 11:00:04',126,'EXECUTED','7:51cac4d46c52a347a3236838c6228e04','addForeignKeyConstraint baseTableName=external_event, constraintName=fk_external_event__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump127','rancher (generated)','db/core-200.xml','2017-08-14 11:00:04',127,'EXECUTED','7:06d74153b6f1d93f3911ebaa25a22081','addForeignKeyConstraint baseTableName=external_event, constraintName=fk_external_event__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump128','rancher (generated)','db/core-200.xml','2017-08-14 11:00:04',128,'EXECUTED','7:1df2a99590037d40b0efd1d5cdbc4e86','addForeignKeyConstraint baseTableName=external_event, constraintName=fk_external_event__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump129','rancher (generated)','db/core-200.xml','2017-08-14 11:00:05',129,'EXECUTED','7:b2b6b63bc2f66c86843c34647edd95d2','addForeignKeyConstraint baseTableName=external_event, constraintName=fk_external_event__reported_account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump130','rancher (generated)','db/core-200.xml','2017-08-14 11:00:05',130,'EXECUTED','7:600e48bcf75a797e50183c3101b2495f','addForeignKeyConstraint baseTableName=generic_object, constraintName=fk_generic_object__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump131','rancher (generated)','db/core-200.xml','2017-08-14 11:00:05',131,'EXECUTED','7:63cc2e2ce3db6c95ef4c41151cd7b82d','addForeignKeyConstraint baseTableName=generic_object, constraintName=fk_generic_object__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump132','rancher (generated)','db/core-200.xml','2017-08-14 11:00:05',132,'EXECUTED','7:e0c2bab87786b6f69357db8eb5248649','addForeignKeyConstraint baseTableName=generic_object, constraintName=fk_generic_object__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump133','rancher (generated)','db/core-200.xml','2017-08-14 11:00:06',133,'EXECUTED','7:635cabddc60e4c4bc06e35e6f0df0297','addForeignKeyConstraint baseTableName=host, constraintName=fk_host__agent_id, referencedTableName=agent','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump134','rancher (generated)','db/core-200.xml','2017-08-14 11:00:06',134,'EXECUTED','7:0e116787b7f26d675eb70aecbbe405a5','addForeignKeyConstraint baseTableName=host, constraintName=fk_host__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump135','rancher (generated)','db/core-200.xml','2017-08-14 11:00:06',135,'EXECUTED','7:38a20f57addc1b34bb7c5f3b982ee1f3','addForeignKeyConstraint baseTableName=host, constraintName=fk_host__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump136','rancher (generated)','db/core-200.xml','2017-08-14 11:00:06',136,'EXECUTED','7:566f85abfaee5d0c67197340bba8de9d','addForeignKeyConstraint baseTableName=host, constraintName=fk_host__environment_id, referencedTableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump137','rancher (generated)','db/core-200.xml','2017-08-14 11:00:07',137,'EXECUTED','7:43f6b9eb32bac5f7822666bfe0d6f4ba','addForeignKeyConstraint baseTableName=host, constraintName=fk_host__host_template_id, referencedTableName=host_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump138','rancher (generated)','db/core-200.xml','2017-08-14 11:00:07',138,'EXECUTED','7:5b1f47ab83d36141534fba5ea707ae29','addForeignKeyConstraint baseTableName=host_template, constraintName=fk_host_template__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump139','rancher (generated)','db/core-200.xml','2017-08-14 11:00:07',139,'EXECUTED','7:59861405689dd84ff8721bba72538b0d','addForeignKeyConstraint baseTableName=host_template, constraintName=fk_host_template__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump140','rancher (generated)','db/core-200.xml','2017-08-14 11:00:08',140,'EXECUTED','7:e9f5a46795e093d71eb2d06b8d8b6e94','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump141','rancher (generated)','db/core-200.xml','2017-08-14 11:00:08',141,'EXECUTED','7:6401dceaabc8f5bfec5d1540a5c7cfab','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__agent_id, referencedTableName=agent','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump142','rancher (generated)','db/core-200.xml','2017-08-14 11:00:08',142,'EXECUTED','7:a1a0a34923106abc90ee8415e7faeec5','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump143','rancher (generated)','db/core-200.xml','2017-08-14 11:00:08',143,'EXECUTED','7:d7207fb994374fcc60b29b8bf5fa668f','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump144','rancher (generated)','db/core-200.xml','2017-08-14 11:00:09',144,'EXECUTED','7:a7d0553ccd071f7a117ca85d24f21e2c','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__deployment_unit_id, referencedTableName=deployment_unit','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump145','rancher (generated)','db/core-200.xml','2017-08-14 11:00:09',145,'EXECUTED','7:7a77f4a85bf72c0dcf3d6d05784ac5ca','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__environment_id, referencedTableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump146','rancher (generated)','db/core-200.xml','2017-08-14 11:00:09',146,'EXECUTED','7:a0c2502e50d11359470e06a3d132a7ff','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__host_id, referencedTableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump147','rancher (generated)','db/core-200.xml','2017-08-14 11:00:10',147,'EXECUTED','7:40c597d7a31998a380d2a5bc395ced34','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__instance_id, referencedTableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump148','rancher (generated)','db/core-200.xml','2017-08-14 11:00:10',148,'EXECUTED','7:58ca8903070c1bf86cc726414e364b15','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__network_id, referencedTableName=network','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump149','rancher (generated)','db/core-200.xml','2017-08-14 11:00:11',149,'EXECUTED','7:01d79871d444a4bfdd4e26532f11ff97','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__registry_credential_id, referencedTableName=credential','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump150','rancher (generated)','db/core-200.xml','2017-08-14 11:00:11',150,'EXECUTED','7:75bbe6dfb3dcefe93809e1dbc0983cfe','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__revision_id, referencedTableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump151','rancher (generated)','db/core-200.xml','2017-08-14 11:00:11',151,'EXECUTED','7:5cac75b00a50b0308c26433f56ddc24e','addForeignKeyConstraint baseTableName=instance, constraintName=fk_instance__service_id, referencedTableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump152','rancher (generated)','db/core-200.xml','2017-08-14 11:00:12',152,'EXECUTED','7:723f85f6c3aa70c11e2ccc0b7923547b','addForeignKeyConstraint baseTableName=machine_driver, constraintName=fk_machine_driver__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump153','rancher (generated)','db/core-200.xml','2017-08-14 11:00:12',153,'EXECUTED','7:aed285f44ff6c66be6786dcfb35662ba','addForeignKeyConstraint baseTableName=mount, constraintName=fk_mount__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump154','rancher (generated)','db/core-200.xml','2017-08-14 11:00:12',154,'EXECUTED','7:cbf7eedc27f3782ef0a0fbcb6121b748','addForeignKeyConstraint baseTableName=mount, constraintName=fk_mount__instance_id, referencedTableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump155','rancher (generated)','db/core-200.xml','2017-08-14 11:00:13',155,'EXECUTED','7:5189c7af91eb55fcaf66b1c431c26967','addForeignKeyConstraint baseTableName=mount, constraintName=fk_mount__volume_id, referencedTableName=volume','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump156','rancher (generated)','db/core-200.xml','2017-08-14 11:00:13',156,'EXECUTED','7:c3a62e5fc26f6b12c5f861613717aa49','addForeignKeyConstraint baseTableName=network, constraintName=fk_network__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump157','rancher (generated)','db/core-200.xml','2017-08-14 11:00:13',157,'EXECUTED','7:89c2413e2cc5762b675fde216efc9d2f','addForeignKeyConstraint baseTableName=network, constraintName=fk_network__network_driver_id, referencedTableName=network_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump158','rancher (generated)','db/core-200.xml','2017-08-14 11:00:13',158,'EXECUTED','7:0d8902ef4f66840b87a8a7ab78937485','addForeignKeyConstraint baseTableName=network_driver, constraintName=fk_network_driver__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump159','rancher (generated)','db/core-200.xml','2017-08-14 11:00:14',159,'EXECUTED','7:3aa50eff76c926968cfa2c73349c8345','addForeignKeyConstraint baseTableName=network_driver, constraintName=fk_network_driver__service_id, referencedTableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump160','rancher (generated)','db/core-200.xml','2017-08-14 11:00:14',160,'EXECUTED','7:b3956cf6161e7fd54d0e6fe16f17a0da','addForeignKeyConstraint baseTableName=process_execution, constraintName=fk_process_execution_process_instance_id, referencedTableName=process_instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump161','rancher (generated)','db/core-200.xml','2017-08-14 11:00:14',161,'EXECUTED','7:93ea6eaa6dc7ce88bf11fac07445de84','addForeignKeyConstraint baseTableName=process_instance, constraintName=fk_process_instance__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump162','rancher (generated)','db/core-200.xml','2017-08-14 11:00:14',162,'EXECUTED','7:55af92ddf86aea95ec1eb90802681c05','addForeignKeyConstraint baseTableName=process_instance, constraintName=fk_process_instance__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump163','rancher (generated)','db/core-200.xml','2017-08-14 11:00:15',163,'EXECUTED','7:1d057b6332fda7c85edb7c5f8aba178e','addForeignKeyConstraint baseTableName=project_member, constraintName=fk_project_member__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump164','rancher (generated)','db/core-200.xml','2017-08-14 11:00:15',164,'EXECUTED','7:949fa326f5e7ff27ac9f5ca0e97a39cf','addForeignKeyConstraint baseTableName=project_member, constraintName=fk_project_member__project_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump165','rancher (generated)','db/core-200.xml','2017-08-14 11:00:15',165,'EXECUTED','7:56b9612f8e5d82e7117abcfae212cc47','addForeignKeyConstraint baseTableName=resource_pool, constraintName=fk_resource_pool__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump166','rancher (generated)','db/core-200.xml','2017-08-14 11:00:15',166,'EXECUTED','7:9d9531f5b7226e00a9fa64592f687bec','addForeignKeyConstraint baseTableName=revision, constraintName=fk_revision__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump167','rancher (generated)','db/core-200.xml','2017-08-14 11:00:16',167,'EXECUTED','7:a296dbb3edc726650078df95bb9edd31','addForeignKeyConstraint baseTableName=revision, constraintName=fk_revision__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump168','rancher (generated)','db/core-200.xml','2017-08-14 11:00:16',168,'EXECUTED','7:9403ea916f756856e5d4121add9a16e2','addForeignKeyConstraint baseTableName=revision, constraintName=fk_revision__service_id, referencedTableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump169','rancher (generated)','db/core-200.xml','2017-08-14 11:00:16',169,'EXECUTED','7:83dcac148545ae2a9998021abf63b370','addForeignKeyConstraint baseTableName=scheduled_upgrade, constraintName=fk_scheduled_upgrade__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump170','rancher (generated)','db/core-200.xml','2017-08-14 11:00:16',170,'EXECUTED','7:e41b77087bcf360d0e6a57ab17b568e5','addForeignKeyConstraint baseTableName=scheduled_upgrade, constraintName=fk_scheduled_upgrade__environment_id, referencedTableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump171','rancher (generated)','db/core-200.xml','2017-08-14 11:00:17',171,'EXECUTED','7:4bcf17d6aeb0f8aa234989cad84344d8','addForeignKeyConstraint baseTableName=secret, constraintName=fk_secret__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump172','rancher (generated)','db/core-200.xml','2017-08-14 11:00:17',172,'EXECUTED','7:c9214a017cf5fa6daf061f13f6fb4891','addForeignKeyConstraint baseTableName=secret, constraintName=fk_secret__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump173','rancher (generated)','db/core-200.xml','2017-08-14 11:00:17',173,'EXECUTED','7:d7b108f533610874e989a103f99d9702','addForeignKeyConstraint baseTableName=secret, constraintName=fk_secret__environment_id, referencedTableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump174','rancher (generated)','db/core-200.xml','2017-08-14 11:00:18',174,'EXECUTED','7:380b97b22c75ecd7a94a372a07a219ae','addForeignKeyConstraint baseTableName=service, constraintName=fk_service__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump175','rancher (generated)','db/core-200.xml','2017-08-14 11:00:18',175,'EXECUTED','7:7873855829d5a388e0ad4a830bc2c68d','addForeignKeyConstraint baseTableName=service, constraintName=fk_service__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump176','rancher (generated)','db/core-200.xml','2017-08-14 11:00:18',176,'EXECUTED','7:5d7dde110d55daa9a8eb8fba30b44c17','addForeignKeyConstraint baseTableName=service, constraintName=fk_service__environment_id, referencedTableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump177','rancher (generated)','db/core-200.xml','2017-08-14 11:00:18',177,'EXECUTED','7:1eff8f517591f433aba8cc4ddbcd8a38','addForeignKeyConstraint baseTableName=service, constraintName=fk_service__previous_revision_id, referencedTableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump178','rancher (generated)','db/core-200.xml','2017-08-14 11:00:19',178,'EXECUTED','7:b938c391fc7871ac8cc944d277e34ce7','addForeignKeyConstraint baseTableName=service, constraintName=fk_service__revision_id, referencedTableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump179','rancher (generated)','db/core-200.xml','2017-08-14 11:00:19',179,'EXECUTED','7:f4d6ed08ca38a5114c63e9c69f915803','addForeignKeyConstraint baseTableName=service, constraintName=fk_service_cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump180','rancher (generated)','db/core-200.xml','2017-08-14 11:00:19',180,'EXECUTED','7:08a7768ac1ec21ea5b01ebbcf325be82','addForeignKeyConstraint baseTableName=service_event, constraintName=fk_service_event__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump181','rancher (generated)','db/core-200.xml','2017-08-14 11:00:19',181,'EXECUTED','7:9cd41b6380df6cbe094dfc7f5047fbe3','addForeignKeyConstraint baseTableName=service_event, constraintName=fk_service_event__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump182','rancher (generated)','db/core-200.xml','2017-08-14 11:00:20',182,'EXECUTED','7:fec454a9a6af0573f2e1ba216824f17f','addForeignKeyConstraint baseTableName=service_event, constraintName=fk_service_event__host_id, referencedTableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump183','rancher (generated)','db/core-200.xml','2017-08-14 11:00:20',183,'EXECUTED','7:5daea2c6ea73b1f6acb34f81ad755ae2','addForeignKeyConstraint baseTableName=service_event, constraintName=fk_service_event__instance_id, referencedTableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump184','rancher (generated)','db/core-200.xml','2017-08-14 11:00:20',184,'EXECUTED','7:1b17ef8ac8512ee8dcd0672023e788fc','addForeignKeyConstraint baseTableName=service_log, constraintName=fk_service_log__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump185','rancher (generated)','db/core-200.xml','2017-08-14 11:00:21',185,'EXECUTED','7:72c85d3c04ed9e234c2bc03c4e07890a','addForeignKeyConstraint baseTableName=service_log, constraintName=fk_service_log__deployment_unit_id, referencedTableName=deployment_unit','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump186','rancher (generated)','db/core-200.xml','2017-08-14 11:00:21',186,'EXECUTED','7:dd14e2874ff3fc76bbf509197cdf3f69','addForeignKeyConstraint baseTableName=service_log, constraintName=fk_service_log__instance_id, referencedTableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump187','rancher (generated)','db/core-200.xml','2017-08-14 11:00:21',187,'EXECUTED','7:2ba88ce67a8349c0a47e1575773f9777','addForeignKeyConstraint baseTableName=service_log, constraintName=fk_service_log__service_id, referencedTableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump188','rancher (generated)','db/core-200.xml','2017-08-14 11:00:21',188,'EXECUTED','7:9ce88a5295e8abe4afc66c48072754c7','addForeignKeyConstraint baseTableName=stack, constraintName=fk_stack__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump189','rancher (generated)','db/core-200.xml','2017-08-14 11:00:22',189,'EXECUTED','7:be9714311e08308c641ca022b25adf47','addForeignKeyConstraint baseTableName=stack, constraintName=fk_stack__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump190','rancher (generated)','db/core-200.xml','2017-08-14 11:00:22',190,'EXECUTED','7:845b70fbd7efedb420d9c4fa234d5fbf','addForeignKeyConstraint baseTableName=storage_driver, constraintName=fk_storage_driver__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump191','rancher (generated)','db/core-200.xml','2017-08-14 11:00:22',191,'EXECUTED','7:8586fae3451837480351428ec2b29074','addForeignKeyConstraint baseTableName=storage_pool, constraintName=fk_storage_driver__id, referencedTableName=storage_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump192','rancher (generated)','db/core-200.xml','2017-08-14 11:00:22',192,'EXECUTED','7:4c57c7edbd73d9071d3790df0a551cb6','addForeignKeyConstraint baseTableName=storage_driver, constraintName=fk_storage_driver__service_id, referencedTableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump193','rancher (generated)','db/core-200.xml','2017-08-14 11:00:23',193,'EXECUTED','7:cb03ddc387db3cdf9b77e5bf4d930d5c','addForeignKeyConstraint baseTableName=storage_pool, constraintName=fk_storage_pool__agent_id, referencedTableName=agent','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump194','rancher (generated)','db/core-200.xml','2017-08-14 11:00:23',194,'EXECUTED','7:5b94f1abe92370f12ca35323d8052205','addForeignKeyConstraint baseTableName=storage_pool, constraintName=fk_storage_pool__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump195','rancher (generated)','db/core-200.xml','2017-08-14 11:00:23',195,'EXECUTED','7:0097cde3f08d70e565b0ed0700e51b2a','addForeignKeyConstraint baseTableName=storage_pool_host_map, constraintName=fk_storage_pool_host_map__host_id, referencedTableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump196','rancher (generated)','db/core-200.xml','2017-08-14 11:00:24',196,'EXECUTED','7:d2c6114a561e2d1bc48e8c22e07a8d06','addForeignKeyConstraint baseTableName=storage_pool_host_map, constraintName=fk_storage_pool_host_map__storage_pool_id, referencedTableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump197','rancher (generated)','db/core-200.xml','2017-08-14 11:00:24',197,'EXECUTED','7:eb8841ba61918105aaa00a17d4c4d8f9','addForeignKeyConstraint baseTableName=subnet, constraintName=fk_subnet__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump198','rancher (generated)','db/core-200.xml','2017-08-14 11:00:24',198,'EXECUTED','7:50fcc672bd23ad855d6fda1ff1124999','addForeignKeyConstraint baseTableName=subnet, constraintName=fk_subnet__network_id, referencedTableName=network','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump199','rancher (generated)','db/core-200.xml','2017-08-14 11:00:24',199,'EXECUTED','7:c5a98036b67991d5062cec3f756e18cb','addForeignKeyConstraint baseTableName=user_preference, constraintName=fk_user_preference__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump200','rancher (generated)','db/core-200.xml','2017-08-14 11:00:25',200,'EXECUTED','7:18d8c7a92a2b2f0eebd2b9cc44f1e965','addForeignKeyConstraint baseTableName=volume, constraintName=fk_volume__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump201','rancher (generated)','db/core-200.xml','2017-08-14 11:00:25',201,'EXECUTED','7:caacf688f970849163e93b38a8ea8fe3','addForeignKeyConstraint baseTableName=volume, constraintName=fk_volume__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump202','rancher (generated)','db/core-200.xml','2017-08-14 11:00:25',202,'EXECUTED','7:67a32da4015eb1769b164cbb4a36af5d','addForeignKeyConstraint baseTableName=volume, constraintName=fk_volume__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump203','rancher (generated)','db/core-200.xml','2017-08-14 11:00:26',203,'EXECUTED','7:94867187dfa80c1caffd715614d609b4','addForeignKeyConstraint baseTableName=volume, constraintName=fk_volume__deployment_unit_id, referencedTableName=deployment_unit','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump204','rancher (generated)','db/core-200.xml','2017-08-14 11:00:26',204,'EXECUTED','7:65a8567fe6572e1d8aed026268d170ce','addForeignKeyConstraint baseTableName=volume, constraintName=fk_volume__environment_id, referencedTableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump205','rancher (generated)','db/core-200.xml','2017-08-14 11:00:26',205,'EXECUTED','7:63ca4069f36b059eadd9673684d445b6','addForeignKeyConstraint baseTableName=volume, constraintName=fk_volume__host_id, referencedTableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump206','rancher (generated)','db/core-200.xml','2017-08-14 11:00:27',206,'EXECUTED','7:e28b717871a5e4e2b35d8045902dbcdc','addForeignKeyConstraint baseTableName=volume, constraintName=fk_volume__storage_driver_id, referencedTableName=storage_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump207','rancher (generated)','db/core-200.xml','2017-08-14 11:00:27',207,'EXECUTED','7:3ceb77e7a79f285c66c49c36fb0dbf14','addForeignKeyConstraint baseTableName=volume, constraintName=fk_volume__storage_pool_id, referencedTableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump208','rancher (generated)','db/core-200.xml','2017-08-14 11:00:27',208,'EXECUTED','7:8af9eec5f5cbf444dd2c13a1a11b2700','addForeignKeyConstraint baseTableName=volume, constraintName=fk_volume__volume_template_id, referencedTableName=volume_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump209','rancher (generated)','db/core-200.xml','2017-08-14 11:00:28',209,'EXECUTED','7:c856ea11691bafece7b3abe9a5018909','addForeignKeyConstraint baseTableName=volume_storage_pool_map, constraintName=fk_volume_storage_pool_map__storage_pool_id, referencedTableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump210','rancher (generated)','db/core-200.xml','2017-08-14 11:00:28',210,'EXECUTED','7:de18b709b668aaae150b87645c563eca','addForeignKeyConstraint baseTableName=volume_storage_pool_map, constraintName=fk_volume_storage_pool_map__volume_id, referencedTableName=volume','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump211','rancher (generated)','db/core-200.xml','2017-08-14 11:00:28',211,'EXECUTED','7:b41fd38523d6919e4a6523e274312107','addForeignKeyConstraint baseTableName=volume_template, constraintName=fk_volume_template__account_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump212','rancher (generated)','db/core-200.xml','2017-08-14 11:00:28',212,'EXECUTED','7:89dc947faa087dee04041e9242dadbbb','addForeignKeyConstraint baseTableName=volume_template, constraintName=fk_volume_template__cluster_id, referencedTableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump213','rancher (generated)','db/core-200.xml','2017-08-14 11:00:29',213,'EXECUTED','7:c8df7b85247c28854db958977f1dae71','addForeignKeyConstraint baseTableName=volume_template, constraintName=fk_volume_template__creator_id, referencedTableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump214','rancher (generated)','db/core-200.xml','2017-08-14 11:00:29',214,'EXECUTED','7:55f5dbed01ee2dcc1827ff0880a18705','addForeignKeyConstraint baseTableName=volume_template, constraintName=fk_volume_template__environment_id, referencedTableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump215','rancher (generated)','db/core-200.xml','2017-08-14 11:00:29',215,'EXECUTED','7:5a89aedc6a9fb9bfcce969b242f86ad5','createIndex indexName=created_token, tableName=ui_challenge','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump216','rancher (generated)','db/core-200.xml','2017-08-14 11:00:29',216,'EXECUTED','7:aa6f2c10466e5e5658146af1dfd19e6f','createIndex indexName=fk_agent__uri, tableName=agent','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump217','rancher (generated)','db/core-200.xml','2017-08-14 11:00:29',217,'EXECUTED','7:7304d24402405ebfcbbf8a979be0b6c8','createIndex indexName=fk_host_template__account_id, tableName=host_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump218','rancher (generated)','db/core-200.xml','2017-08-14 11:00:29',218,'EXECUTED','7:f2e5560028d8447bd4e45e656765cb33','createIndex indexName=fk_storage_pool__zone_id, tableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump219','rancher (generated)','db/core-200.xml','2017-08-14 11:00:29',219,'EXECUTED','7:7ebef6446989d8753e196e06c64dcf2a','createIndex indexName=idx_account_name, tableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump220','rancher (generated)','db/core-200.xml','2017-08-14 11:00:29',220,'EXECUTED','7:7f211b4bff5a60c3586e4dacf8471d69','createIndex indexName=idx_account_remove_time, tableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump221','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',221,'EXECUTED','7:4235f7dadf568841987e5ff88b2a34df','createIndex indexName=idx_account_removed, tableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump222','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',222,'EXECUTED','7:c8c3621e95f5ba073966d613ce878975','createIndex indexName=idx_account_state, tableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump223','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',223,'EXECUTED','7:4bc29cf7565f01a325e9e22f4bb982c7','createIndex indexName=idx_agent_name, tableName=agent','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump224','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',224,'EXECUTED','7:251765fc75d3ce99464dc73bbeace618','createIndex indexName=idx_agent_remove_time, tableName=agent','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump225','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',225,'EXECUTED','7:42903ec75b4af0cbd4e0fe4c22ce04c3','createIndex indexName=idx_agent_removed, tableName=agent','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump226','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',226,'EXECUTED','7:5c1d95bb342421d5feb552120a1295fe','createIndex indexName=idx_agent_state, tableName=agent','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump227','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',227,'EXECUTED','7:009e322269a1c9710063cb500291164a','createIndex indexName=idx_audit_log_client_ip, tableName=audit_log','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump228','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',228,'EXECUTED','7:cc8d54d8a8766baaf2023737b1f2449f','createIndex indexName=idx_audit_log_created, tableName=audit_log','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump229','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',229,'EXECUTED','7:b86893ffa2f8ba18009423f087c51977','createIndex indexName=idx_audit_log_event_type, tableName=audit_log','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump230','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',230,'EXECUTED','7:5e5662331fe48725e305573a9e420261','createIndex indexName=idx_auth_token_expires, tableName=auth_token','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump231','rancher (generated)','db/core-200.xml','2017-08-14 11:00:30',231,'EXECUTED','7:bea13c489bbc2b6a203c42ac049e638c','createIndex indexName=idx_catalog_environment_id, tableName=catalog','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump232','rancher (generated)','db/core-200.xml','2017-08-14 11:00:31',232,'EXECUTED','7:78363ed9a3adc5729943d25db62d4683','createIndex indexName=idx_catalog_template_environment_id, tableName=catalog_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump233','rancher (generated)','db/core-200.xml','2017-08-14 11:00:31',233,'EXECUTED','7:e1e08ad1476b9014d28926486b786833','createIndex indexName=idx_cert_data_name, tableName=certificate','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump234','rancher (generated)','db/core-200.xml','2017-08-14 11:00:31',234,'EXECUTED','7:d1455b154c96ea9dd78ff09b412b2443','createIndex indexName=idx_cert_data_remove_time, tableName=certificate','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump235','rancher (generated)','db/core-200.xml','2017-08-14 11:00:31',235,'EXECUTED','7:705795e10d19c5b329aad57affad4613','createIndex indexName=idx_cert_data_removed, tableName=certificate','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump236','rancher (generated)','db/core-200.xml','2017-08-14 11:00:31',236,'EXECUTED','7:f3216440bf9c06f702ec84b2a09e9f24','createIndex indexName=idx_cert_data_state, tableName=certificate','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump237','rancher (generated)','db/core-200.xml','2017-08-14 11:00:31',237,'EXECUTED','7:eb84590f6b6afed83e00b45fcd86c838','createIndex indexName=idx_cluster_membership_name, tableName=ha_membership','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump238','rancher (generated)','db/core-200.xml','2017-08-14 11:00:31',238,'EXECUTED','7:8bbf8fe5d7c6557fd4da69650714d112','createIndex indexName=idx_cluster_name, tableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump239','rancher (generated)','db/core-200.xml','2017-08-14 11:00:31',239,'EXECUTED','7:ec6412b197d6d2e016846063d50807e8','createIndex indexName=idx_cluster_remove_time, tableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump240','rancher (generated)','db/core-200.xml','2017-08-14 11:00:31',240,'EXECUTED','7:9293030860b098abf8510ed93c8bcb7d','createIndex indexName=idx_cluster_removed, tableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump241','rancher (generated)','db/core-200.xml','2017-08-14 11:00:31',241,'EXECUTED','7:62b82bfca366258c09d56c90406648ca','createIndex indexName=idx_cluster_state, tableName=cluster','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump242','rancher (generated)','db/core-200.xml','2017-08-14 11:00:32',242,'EXECUTED','7:b532e2d0ea94dc11db68b9549c12efa1','createIndex indexName=idx_credential_name, tableName=credential','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump243','rancher (generated)','db/core-200.xml','2017-08-14 11:00:32',243,'EXECUTED','7:d3c049c295c654b9e0839f6c8bddd80c','createIndex indexName=idx_credential_remove_time, tableName=credential','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump244','rancher (generated)','db/core-200.xml','2017-08-14 11:00:32',244,'EXECUTED','7:f03464e4aae83d1d379184ce9346a842','createIndex indexName=idx_credential_removed, tableName=credential','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump245','rancher (generated)','db/core-200.xml','2017-08-14 11:00:32',245,'EXECUTED','7:45498f19b55aac4ba4da98a0eb310e4f','createIndex indexName=idx_credential_state, tableName=credential','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump246','rancher (generated)','db/core-200.xml','2017-08-14 11:00:32',246,'EXECUTED','7:e0c9a5919d13e12eb6ef8c6e50162c3c','createIndex indexName=idx_deployment_unit_name, tableName=deployment_unit','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump247','rancher (generated)','db/core-200.xml','2017-08-14 11:00:32',247,'EXECUTED','7:28a6175f76955660026b676c300525d5','createIndex indexName=idx_deployment_unit_remove_time, tableName=deployment_unit','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump248','rancher (generated)','db/core-200.xml','2017-08-14 11:00:32',248,'EXECUTED','7:f62c64f9d74dcab5aee3e004056d3181','createIndex indexName=idx_deployment_unit_removed, tableName=deployment_unit','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump249','rancher (generated)','db/core-200.xml','2017-08-14 11:00:32',249,'EXECUTED','7:1feb52a3ce754d95972398700b6db26d','createIndex indexName=idx_deployment_unit_state, tableName=deployment_unit','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump250','rancher (generated)','db/core-200.xml','2017-08-14 11:00:32',250,'EXECUTED','7:48266ed8c41e982f2bb901f6d00ee487','createIndex indexName=idx_dynamic_schema_name, tableName=dynamic_schema','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump251','rancher (generated)','db/core-200.xml','2017-08-14 11:00:32',251,'EXECUTED','7:16004b3ae80c580b3d294929c153a8fa','createIndex indexName=idx_dynamic_schema_removed, tableName=dynamic_schema','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump252','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',252,'EXECUTED','7:3fb253afdfcc4e334cced44eb509c9ee','createIndex indexName=idx_dynamic_schema_state, tableName=dynamic_schema','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump253','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',253,'EXECUTED','7:5de723b53d6def0545b1018ceb0ea65d','createIndex indexName=idx_environment_external_id, tableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump254','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',254,'EXECUTED','7:e4c7a3c80043917dc41b05fab71e93c5','createIndex indexName=idx_environment_name, tableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump255','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',255,'EXECUTED','7:1e5117c3dc16a6a6c3ca8fd11142c96e','createIndex indexName=idx_environment_remove_time, tableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump256','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',256,'EXECUTED','7:342cb43f4cffce3827567fb7b21023c2','createIndex indexName=idx_environment_removed, tableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump257','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',257,'EXECUTED','7:113f0fde9e8f055f9960b5b1ee8b161f','createIndex indexName=idx_environment_state, tableName=stack','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump258','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',258,'EXECUTED','7:981f2a7413b771ac5f766f062e80971f','createIndex indexName=idx_external_event_state, tableName=external_event','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump259','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',259,'EXECUTED','7:61b241c8872d6ea421c765190037165c','createIndex indexName=idx_external_ids, tableName=account','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump260','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',260,'EXECUTED','7:87211ce1d05e99ea51e4e12d052fd2c4','createIndex indexName=idx_generic_object_key, tableName=generic_object','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump261','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',261,'EXECUTED','7:8cda9d4a0996f326404fc799b3d93c0e','createIndex indexName=idx_generic_object_name, tableName=generic_object','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump262','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',262,'EXECUTED','7:ceacd1e7d5cc786691999a5e88d04c8a','createIndex indexName=idx_generic_object_remove_time, tableName=generic_object','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump263','rancher (generated)','db/core-200.xml','2017-08-14 11:00:33',263,'EXECUTED','7:941295e79a43fd7b3d80be6b3ed7fe5c','createIndex indexName=idx_generic_object_removed, tableName=generic_object','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump264','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',264,'EXECUTED','7:46b1d2527bc447c51c4f9c1d07a348b2','createIndex indexName=idx_generic_object_state, tableName=generic_object','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump265','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',265,'EXECUTED','7:c5d4048c993907fffcfa8212397b2fb9','createIndex indexName=idx_host__external_id, tableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump266','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',266,'EXECUTED','7:1266e60a9af2ca9dd967d0bef33ef50b','createIndex indexName=idx_host__remove_after, tableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump267','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',267,'EXECUTED','7:3896212ef9d6ddc6b316bce32d286d25','createIndex indexName=idx_host_name, tableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump268','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',268,'EXECUTED','7:12f725998e55d971d46e04c7215577f8','createIndex indexName=idx_host_remove_time, tableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump269','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',269,'EXECUTED','7:2d5f794ff33877fcca14c2dd49833f6b','createIndex indexName=idx_host_removed, tableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump270','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',270,'EXECUTED','7:c21f6fcceaed54fec7e94b51f5c141ad','createIndex indexName=idx_host_state, tableName=host','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump271','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',271,'EXECUTED','7:3224ee85efe6ca9543d0fa46cb72a90e','createIndex indexName=idx_host_template_name, tableName=host_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump272','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',272,'EXECUTED','7:f871d4109b5efde6607c200f96cb8ac7','createIndex indexName=idx_host_template_remove_time, tableName=host_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump273','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',273,'EXECUTED','7:7ac42e8843e2a7a20556913439bec3ef','createIndex indexName=idx_host_template_removed, tableName=host_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump274','rancher (generated)','db/core-200.xml','2017-08-14 11:00:34',274,'EXECUTED','7:b6b355358ca8ba2c71a6c2835332b4e2','createIndex indexName=idx_host_template_state, tableName=host_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump275','rancher (generated)','db/core-200.xml','2017-08-14 11:00:35',275,'EXECUTED','7:f7611a385bc2bfbf02ffaf13de0fd74e','createIndex indexName=idx_instance_external_id, tableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump276','rancher (generated)','db/core-200.xml','2017-08-14 11:00:35',276,'EXECUTED','7:3c11c41cb58574ef2ca7db68d764bcc1','createIndex indexName=idx_instance_name, tableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump277','rancher (generated)','db/core-200.xml','2017-08-14 11:00:35',277,'EXECUTED','7:6b4005c25a4505ecb09c337a23293d72','createIndex indexName=idx_instance_remove_time, tableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump278','rancher (generated)','db/core-200.xml','2017-08-14 11:00:35',278,'EXECUTED','7:784f952cca266edf09be8a3c8a6aa5e0','createIndex indexName=idx_instance_removed, tableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump279','rancher (generated)','db/core-200.xml','2017-08-14 11:00:35',279,'EXECUTED','7:1d66cb11c56f8e4299cf45435e36a735','createIndex indexName=idx_instance_state, tableName=instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump280','rancher (generated)','db/core-200.xml','2017-08-14 11:00:35',280,'EXECUTED','7:61a10117e57e03e9b4c170824c4c150e','createIndex indexName=idx_machine_driver_name, tableName=machine_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump281','rancher (generated)','db/core-200.xml','2017-08-14 11:00:35',281,'EXECUTED','7:ce4929612c47e17a0bfe90b12edb867f','createIndex indexName=idx_machine_driver_remove_time, tableName=machine_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump282','rancher (generated)','db/core-200.xml','2017-08-14 11:00:35',282,'EXECUTED','7:d53a45b17ebfde394158dfc62f1dec1a','createIndex indexName=idx_machine_driver_removed, tableName=machine_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump283','rancher (generated)','db/core-200.xml','2017-08-14 11:00:35',283,'EXECUTED','7:97a12893100045fa930e72c3414ad392','createIndex indexName=idx_machine_driver_state, tableName=machine_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump284','rancher (generated)','db/core-200.xml','2017-08-14 11:00:35',284,'EXECUTED','7:a2bee99c66a43346254df77117761261','createIndex indexName=idx_mount_name, tableName=mount','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump285','rancher (generated)','db/core-200.xml','2017-08-14 11:00:36',285,'EXECUTED','7:da609ff59d7bd12e32693b1758be7723','createIndex indexName=idx_mount_remove_time, tableName=mount','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump286','rancher (generated)','db/core-200.xml','2017-08-14 11:00:36',286,'EXECUTED','7:ac4b0a9e94721555d778f3cfd97328ea','createIndex indexName=idx_mount_removed, tableName=mount','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump287','rancher (generated)','db/core-200.xml','2017-08-14 11:00:36',287,'EXECUTED','7:e584158fc36a6ce459cc924a37ba03e3','createIndex indexName=idx_mount_state, tableName=mount','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump288','rancher (generated)','db/core-200.xml','2017-08-14 11:00:36',288,'EXECUTED','7:6a45d42b6779832acb8b8f74d09f9e42','createIndex indexName=idx_network_driver_name, tableName=network_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump289','rancher (generated)','db/core-200.xml','2017-08-14 11:00:36',289,'EXECUTED','7:e6c4ba06a30d26179c81ee96129c3a22','createIndex indexName=idx_network_driver_remove_time, tableName=network_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump290','rancher (generated)','db/core-200.xml','2017-08-14 11:00:36',290,'EXECUTED','7:84d08274705c57131378f297ff932f97','createIndex indexName=idx_network_driver_removed, tableName=network_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump291','rancher (generated)','db/core-200.xml','2017-08-14 11:00:36',291,'EXECUTED','7:d3c1ac035632184fcc4d04d629be0ba7','createIndex indexName=idx_network_driver_state, tableName=network_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump292','rancher (generated)','db/core-200.xml','2017-08-14 11:00:36',292,'EXECUTED','7:d792ea30005128894897ddb16cbbbbbd','createIndex indexName=idx_network_name, tableName=network','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump293','rancher (generated)','db/core-200.xml','2017-08-14 11:00:36',293,'EXECUTED','7:5632c8131fc302aad19857aacc89f9f4','createIndex indexName=idx_network_remove_time, tableName=network','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump294','rancher (generated)','db/core-200.xml','2017-08-14 11:00:36',294,'EXECUTED','7:38ccfe67b6fd2ed481dc9f1ec515b11f','createIndex indexName=idx_network_removed, tableName=network','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump295','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',295,'EXECUTED','7:4140ce5da433ed77c93fccba6ebf1c85','createIndex indexName=idx_network_state, tableName=network','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump296','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',296,'EXECUTED','7:9ef9b29f91de41a3dd4fa588a22c3f68','createIndex indexName=idx_pool_owner2, tableName=resource_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump297','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',297,'EXECUTED','7:ddcf793b9521f46c75b298ae96cb191c','createIndex indexName=idx_process_instance_end_time, tableName=process_instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump298','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',298,'EXECUTED','7:77c417367d4446fee2a4eb047ce4797d','createIndex indexName=idx_process_instance_et_rt_ri, tableName=process_instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump299','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',299,'EXECUTED','7:f1f2beef37463db538593c808c2cc694','createIndex indexName=idx_process_instance_priority, tableName=process_instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump300','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',300,'EXECUTED','7:05b45d522827f751db85eb5fdb66f0e7','createIndex indexName=idx_process_instance_run_after, tableName=process_instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump301','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',301,'EXECUTED','7:7791200ddd958a6cd672228de331d37c','createIndex indexName=idx_process_instance_start_time, tableName=process_instance','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump302','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',302,'EXECUTED','7:f84d314457f5fb4bdd21ee845b843a16','createIndex indexName=idx_processs_execution_created_time, tableName=process_execution','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump303','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',303,'EXECUTED','7:c171b5402eb0912f04883349e185afb1','createIndex indexName=idx_project_member_name, tableName=project_member','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump304','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',304,'EXECUTED','7:5d7c3e2979350de2f5ec3e694a464907','createIndex indexName=idx_project_member_remove_time, tableName=project_member','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump305','rancher (generated)','db/core-200.xml','2017-08-14 11:00:37',305,'EXECUTED','7:d7554ea6044d08e34fda839fa8c89a7f','createIndex indexName=idx_project_member_removed, tableName=project_member','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump306','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',306,'EXECUTED','7:5f19af3137ebb6ccc9309b01ee94d868','createIndex indexName=idx_project_member_state, tableName=project_member','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump307','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',307,'EXECUTED','7:da358d45a11459c10440744c3f23eb9f','createIndex indexName=idx_resource_pool_name, tableName=resource_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump308','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',308,'EXECUTED','7:a24bc627eb9119100ac65ba9d2cba35d','createIndex indexName=idx_resource_pool_remove_time, tableName=resource_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump309','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',309,'EXECUTED','7:c8c733bae08de72246657bc87d3ca359','createIndex indexName=idx_resource_pool_removed, tableName=resource_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump310','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',310,'EXECUTED','7:2fe276dcf12da67ce740e3681d3cecb5','createIndex indexName=idx_resource_pool_state, tableName=resource_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump311','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',311,'EXECUTED','7:cdcbcfbf071247252a2f0bbb964da8cc','createIndex indexName=idx_revision_name, tableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump312','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',312,'EXECUTED','7:2cc68dd5934680e4325f2e5e1e70a310','createIndex indexName=idx_revision_remove_time, tableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump313','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',313,'EXECUTED','7:96abae65d681e3aee7549ae88119c2a6','createIndex indexName=idx_revision_removed, tableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump314','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',314,'EXECUTED','7:c27a2415c39d6ce0b1643187cfb973ce','createIndex indexName=idx_revision_state, tableName=revision','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump315','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',315,'EXECUTED','7:0e2b8b789bc180bd04ee9f5aec16d314','createIndex indexName=idx_secret_name, tableName=secret','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump316','rancher (generated)','db/core-200.xml','2017-08-14 11:00:38',316,'EXECUTED','7:0c4f083af5a5ad1084b570bf031c4a35','createIndex indexName=idx_secret_remove_time, tableName=secret','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump317','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',317,'EXECUTED','7:0a8ba5f58831aac4259a2031d8abb09f','createIndex indexName=idx_secret_removed, tableName=secret','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump318','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',318,'EXECUTED','7:ea0db7b1f855216cebe04b58791e143d','createIndex indexName=idx_secret_state, tableName=secret','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump319','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',319,'EXECUTED','7:240ecba6c25c4f217004ecf53d08ae1f','createIndex indexName=idx_service_event_name, tableName=service_event','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump320','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',320,'EXECUTED','7:1ceece34c6ea3c45e4fb2614c4e23387','createIndex indexName=idx_service_event_remove_time, tableName=service_event','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump321','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',321,'EXECUTED','7:2c5ab785566b7d07a98b114530e4d3b2','createIndex indexName=idx_service_event_removed, tableName=service_event','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump322','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',322,'EXECUTED','7:b1c915cc69cc16efc0b4375dea741e52','createIndex indexName=idx_service_event_state, tableName=service_event','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump323','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',323,'EXECUTED','7:7dde09be75cd4ef26cb2b7f917ddbbaf','createIndex indexName=idx_service_external_id, tableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump324','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',324,'EXECUTED','7:86412915261ef948b2bae082de6e70d1','createIndex indexName=idx_service_name, tableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump325','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',325,'EXECUTED','7:1a16361fc4e1798da0162f65365e4971','createIndex indexName=idx_service_remove_time, tableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump326','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',326,'EXECUTED','7:6eb30214acec8a9498dc277d6d625afc','createIndex indexName=idx_service_removed, tableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump327','rancher (generated)','db/core-200.xml','2017-08-14 11:00:39',327,'EXECUTED','7:ec61d6b0701c26c6f478506253275cd1','createIndex indexName=idx_service_state, tableName=service','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump328','rancher (generated)','db/core-200.xml','2017-08-14 11:00:40',328,'EXECUTED','7:9359ae811e9bbb9ed8f42470b35725ac','createIndex indexName=idx_setting_name, tableName=setting','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump329','rancher (generated)','db/core-200.xml','2017-08-14 11:00:40',329,'EXECUTED','7:3a82091106fde93b13aaccdcc28121f5','createIndex indexName=idx_storage_driver_name, tableName=storage_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump330','rancher (generated)','db/core-200.xml','2017-08-14 11:00:40',330,'EXECUTED','7:1560ed4f8f29e149e393a1638315ea47','createIndex indexName=idx_storage_driver_remove_time, tableName=storage_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump331','rancher (generated)','db/core-200.xml','2017-08-14 11:00:40',331,'EXECUTED','7:a1700fd436c0f5c820e6550022ef2d11','createIndex indexName=idx_storage_driver_removed, tableName=storage_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump332','rancher (generated)','db/core-200.xml','2017-08-14 11:00:40',332,'EXECUTED','7:79eef14272f04349daf5c515c4864c85','createIndex indexName=idx_storage_driver_state, tableName=storage_driver','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump333','rancher (generated)','db/core-200.xml','2017-08-14 11:00:40',333,'EXECUTED','7:a89bcc77421e46e0e44752bed5e76417','createIndex indexName=idx_storage_pool_host_map_name, tableName=storage_pool_host_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump334','rancher (generated)','db/core-200.xml','2017-08-14 11:00:40',334,'EXECUTED','7:ead2d45335f0b8adc05fa8d361d8f120','createIndex indexName=idx_storage_pool_host_map_remove_time, tableName=storage_pool_host_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump335','rancher (generated)','db/core-200.xml','2017-08-14 11:00:40',335,'EXECUTED','7:18d4029d37e568f0d11e0f5b88e70738','createIndex indexName=idx_storage_pool_host_map_removed, tableName=storage_pool_host_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump336','rancher (generated)','db/core-200.xml','2017-08-14 11:00:40',336,'EXECUTED','7:359a23c691262d085c3f05db0bae4017','createIndex indexName=idx_storage_pool_host_map_state, tableName=storage_pool_host_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump337','rancher (generated)','db/core-200.xml','2017-08-14 11:00:40',337,'EXECUTED','7:5bd182cf00ca276a2448593b6a962d72','createIndex indexName=idx_storage_pool_name, tableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump338','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',338,'EXECUTED','7:cac59da7b071b29eb6e03f8dba2e5840','createIndex indexName=idx_storage_pool_remove_time, tableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump339','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',339,'EXECUTED','7:1de8a10a1d6cada7bc594081a6555856','createIndex indexName=idx_storage_pool_removed, tableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump340','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',340,'EXECUTED','7:8707e9de2c73ae74269272f06a61e6e0','createIndex indexName=idx_storage_pool_state, tableName=storage_pool','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump341','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',341,'EXECUTED','7:133f96410addd5523a4574b273e4b88b','createIndex indexName=idx_subnet_name, tableName=subnet','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump342','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',342,'EXECUTED','7:856f204b9435eea1ac265b0f6499fbc6','createIndex indexName=idx_subnet_remove_time, tableName=subnet','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump343','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',343,'EXECUTED','7:a428380845570b8dcf87a29b212f6fd3','createIndex indexName=idx_subnet_removed, tableName=subnet','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump344','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',344,'EXECUTED','7:ad6a8cc1bb7b3daa167d77be87ca2adf','createIndex indexName=idx_subnet_state, tableName=subnet','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump345','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',345,'EXECUTED','7:255c8772375066cb6b155c28a8c68b8f','createIndex indexName=idx_user_preference_name, tableName=user_preference','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump346','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',346,'EXECUTED','7:e240e0bb982c45c834c56b15d47157db','createIndex indexName=idx_user_preference_remove_time, tableName=user_preference','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump347','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',347,'EXECUTED','7:4fdeb2928bc670901e6332d24d106b5b','createIndex indexName=idx_user_preference_removed, tableName=user_preference','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump348','rancher (generated)','db/core-200.xml','2017-08-14 11:00:41',348,'EXECUTED','7:14fec1c3752121d479b27732e45c7075','createIndex indexName=idx_user_preference_state, tableName=user_preference','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump349','rancher (generated)','db/core-200.xml','2017-08-14 11:00:42',349,'EXECUTED','7:0733e7166cf961e8f0d9f861c87609aa','createIndex indexName=idx_volume_external_id, tableName=volume','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump350','rancher (generated)','db/core-200.xml','2017-08-14 11:00:42',350,'EXECUTED','7:359689d2ffbdfb9926f67e2573220c73','createIndex indexName=idx_volume_name, tableName=volume','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump351','rancher (generated)','db/core-200.xml','2017-08-14 11:00:42',351,'EXECUTED','7:b640857fedd3b5b7cd0d19ddf74a9613','createIndex indexName=idx_volume_remove_time, tableName=volume','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump352','rancher (generated)','db/core-200.xml','2017-08-14 11:00:42',352,'EXECUTED','7:0e1e504d4c51ae203eb52f1a75d3c211','createIndex indexName=idx_volume_removed, tableName=volume','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump353','rancher (generated)','db/core-200.xml','2017-08-14 11:00:42',353,'EXECUTED','7:a60cf8d1129602f3d4d3720f018481df','createIndex indexName=idx_volume_state, tableName=volume','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump354','rancher (generated)','db/core-200.xml','2017-08-14 11:00:42',354,'EXECUTED','7:a6dcdc82dbcbc5e156bac27aec2b46c8','createIndex indexName=idx_volume_storage_pool_map_name, tableName=volume_storage_pool_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump355','rancher (generated)','db/core-200.xml','2017-08-14 11:00:42',355,'EXECUTED','7:aec2d117078daec21bb5d2e886447493','createIndex indexName=idx_volume_storage_pool_map_remove_time, tableName=volume_storage_pool_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump356','rancher (generated)','db/core-200.xml','2017-08-14 11:00:42',356,'EXECUTED','7:74c6c271f5479776bda9b3816363a08d','createIndex indexName=idx_volume_storage_pool_map_removed, tableName=volume_storage_pool_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump357','rancher (generated)','db/core-200.xml','2017-08-14 11:00:42',357,'EXECUTED','7:5b239073066176b78afc445dc0bfe976','createIndex indexName=idx_volume_storage_pool_map_state, tableName=volume_storage_pool_map','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump358','rancher (generated)','db/core-200.xml','2017-08-14 11:00:42',358,'EXECUTED','7:aa891ec5cdbdf87f8b9297ef3a405221','createIndex indexName=idx_volume_template_name, tableName=volume_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump359','rancher (generated)','db/core-200.xml','2017-08-14 11:00:43',359,'EXECUTED','7:080cf191cb83a67c12ad4ec1bd9cc6c4','createIndex indexName=idx_volume_template_remove_time, tableName=volume_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump360','rancher (generated)','db/core-200.xml','2017-08-14 11:00:43',360,'EXECUTED','7:f83c0a750b43ff3f21c641275257885f','createIndex indexName=idx_volume_template_removed, tableName=volume_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump361','rancher (generated)','db/core-200.xml','2017-08-14 11:00:43',361,'EXECUTED','7:648c5be239e801f2b6b501a4403c068c','createIndex indexName=idx_volume_template_state, tableName=volume_template','',NULL,'3.5.3',NULL,NULL,'2733585135'),('dump362','rancher (generated)','db/core-200.xml','2017-08-14 11:00:43',362,'EXECUTED','7:f318dc5a1bff455232ace27c15d8da96','createIndex indexName=idx_volume_uri, tableName=volume','',NULL,'3.5.3',NULL,NULL,'2733585135');
/*!40000 ALTER TABLE `DATABASECHANGELOG` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `DATABASECHANGELOGLOCK`
--

DROP TABLE IF EXISTS `DATABASECHANGELOGLOCK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DATABASECHANGELOGLOCK` (
  `ID` int(11) NOT NULL,
  `LOCKED` bit(1) NOT NULL,
  `LOCKGRANTED` datetime DEFAULT NULL,
  `LOCKEDBY` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DATABASECHANGELOGLOCK`
--

LOCK TABLES `DATABASECHANGELOGLOCK` WRITE;
/*!40000 ALTER TABLE `DATABASECHANGELOGLOCK` DISABLE KEYS */;
/*!40000 ALTER TABLE `DATABASECHANGELOGLOCK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `account`
--

DROP TABLE IF EXISTS `account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `external_id` varchar(255) DEFAULT NULL,
  `external_id_type` varchar(128) DEFAULT NULL,
  `version` varchar(128) DEFAULT NULL,
  `cluster_id` bigint(20) DEFAULT NULL,
  `cluster_owner` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_account_uuid` (`uuid`),
  KEY `fk_account__cluster_id` (`cluster_id`),
  KEY `idx_account_name` (`name`),
  KEY `idx_account_remove_time` (`remove_time`),
  KEY `idx_account_removed` (`removed`),
  KEY `idx_account_state` (`state`),
  KEY `idx_external_ids` (`external_id`,`external_id_type`),
  CONSTRAINT `fk_account__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account`
--

LOCK TABLES `account` WRITE;
/*!40000 ALTER TABLE `account` DISABLE KEYS */;
/*!40000 ALTER TABLE `account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `agent`
--

DROP TABLE IF EXISTS `agent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `agent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `uri` varchar(255) DEFAULT NULL,
  `managed_config` bit(1) NOT NULL DEFAULT b'1',
  `resource_account_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_agent_uuid` (`uuid`),
  KEY `fk_agent__account_id` (`account_id`),
  KEY `fk_agent__cluster_id` (`cluster_id`),
  KEY `fk_agent__resource_account_id` (`resource_account_id`),
  KEY `fk_agent__uri` (`uri`),
  KEY `idx_agent_name` (`name`),
  KEY `idx_agent_remove_time` (`remove_time`),
  KEY `idx_agent_removed` (`removed`),
  KEY `idx_agent_state` (`state`),
  CONSTRAINT `fk_agent__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_agent__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_agent__resource_account_id` FOREIGN KEY (`resource_account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agent`
--

LOCK TABLES `agent` WRITE;
/*!40000 ALTER TABLE `agent` DISABLE KEYS */;
/*!40000 ALTER TABLE `agent` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) DEFAULT NULL,
  `authenticated_as_account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `auth_type` varchar(255) DEFAULT NULL,
  `event_type` varchar(255) NOT NULL,
  `resource_type` varchar(255) NOT NULL,
  `resource_id` bigint(20) DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `data` mediumtext,
  `authenticated_as_identity_id` varchar(255) DEFAULT NULL,
  `runtime` bigint(20) DEFAULT NULL,
  `client_ip` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_audit_log__account_id` (`account_id`),
  KEY `fk_audit_log__authenticated_as_account_id` (`authenticated_as_account_id`),
  KEY `idx_audit_log_client_ip` (`client_ip`),
  KEY `idx_audit_log_created` (`created`),
  KEY `idx_audit_log_event_type` (`event_type`),
  CONSTRAINT `fk_audit_log__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_audit_log__authenticated_as_account_id` FOREIGN KEY (`authenticated_as_account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auth_token`
--

DROP TABLE IF EXISTS `auth_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `auth_token` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `created` datetime NOT NULL,
  `expires` datetime NOT NULL,
  `key` varchar(40) NOT NULL,
  `value` mediumtext NOT NULL,
  `version` varchar(255) NOT NULL,
  `provider` varchar(255) NOT NULL,
  `authenticated_as_account_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_auth_token_key` (`key`),
  UNIQUE KEY `key` (`key`),
  KEY `auth_token_ibfk_1` (`authenticated_as_account_id`),
  KEY `fk_auth_token__account_id` (`account_id`),
  KEY `idx_auth_token_expires` (`expires`),
  CONSTRAINT `auth_token_ibfk_1` FOREIGN KEY (`authenticated_as_account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_auth_token__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auth_token`
--

LOCK TABLES `auth_token` WRITE;
/*!40000 ALTER TABLE `auth_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `auth_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog`
--

DROP TABLE IF EXISTS `catalog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `catalog` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `environment_id` varchar(255) DEFAULT NULL,
  `name` varchar(1024) DEFAULT NULL,
  `url` varchar(1024) DEFAULT NULL,
  `branch` varchar(1024) DEFAULT NULL,
  `commit` varchar(1024) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `kind` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_catalog_environment_id` (`environment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog`
--

LOCK TABLES `catalog` WRITE;
/*!40000 ALTER TABLE `catalog` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_category`
--

DROP TABLE IF EXISTS `catalog_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `catalog_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `name` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_category`
--

LOCK TABLES `catalog_category` WRITE;
/*!40000 ALTER TABLE `catalog_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_file`
--

DROP TABLE IF EXISTS `catalog_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `catalog_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `version_id` bigint(20) DEFAULT NULL,
  `name` varchar(1024) DEFAULT NULL,
  `contents` mediumblob,
  PRIMARY KEY (`id`),
  KEY `fk_catalog_file__version_id` (`version_id`),
  CONSTRAINT `fk_catalog_file__version_id` FOREIGN KEY (`version_id`) REFERENCES `catalog_version` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=1157 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_file`
--

LOCK TABLES `catalog_file` WRITE;
/*!40000 ALTER TABLE `catalog_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_label`
--

DROP TABLE IF EXISTS `catalog_label`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `catalog_label` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `template_id` bigint(20) DEFAULT NULL,
  `key` varchar(1024) DEFAULT NULL,
  `value` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_catalog_label__template_id` (`template_id`),
  CONSTRAINT `fk_catalog_label__template_id` FOREIGN KEY (`template_id`) REFERENCES `catalog_template` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_label`
--

LOCK TABLES `catalog_label` WRITE;
/*!40000 ALTER TABLE `catalog_label` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_label` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_template`
--

DROP TABLE IF EXISTS `catalog_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `catalog_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `environment_id` varchar(255) DEFAULT NULL,
  `catalog_id` bigint(20) DEFAULT NULL,
  `name` varchar(1024) DEFAULT NULL,
  `is_system` varchar(255) DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `default_version` varchar(1024) DEFAULT NULL,
  `path` varchar(1024) DEFAULT NULL,
  `maintainer` varchar(1024) DEFAULT NULL,
  `license` mediumtext,
  `project_url` varchar(1024) DEFAULT NULL,
  `upgrade_from` varchar(1024) DEFAULT NULL,
  `folder_name` varchar(1024) DEFAULT NULL,
  `catalog` varchar(1024) DEFAULT NULL,
  `base` varchar(1024) DEFAULT NULL,
  `icon` mediumtext,
  `icon_filename` varchar(255) DEFAULT NULL,
  `readme` mediumblob,
  PRIMARY KEY (`id`),
  KEY `fk_catalog_template__catalog_id` (`catalog_id`),
  KEY `idx_catalog_template_environment_id` (`environment_id`),
  CONSTRAINT `fk_catalog_template__catalog_id` FOREIGN KEY (`catalog_id`) REFERENCES `catalog` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=199 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_template`
--

LOCK TABLES `catalog_template` WRITE;
/*!40000 ALTER TABLE `catalog_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_template_category`
--

DROP TABLE IF EXISTS `catalog_template_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `catalog_template_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `template_id` bigint(20) DEFAULT NULL,
  `category_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_catalog_t_catalog__category_id` (`category_id`),
  KEY `fk_catalog_t_category__template_id` (`template_id`),
  CONSTRAINT `fk_catalog_t_catalog__category_id` FOREIGN KEY (`category_id`) REFERENCES `catalog_category` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_catalog_t_category__template_id` FOREIGN KEY (`template_id`) REFERENCES `catalog_template` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=177 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_template_category`
--

LOCK TABLES `catalog_template_category` WRITE;
/*!40000 ALTER TABLE `catalog_template_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_template_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_version`
--

DROP TABLE IF EXISTS `catalog_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `catalog_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `template_id` bigint(20) DEFAULT NULL,
  `revision` bigint(20) DEFAULT NULL,
  `version` varchar(1024) DEFAULT NULL,
  `minimum_rancher_version` varchar(1024) DEFAULT NULL,
  `maximum_rancher_version` varchar(1024) DEFAULT NULL,
  `upgrade_from` varchar(1024) DEFAULT NULL,
  `readme` mediumblob,
  PRIMARY KEY (`id`),
  KEY `fk_catalog_template__template_id` (`template_id`),
  CONSTRAINT `fk_catalog_template__template_id` FOREIGN KEY (`template_id`) REFERENCES `catalog_template` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=448 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_version`
--

LOCK TABLES `catalog_version` WRITE;
/*!40000 ALTER TABLE `catalog_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_version_label`
--

DROP TABLE IF EXISTS `catalog_version_label`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `catalog_version_label` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version_id` bigint(20) DEFAULT NULL,
  `key` varchar(1024) DEFAULT NULL,
  `value` varchar(1024) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_catalog_v_l__version_id` (`version_id`),
  CONSTRAINT `fk_catalog_v_l__version_id` FOREIGN KEY (`version_id`) REFERENCES `catalog_version` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_version_label`
--

LOCK TABLES `catalog_version_label` WRITE;
/*!40000 ALTER TABLE `catalog_version_label` DISABLE KEYS */;
/*!40000 ALTER TABLE `catalog_version_label` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `certificate`
--

DROP TABLE IF EXISTS `certificate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `certificate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `cert_chain` text,
  `cert` text,
  `key` text,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_cert_data_uuid` (`uuid`),
  KEY `fk_cert_data__account_id` (`account_id`),
  KEY `fk_certificate__creator_id` (`creator_id`),
  KEY `idx_cert_data_name` (`name`),
  KEY `idx_cert_data_remove_time` (`remove_time`),
  KEY `idx_cert_data_removed` (`removed`),
  KEY `idx_cert_data_state` (`state`),
  CONSTRAINT `fk_cert_data__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_certificate__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `certificate`
--

LOCK TABLES `certificate` WRITE;
/*!40000 ALTER TABLE `certificate` DISABLE KEYS */;
/*!40000 ALTER TABLE `certificate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cluster`
--

DROP TABLE IF EXISTS `cluster`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cluster` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `embedded` bit(1) NOT NULL DEFAULT b'0',
  `creator_id` bigint(20) DEFAULT NULL,
  `default_network_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_cluster_uuid` (`uuid`),
  KEY `fk_cluster__creator_id` (`creator_id`),
  KEY `fk_cluster__network_id` (`default_network_id`),
  KEY `idx_cluster_name` (`name`),
  KEY `idx_cluster_remove_time` (`remove_time`),
  KEY `idx_cluster_removed` (`removed`),
  KEY `idx_cluster_state` (`state`),
  CONSTRAINT `fk_cluster__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_cluster__network_id` FOREIGN KEY (`default_network_id`) REFERENCES `network` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cluster`
--

LOCK TABLES `cluster` WRITE;
/*!40000 ALTER TABLE `cluster` DISABLE KEYS */;
/*!40000 ALTER TABLE `cluster` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `credential`
--

DROP TABLE IF EXISTS `credential`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `credential` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `public_value` varchar(4096) DEFAULT NULL,
  `secret_value` varchar(4096) DEFAULT NULL,
  `registry_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_credential_uuid` (`uuid`),
  KEY `fk_credential__account_id` (`account_id`),
  KEY `fk_credential__registry_id` (`registry_id`),
  KEY `idx_credential_name` (`name`),
  KEY `idx_credential_remove_time` (`remove_time`),
  KEY `idx_credential_removed` (`removed`),
  KEY `idx_credential_state` (`state`),
  CONSTRAINT `fk_credential__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_credential__registry_id` FOREIGN KEY (`registry_id`) REFERENCES `storage_pool` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credential`
--

LOCK TABLES `credential` WRITE;
/*!40000 ALTER TABLE `credential` DISABLE KEYS */;
/*!40000 ALTER TABLE `credential` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `data`
--

DROP TABLE IF EXISTS `data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `visible` bit(1) NOT NULL DEFAULT b'1',
  `value` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_data_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `data`
--

LOCK TABLES `data` WRITE;
/*!40000 ALTER TABLE `data` DISABLE KEYS */;
/*!40000 ALTER TABLE `data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `deployment_unit`
--

DROP TABLE IF EXISTS `deployment_unit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `deployment_unit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` text,
  `service_index` varchar(255) DEFAULT NULL,
  `service_id` bigint(20) DEFAULT NULL,
  `environment_id` bigint(20) NOT NULL,
  `host_id` bigint(20) DEFAULT NULL,
  `requested_revision_id` bigint(20) DEFAULT NULL,
  `revision_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_deployment_unit_uuid` (`uuid`),
  KEY `fk_deployment_unit__account_id` (`account_id`),
  KEY `fk_deployment_unit__cluster_id` (`cluster_id`),
  KEY `fk_deployment_unit__environment_id` (`environment_id`),
  KEY `fk_deployment_unit__host_id` (`host_id`),
  KEY `fk_deployment_unit__revision_id` (`revision_id`),
  KEY `fk_deployment_unit__service_id` (`service_id`),
  KEY `fk_deployment_unit_requested_revision_id` (`requested_revision_id`),
  KEY `idx_deployment_unit_name` (`name`),
  KEY `idx_deployment_unit_remove_time` (`remove_time`),
  KEY `idx_deployment_unit_removed` (`removed`),
  KEY `idx_deployment_unit_state` (`state`),
  CONSTRAINT `fk_deployment_unit__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_deployment_unit__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_deployment_unit__environment_id` FOREIGN KEY (`environment_id`) REFERENCES `stack` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_deployment_unit__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_deployment_unit__revision_id` FOREIGN KEY (`revision_id`) REFERENCES `revision` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_deployment_unit__service_id` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_deployment_unit_requested_revision_id` FOREIGN KEY (`requested_revision_id`) REFERENCES `revision` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `deployment_unit`
--

LOCK TABLES `deployment_unit` WRITE;
/*!40000 ALTER TABLE `deployment_unit` DISABLE KEYS */;
/*!40000 ALTER TABLE `deployment_unit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dynamic_schema`
--

DROP TABLE IF EXISTS `dynamic_schema`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dynamic_schema` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `data` text,
  `parent` varchar(255) DEFAULT NULL,
  `definition` mediumtext,
  `service_id` bigint(20) DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_dynamic_schema_uuid` (`uuid`),
  KEY `fk_dynamic_schema__account_id` (`account_id`),
  KEY `fk_dynamic_schema__creator_id` (`creator_id`),
  KEY `fk_dynamic_schema__service_id` (`service_id`),
  KEY `idx_dynamic_schema_name` (`name`),
  KEY `idx_dynamic_schema_removed` (`removed`),
  KEY `idx_dynamic_schema_state` (`state`),
  CONSTRAINT `fk_dynamic_schema__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_dynamic_schema__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_dynamic_schema__service_id` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dynamic_schema`
--

LOCK TABLES `dynamic_schema` WRITE;
/*!40000 ALTER TABLE `dynamic_schema` DISABLE KEYS */;
/*!40000 ALTER TABLE `dynamic_schema` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dynamic_schema_role`
--

DROP TABLE IF EXISTS `dynamic_schema_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dynamic_schema_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dynamic_schema_id` bigint(20) DEFAULT NULL,
  `role` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_dynamic_schema_role_dynamic_schema_id` (`dynamic_schema_id`),
  CONSTRAINT `fk_dynamic_schema_role_dynamic_schema_id` FOREIGN KEY (`dynamic_schema_id`) REFERENCES `dynamic_schema` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dynamic_schema_role`
--

LOCK TABLES `dynamic_schema_role` WRITE;
/*!40000 ALTER TABLE `dynamic_schema_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `dynamic_schema_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `external_event`
--

DROP TABLE IF EXISTS `external_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `external_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `data` text,
  `external_id` varchar(255) DEFAULT NULL,
  `event_type` varchar(255) DEFAULT NULL,
  `reported_account_id` bigint(20) DEFAULT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_external_event_uuid` (`uuid`),
  KEY `fk_external_event__account_id` (`account_id`),
  KEY `fk_external_event__cluster_id` (`cluster_id`),
  KEY `fk_external_event__creator_id` (`creator_id`),
  KEY `fk_external_event__reported_account_id` (`reported_account_id`),
  KEY `idx_external_event_state` (`state`),
  CONSTRAINT `fk_external_event__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_external_event__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_external_event__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_external_event__reported_account_id` FOREIGN KEY (`reported_account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `external_event`
--

LOCK TABLES `external_event` WRITE;
/*!40000 ALTER TABLE `external_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `external_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `generic_object`
--

DROP TABLE IF EXISTS `generic_object`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `generic_object` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `key` varchar(255) DEFAULT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_generic_object_uuid` (`uuid`),
  KEY `fk_generic_object__account_id` (`account_id`),
  KEY `fk_generic_object__cluster_id` (`cluster_id`),
  KEY `fk_generic_object__creator_id` (`creator_id`),
  KEY `idx_generic_object_key` (`key`),
  KEY `idx_generic_object_name` (`name`),
  KEY `idx_generic_object_remove_time` (`remove_time`),
  KEY `idx_generic_object_removed` (`removed`),
  KEY `idx_generic_object_state` (`state`),
  CONSTRAINT `fk_generic_object__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_generic_object__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_generic_object__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `generic_object`
--

LOCK TABLES `generic_object` WRITE;
/*!40000 ALTER TABLE `generic_object` DISABLE KEYS */;
/*!40000 ALTER TABLE `generic_object` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ha_membership`
--

DROP TABLE IF EXISTS `ha_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ha_membership` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `uuid` varchar(128) NOT NULL,
  `heartbeat` bigint(20) DEFAULT NULL,
  `config` mediumtext,
  `clustered` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_cluster_membership_uuid` (`uuid`),
  KEY `idx_cluster_membership_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ha_membership`
--

LOCK TABLES `ha_membership` WRITE;
/*!40000 ALTER TABLE `ha_membership` DISABLE KEYS */;
/*!40000 ALTER TABLE `ha_membership` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `host`
--

DROP TABLE IF EXISTS `host`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `host` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `uri` varchar(255) DEFAULT NULL,
  `agent_id` bigint(20) DEFAULT NULL,
  `agent_state` varchar(128) DEFAULT NULL,
  `local_storage_mb` bigint(20) DEFAULT NULL,
  `memory` bigint(20) DEFAULT NULL,
  `milli_cpu` bigint(20) DEFAULT NULL,
  `environment_id` bigint(20) DEFAULT NULL,
  `remove_after` datetime DEFAULT NULL,
  `host_template_id` bigint(20) DEFAULT NULL,
  `external_id` varchar(128) DEFAULT NULL,
  `revision` bigint(20) NOT NULL DEFAULT '0',
  `cluster_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_host_uuid` (`uuid`),
  KEY `fk_host__agent_id` (`agent_id`),
  KEY `fk_host__cluster_id` (`cluster_id`),
  KEY `fk_host__creator_id` (`creator_id`),
  KEY `fk_host__environment_id` (`environment_id`),
  KEY `fk_host__host_template_id` (`host_template_id`),
  KEY `idx_host__external_id` (`external_id`),
  KEY `idx_host__remove_after` (`remove_after`),
  KEY `idx_host_name` (`name`),
  KEY `idx_host_remove_time` (`remove_time`),
  KEY `idx_host_removed` (`removed`),
  KEY `idx_host_state` (`state`),
  CONSTRAINT `fk_host__agent_id` FOREIGN KEY (`agent_id`) REFERENCES `agent` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_host__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_host__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_host__environment_id` FOREIGN KEY (`environment_id`) REFERENCES `stack` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_host__host_template_id` FOREIGN KEY (`host_template_id`) REFERENCES `host_template` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `host`
--

LOCK TABLES `host` WRITE;
/*!40000 ALTER TABLE `host` DISABLE KEYS */;
/*!40000 ALTER TABLE `host` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `host_template`
--

DROP TABLE IF EXISTS `host_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `host_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `driver` varchar(255) DEFAULT NULL,
  `flavor_prefix` varchar(255) DEFAULT NULL,
  `cluster_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_host_template_uuid` (`uuid`),
  KEY `fk_host_template__cluster_id` (`cluster_id`),
  KEY `fk_host_template__creator_id` (`creator_id`),
  KEY `fk_host_template__account_id` (`account_id`),
  KEY `idx_host_template_name` (`name`),
  KEY `idx_host_template_remove_time` (`remove_time`),
  KEY `idx_host_template_removed` (`removed`),
  KEY `idx_host_template_state` (`state`),
  CONSTRAINT `fk_host_template__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_host_template__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `host_template`
--

LOCK TABLES `host_template` WRITE;
/*!40000 ALTER TABLE `host_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `host_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance`
--

DROP TABLE IF EXISTS `instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `memory_mb` bigint(20) DEFAULT NULL,
  `hostname` varchar(255) DEFAULT NULL,
  `instance_triggered_stop` varchar(128) DEFAULT NULL,
  `agent_id` bigint(20) DEFAULT NULL,
  `domain` varchar(128) DEFAULT NULL,
  `first_running` datetime DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  `userdata` text,
  `registry_credential_id` bigint(20) DEFAULT NULL,
  `external_id` varchar(128) DEFAULT NULL,
  `native_container` bit(1) NOT NULL DEFAULT b'0',
  `network_container_id` bigint(20) DEFAULT NULL,
  `health_state` varchar(128) DEFAULT NULL,
  `start_count` bigint(20) DEFAULT NULL,
  `create_index` bigint(20) DEFAULT NULL,
  `version` varchar(255) DEFAULT '0',
  `memory_reservation` bigint(20) DEFAULT NULL,
  `milli_cpu_reservation` bigint(20) DEFAULT NULL,
  `system` bit(1) NOT NULL DEFAULT b'0',
  `service_id` bigint(20) DEFAULT NULL,
  `environment_id` bigint(20) DEFAULT NULL,
  `deployment_unit_id` bigint(20) DEFAULT NULL,
  `revision_id` bigint(20) DEFAULT NULL,
  `desired` bit(1) NOT NULL DEFAULT b'1',
  `host_id` bigint(20) DEFAULT NULL,
  `network_id` bigint(20) DEFAULT NULL,
  `service_index` int(11) DEFAULT NULL,
  `upgrade_time` datetime DEFAULT NULL,
  `revision` bigint(20) NOT NULL DEFAULT '0',
  `cluster_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_instance_uuid` (`uuid`),
  KEY `fk_instance__account_id` (`account_id`),
  KEY `fk_instance__agent_id` (`agent_id`),
  KEY `fk_instance__cluster_id` (`cluster_id`),
  KEY `fk_instance__creator_id` (`creator_id`),
  KEY `fk_instance__deployment_unit_id` (`deployment_unit_id`),
  KEY `fk_instance__environment_id` (`environment_id`),
  KEY `fk_instance__host_id` (`host_id`),
  KEY `fk_instance__instance_id` (`network_container_id`),
  KEY `fk_instance__network_id` (`network_id`),
  KEY `fk_instance__registry_credential_id` (`registry_credential_id`),
  KEY `fk_instance__revision_id` (`revision_id`),
  KEY `fk_instance__service_id` (`service_id`),
  KEY `idx_instance_external_id` (`external_id`),
  KEY `idx_instance_name` (`name`),
  KEY `idx_instance_remove_time` (`remove_time`),
  KEY `idx_instance_removed` (`removed`),
  KEY `idx_instance_state` (`state`),
  CONSTRAINT `fk_instance__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__agent_id` FOREIGN KEY (`agent_id`) REFERENCES `agent` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__deployment_unit_id` FOREIGN KEY (`deployment_unit_id`) REFERENCES `deployment_unit` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__environment_id` FOREIGN KEY (`environment_id`) REFERENCES `stack` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__instance_id` FOREIGN KEY (`network_container_id`) REFERENCES `instance` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__network_id` FOREIGN KEY (`network_id`) REFERENCES `network` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__registry_credential_id` FOREIGN KEY (`registry_credential_id`) REFERENCES `credential` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__revision_id` FOREIGN KEY (`revision_id`) REFERENCES `revision` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_instance__service_id` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance`
--

LOCK TABLES `instance` WRITE;
/*!40000 ALTER TABLE `instance` DISABLE KEYS */;
/*!40000 ALTER TABLE `instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `key_value`
--

DROP TABLE IF EXISTS `key_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `key_value` (
  `name` varchar(255) DEFAULT NULL,
  `value` mediumblob,
  `revision` bigint(20) DEFAULT NULL,
  UNIQUE KEY `uix_key_value_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `key_value`
--

LOCK TABLES `key_value` WRITE;
/*!40000 ALTER TABLE `key_value` DISABLE KEYS */;
/*!40000 ALTER TABLE `key_value` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `machine_driver`
--

DROP TABLE IF EXISTS `machine_driver`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `machine_driver` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` text,
  `uri` varchar(255) DEFAULT NULL,
  `md5checksum` varchar(255) DEFAULT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_machine_driver_uuid` (`uuid`),
  KEY `fk_machine_driver__creator_id` (`creator_id`),
  KEY `idx_machine_driver_name` (`name`),
  KEY `idx_machine_driver_remove_time` (`remove_time`),
  KEY `idx_machine_driver_removed` (`removed`),
  KEY `idx_machine_driver_state` (`state`),
  CONSTRAINT `fk_machine_driver__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `machine_driver`
--

LOCK TABLES `machine_driver` WRITE;
/*!40000 ALTER TABLE `machine_driver` DISABLE KEYS */;
/*!40000 ALTER TABLE `machine_driver` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mount`
--

DROP TABLE IF EXISTS `mount`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mount` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `volume_id` bigint(20) DEFAULT NULL,
  `instance_id` bigint(20) DEFAULT NULL,
  `permissions` varchar(128) DEFAULT NULL,
  `path` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_mount_uuid` (`uuid`),
  KEY `fk_mount__account_id` (`account_id`),
  KEY `fk_mount__instance_id` (`instance_id`),
  KEY `fk_mount__volume_id` (`volume_id`),
  KEY `idx_mount_name` (`name`),
  KEY `idx_mount_remove_time` (`remove_time`),
  KEY `idx_mount_removed` (`removed`),
  KEY `idx_mount_state` (`state`),
  CONSTRAINT `fk_mount__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_mount__instance_id` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_mount__volume_id` FOREIGN KEY (`volume_id`) REFERENCES `volume` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mount`
--

LOCK TABLES `mount` WRITE;
/*!40000 ALTER TABLE `mount` DISABLE KEYS */;
/*!40000 ALTER TABLE `mount` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `network`
--

DROP TABLE IF EXISTS `network`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `network` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `domain` varchar(128) DEFAULT NULL,
  `network_driver_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_network_uuid` (`uuid`),
  KEY `fk_network__cluster_id` (`cluster_id`),
  KEY `fk_network__network_driver_id` (`network_driver_id`),
  KEY `idx_network_name` (`name`),
  KEY `idx_network_remove_time` (`remove_time`),
  KEY `idx_network_removed` (`removed`),
  KEY `idx_network_state` (`state`),
  CONSTRAINT `fk_network__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_network__network_driver_id` FOREIGN KEY (`network_driver_id`) REFERENCES `network_driver` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `network`
--

LOCK TABLES `network` WRITE;
/*!40000 ALTER TABLE `network` DISABLE KEYS */;
/*!40000 ALTER TABLE `network` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `network_driver`
--

DROP TABLE IF EXISTS `network_driver`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `network_driver` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `service_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_network_driver_uuid` (`uuid`),
  KEY `fk_network_driver__cluster_id` (`cluster_id`),
  KEY `fk_network_driver__service_id` (`service_id`),
  KEY `idx_network_driver_name` (`name`),
  KEY `idx_network_driver_remove_time` (`remove_time`),
  KEY `idx_network_driver_removed` (`removed`),
  KEY `idx_network_driver_state` (`state`),
  CONSTRAINT `fk_network_driver__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_network_driver__service_id` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `network_driver`
--

LOCK TABLES `network_driver` WRITE;
/*!40000 ALTER TABLE `network_driver` DISABLE KEYS */;
/*!40000 ALTER TABLE `network_driver` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `process_execution`
--

DROP TABLE IF EXISTS `process_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `process_execution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `process_instance_id` bigint(20) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `log` mediumtext,
  `created` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_process_execution__uuid` (`uuid`),
  KEY `fk_process_execution_process_instance_id` (`process_instance_id`),
  KEY `idx_processs_execution_created_time` (`created`),
  CONSTRAINT `fk_process_execution_process_instance_id` FOREIGN KEY (`process_instance_id`) REFERENCES `process_instance` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `process_execution`
--

LOCK TABLES `process_execution` WRITE;
/*!40000 ALTER TABLE `process_execution` DISABLE KEYS */;
/*!40000 ALTER TABLE `process_execution` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `process_instance`
--

DROP TABLE IF EXISTS `process_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `process_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `priority` int(11) DEFAULT '0',
  `process_name` varchar(128) DEFAULT NULL,
  `resource_type` varchar(128) DEFAULT NULL,
  `resource_id` varchar(128) DEFAULT NULL,
  `result` varchar(128) DEFAULT NULL,
  `exit_reason` varchar(128) DEFAULT NULL,
  `phase` varchar(128) DEFAULT NULL,
  `start_process_server_id` varchar(128) DEFAULT NULL,
  `running_process_server_id` varchar(128) DEFAULT NULL,
  `execution_count` bigint(20) NOT NULL DEFAULT '0',
  `run_after` datetime DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_process_instance__account_id` (`account_id`),
  KEY `fk_process_instance__cluster_id` (`cluster_id`),
  KEY `idx_process_instance_end_time` (`end_time`),
  KEY `idx_process_instance_et_rt_ri` (`end_time`,`resource_type`,`resource_id`),
  KEY `idx_process_instance_priority` (`priority`),
  KEY `idx_process_instance_run_after` (`run_after`),
  KEY `idx_process_instance_start_time` (`start_time`),
  CONSTRAINT `fk_process_instance__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_process_instance__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `process_instance`
--

LOCK TABLES `process_instance` WRITE;
/*!40000 ALTER TABLE `process_instance` DISABLE KEYS */;
/*!40000 ALTER TABLE `process_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_member`
--

DROP TABLE IF EXISTS `project_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_member` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `external_id` varchar(255) NOT NULL,
  `project_id` bigint(20) NOT NULL,
  `external_id_type` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_project_member_uuid` (`uuid`),
  KEY `fk_project_member__account_id` (`account_id`),
  KEY `fk_project_member__project_id` (`project_id`),
  KEY `idx_project_member_name` (`name`),
  KEY `idx_project_member_remove_time` (`remove_time`),
  KEY `idx_project_member_removed` (`removed`),
  KEY `idx_project_member_state` (`state`),
  CONSTRAINT `fk_project_member__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_project_member__project_id` FOREIGN KEY (`project_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_member`
--

LOCK TABLES `project_member` WRITE;
/*!40000 ALTER TABLE `project_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resource_pool`
--

DROP TABLE IF EXISTS `resource_pool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_pool` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `pool_type` varchar(255) DEFAULT NULL,
  `pool_id` bigint(20) DEFAULT NULL,
  `item` varchar(255) DEFAULT NULL,
  `owner_type` varchar(255) DEFAULT NULL,
  `owner_id` bigint(20) DEFAULT NULL,
  `qualifier` varchar(128) NOT NULL DEFAULT 'default',
  `sub_owner` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_resource_pool_uuid` (`uuid`),
  UNIQUE KEY `idx_pool_item2` (`pool_type`,`pool_id`,`qualifier`,`item`),
  KEY `fk_resource_pool__account_id` (`account_id`),
  KEY `idx_pool_owner2` (`pool_type`,`pool_id`,`qualifier`,`owner_type`,`owner_id`,`sub_owner`),
  KEY `idx_resource_pool_name` (`name`),
  KEY `idx_resource_pool_remove_time` (`remove_time`),
  KEY `idx_resource_pool_removed` (`removed`),
  KEY `idx_resource_pool_state` (`state`),
  CONSTRAINT `fk_resource_pool__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resource_pool`
--

LOCK TABLES `resource_pool` WRITE;
/*!40000 ALTER TABLE `resource_pool` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_pool` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `revision`
--

DROP TABLE IF EXISTS `revision`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `revision` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `service_id` bigint(20) DEFAULT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_revision_uuid` (`uuid`),
  KEY `fk_revision__account_id` (`account_id`),
  KEY `fk_revision__creator_id` (`creator_id`),
  KEY `fk_revision__service_id` (`service_id`),
  KEY `idx_revision_name` (`name`),
  KEY `idx_revision_remove_time` (`remove_time`),
  KEY `idx_revision_removed` (`removed`),
  KEY `idx_revision_state` (`state`),
  CONSTRAINT `fk_revision__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_revision__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_revision__service_id` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `revision`
--

LOCK TABLES `revision` WRITE;
/*!40000 ALTER TABLE `revision` DISABLE KEYS */;
/*!40000 ALTER TABLE `revision` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scheduled_upgrade`
--

DROP TABLE IF EXISTS `scheduled_upgrade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scheduled_upgrade` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `run_after` datetime DEFAULT NULL,
  `data` mediumtext,
  `environment_id` bigint(20) DEFAULT NULL,
  `started` datetime DEFAULT NULL,
  `finished` datetime DEFAULT NULL,
  `priority` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_scheduled_upgrade_uuid` (`uuid`),
  KEY `fk_scheduled_upgrade__account_id` (`account_id`),
  KEY `fk_scheduled_upgrade__environment_id` (`environment_id`),
  CONSTRAINT `fk_scheduled_upgrade__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_scheduled_upgrade__environment_id` FOREIGN KEY (`environment_id`) REFERENCES `stack` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scheduled_upgrade`
--

LOCK TABLES `scheduled_upgrade` WRITE;
/*!40000 ALTER TABLE `scheduled_upgrade` DISABLE KEYS */;
/*!40000 ALTER TABLE `scheduled_upgrade` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `secret`
--

DROP TABLE IF EXISTS `secret`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `secret` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `value` mediumtext,
  `environment_id` bigint(20) DEFAULT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_secret_uuid` (`uuid`),
  KEY `fk_secret__account_id` (`account_id`),
  KEY `fk_secret__creator_id` (`creator_id`),
  KEY `fk_secret__environment_id` (`environment_id`),
  KEY `idx_secret_name` (`name`),
  KEY `idx_secret_remove_time` (`remove_time`),
  KEY `idx_secret_removed` (`removed`),
  KEY `idx_secret_state` (`state`),
  CONSTRAINT `fk_secret__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_secret__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_secret__environment_id` FOREIGN KEY (`environment_id`) REFERENCES `stack` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `secret`
--

LOCK TABLES `secret` WRITE;
/*!40000 ALTER TABLE `secret` DISABLE KEYS */;
/*!40000 ALTER TABLE `secret` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service`
--

DROP TABLE IF EXISTS `service`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `service` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `environment_id` bigint(20) DEFAULT NULL,
  `vip` varchar(255) DEFAULT NULL,
  `create_index` bigint(20) DEFAULT NULL,
  `selector` varchar(4096) DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `health_state` varchar(128) DEFAULT NULL,
  `system` bit(1) NOT NULL DEFAULT b'0',
  `previous_revision_id` bigint(20) DEFAULT NULL,
  `revision_id` bigint(20) DEFAULT NULL,
  `revision` bigint(20) NOT NULL DEFAULT '0',
  `cluster_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_service_uuid` (`uuid`),
  KEY `fk_service__account_id` (`account_id`),
  KEY `fk_service__creator_id` (`creator_id`),
  KEY `fk_service__environment_id` (`environment_id`),
  KEY `fk_service__previous_revision_id` (`previous_revision_id`),
  KEY `fk_service__revision_id` (`revision_id`),
  KEY `fk_service_cluster_id` (`cluster_id`),
  KEY `idx_service_external_id` (`external_id`),
  KEY `idx_service_name` (`name`),
  KEY `idx_service_remove_time` (`remove_time`),
  KEY `idx_service_removed` (`removed`),
  KEY `idx_service_state` (`state`),
  CONSTRAINT `fk_service__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_service__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_service__environment_id` FOREIGN KEY (`environment_id`) REFERENCES `stack` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_service__previous_revision_id` FOREIGN KEY (`previous_revision_id`) REFERENCES `revision` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_service__revision_id` FOREIGN KEY (`revision_id`) REFERENCES `revision` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_service_cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service`
--

LOCK TABLES `service` WRITE;
/*!40000 ALTER TABLE `service` DISABLE KEYS */;
/*!40000 ALTER TABLE `service` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_event`
--

DROP TABLE IF EXISTS `service_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `service_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `host_id` bigint(20) DEFAULT NULL,
  `healthcheck_uuid` varchar(255) DEFAULT NULL,
  `instance_id` bigint(20) DEFAULT NULL,
  `reported_health` varchar(255) DEFAULT NULL,
  `external_timestamp` bigint(20) DEFAULT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_service_event_uuid` (`uuid`),
  KEY `fk_service_event__account_id` (`account_id`),
  KEY `fk_service_event__creator_id` (`creator_id`),
  KEY `fk_service_event__host_id` (`host_id`),
  KEY `fk_service_event__instance_id` (`instance_id`),
  KEY `idx_service_event_name` (`name`),
  KEY `idx_service_event_remove_time` (`remove_time`),
  KEY `idx_service_event_removed` (`removed`),
  KEY `idx_service_event_state` (`state`),
  CONSTRAINT `fk_service_event__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_service_event__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_service_event__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_service_event__instance_id` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_event`
--

LOCK TABLES `service_event` WRITE;
/*!40000 ALTER TABLE `service_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_log`
--

DROP TABLE IF EXISTS `service_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `service_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `data` text,
  `end_time` datetime DEFAULT NULL,
  `event_type` varchar(255) DEFAULT NULL,
  `service_id` bigint(20) DEFAULT NULL,
  `instance_id` bigint(20) DEFAULT NULL,
  `transaction_id` varchar(255) DEFAULT NULL,
  `sub_log` bit(1) NOT NULL DEFAULT b'0',
  `level` varchar(255) DEFAULT NULL,
  `deployment_unit_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_service_log__account_id` (`account_id`),
  KEY `fk_service_log__deployment_unit_id` (`deployment_unit_id`),
  KEY `fk_service_log__instance_id` (`instance_id`),
  KEY `fk_service_log__service_id` (`service_id`),
  CONSTRAINT `fk_service_log__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_service_log__deployment_unit_id` FOREIGN KEY (`deployment_unit_id`) REFERENCES `deployment_unit` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_service_log__instance_id` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_service_log__service_id` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_log`
--

LOCK TABLES `service_log` WRITE;
/*!40000 ALTER TABLE `service_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `setting`
--

DROP TABLE IF EXISTS `setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `setting` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `value` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_setting_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `setting`
--

LOCK TABLES `setting` WRITE;
/*!40000 ALTER TABLE `setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stack`
--

DROP TABLE IF EXISTS `stack`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stack` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `external_id` varchar(128) DEFAULT NULL,
  `health_state` varchar(128) DEFAULT NULL,
  `folder` varchar(255) DEFAULT NULL,
  `system` bit(1) NOT NULL DEFAULT b'0',
  `parent_environment_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_environment_uuid` (`uuid`),
  KEY `fk_environment__account_id` (`account_id`),
  KEY `fk_environment_environment_id` (`parent_environment_id`),
  KEY `fk_stack__cluster_id` (`cluster_id`),
  KEY `fk_stack__creator_id` (`creator_id`),
  KEY `idx_environment_external_id` (`external_id`),
  KEY `idx_environment_name` (`name`),
  KEY `idx_environment_remove_time` (`remove_time`),
  KEY `idx_environment_removed` (`removed`),
  KEY `idx_environment_state` (`state`),
  CONSTRAINT `fk_environment__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_environment_environment_id` FOREIGN KEY (`parent_environment_id`) REFERENCES `stack` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_stack__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_stack__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stack`
--

LOCK TABLES `stack` WRITE;
/*!40000 ALTER TABLE `stack` DISABLE KEYS */;
/*!40000 ALTER TABLE `stack` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `storage_driver`
--

DROP TABLE IF EXISTS `storage_driver`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `storage_driver` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `service_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_storage_driver_uuid` (`uuid`),
  KEY `fk_storage_driver__cluster_id` (`cluster_id`),
  KEY `fk_storage_driver__service_id` (`service_id`),
  KEY `idx_storage_driver_name` (`name`),
  KEY `idx_storage_driver_remove_time` (`remove_time`),
  KEY `idx_storage_driver_removed` (`removed`),
  KEY `idx_storage_driver_state` (`state`),
  CONSTRAINT `fk_storage_driver__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_storage_driver__service_id` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `storage_driver`
--

LOCK TABLES `storage_driver` WRITE;
/*!40000 ALTER TABLE `storage_driver` DISABLE KEYS */;
/*!40000 ALTER TABLE `storage_driver` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `storage_pool`
--

DROP TABLE IF EXISTS `storage_pool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `storage_pool` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `physical_total_size_mb` bigint(20) DEFAULT NULL,
  `virtual_total_size_mb` bigint(20) DEFAULT NULL,
  `external` bit(1) NOT NULL DEFAULT b'0',
  `agent_id` bigint(20) DEFAULT NULL,
  `zone_id` bigint(20) DEFAULT NULL,
  `external_id` varchar(128) DEFAULT NULL,
  `driver_name` varchar(255) DEFAULT NULL,
  `volume_access_mode` varchar(255) DEFAULT NULL,
  `storage_driver_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_storage_pool_uuid` (`uuid`),
  KEY `fk_storage_driver__id` (`storage_driver_id`),
  KEY `fk_storage_pool__agent_id` (`agent_id`),
  KEY `fk_storage_pool__cluster_id` (`cluster_id`),
  KEY `fk_storage_pool__zone_id` (`zone_id`),
  KEY `idx_storage_pool_name` (`name`),
  KEY `idx_storage_pool_remove_time` (`remove_time`),
  KEY `idx_storage_pool_removed` (`removed`),
  KEY `idx_storage_pool_state` (`state`),
  CONSTRAINT `fk_storage_driver__id` FOREIGN KEY (`storage_driver_id`) REFERENCES `storage_driver` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_storage_pool__agent_id` FOREIGN KEY (`agent_id`) REFERENCES `agent` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_storage_pool__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `storage_pool`
--

LOCK TABLES `storage_pool` WRITE;
/*!40000 ALTER TABLE `storage_pool` DISABLE KEYS */;
/*!40000 ALTER TABLE `storage_pool` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `storage_pool_host_map`
--

DROP TABLE IF EXISTS `storage_pool_host_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `storage_pool_host_map` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `storage_pool_id` bigint(20) DEFAULT NULL,
  `host_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_storage_pool_host_map_uuid` (`uuid`),
  KEY `fk_storage_pool_host_map__host_id` (`host_id`),
  KEY `fk_storage_pool_host_map__storage_pool_id` (`storage_pool_id`),
  KEY `idx_storage_pool_host_map_name` (`name`),
  KEY `idx_storage_pool_host_map_remove_time` (`remove_time`),
  KEY `idx_storage_pool_host_map_removed` (`removed`),
  KEY `idx_storage_pool_host_map_state` (`state`),
  CONSTRAINT `fk_storage_pool_host_map__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_storage_pool_host_map__storage_pool_id` FOREIGN KEY (`storage_pool_id`) REFERENCES `storage_pool` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `storage_pool_host_map`
--

LOCK TABLES `storage_pool_host_map` WRITE;
/*!40000 ALTER TABLE `storage_pool_host_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `storage_pool_host_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subnet`
--

DROP TABLE IF EXISTS `subnet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subnet` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `network_address` varchar(255) DEFAULT NULL,
  `cidr_size` int(11) DEFAULT NULL,
  `start_address` varchar(255) DEFAULT NULL,
  `end_address` varchar(255) DEFAULT NULL,
  `gateway` varchar(255) DEFAULT NULL,
  `network_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_subnet_uuid` (`uuid`),
  KEY `fk_subnet__cluster_id` (`cluster_id`),
  KEY `fk_subnet__network_id` (`network_id`),
  KEY `idx_subnet_name` (`name`),
  KEY `idx_subnet_remove_time` (`remove_time`),
  KEY `idx_subnet_removed` (`removed`),
  KEY `idx_subnet_state` (`state`),
  CONSTRAINT `fk_subnet__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_subnet__network_id` FOREIGN KEY (`network_id`) REFERENCES `network` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subnet`
--

LOCK TABLES `subnet` WRITE;
/*!40000 ALTER TABLE `subnet` DISABLE KEYS */;
/*!40000 ALTER TABLE `subnet` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ui_challenge`
--

DROP TABLE IF EXISTS `ui_challenge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ui_challenge` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `token` varchar(255) NOT NULL,
  `data` mediumtext,
  `request` varchar(255) NOT NULL,
  `created` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `created_token` (`created`,`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ui_challenge`
--

LOCK TABLES `ui_challenge` WRITE;
/*!40000 ALTER TABLE `ui_challenge` DISABLE KEYS */;
/*!40000 ALTER TABLE `ui_challenge` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_preference`
--

DROP TABLE IF EXISTS `user_preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_preference` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `value` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_preference_uuid` (`uuid`),
  KEY `fk_user_preference__account_id` (`account_id`),
  KEY `idx_user_preference_name` (`name`),
  KEY `idx_user_preference_remove_time` (`remove_time`),
  KEY `idx_user_preference_removed` (`removed`),
  KEY `idx_user_preference_state` (`state`),
  CONSTRAINT `fk_user_preference__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_preference`
--

LOCK TABLES `user_preference` WRITE;
/*!40000 ALTER TABLE `user_preference` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_preference` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `volume`
--

DROP TABLE IF EXISTS `volume`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `volume` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `physical_size_mb` bigint(20) DEFAULT NULL,
  `virtual_size_mb` bigint(20) DEFAULT NULL,
  `uri` varchar(255) DEFAULT NULL,
  `external_id` varchar(128) DEFAULT NULL,
  `access_mode` varchar(255) DEFAULT NULL,
  `host_id` bigint(20) DEFAULT NULL,
  `deployment_unit_id` bigint(20) DEFAULT NULL,
  `environment_id` bigint(20) DEFAULT NULL,
  `volume_template_id` bigint(20) DEFAULT NULL,
  `storage_driver_id` bigint(20) DEFAULT NULL,
  `size_mb` bigint(20) DEFAULT NULL,
  `storage_pool_id` bigint(20) DEFAULT NULL,
  `cluster_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_volume_uuid` (`uuid`),
  KEY `fk_volume__account_id` (`account_id`),
  KEY `fk_volume__cluster_id` (`cluster_id`),
  KEY `fk_volume__creator_id` (`creator_id`),
  KEY `fk_volume__deployment_unit_id` (`deployment_unit_id`),
  KEY `fk_volume__environment_id` (`environment_id`),
  KEY `fk_volume__host_id` (`host_id`),
  KEY `fk_volume__storage_driver_id` (`storage_driver_id`),
  KEY `fk_volume__storage_pool_id` (`storage_pool_id`),
  KEY `fk_volume__volume_template_id` (`volume_template_id`),
  KEY `idx_volume_external_id` (`external_id`),
  KEY `idx_volume_name` (`name`),
  KEY `idx_volume_remove_time` (`remove_time`),
  KEY `idx_volume_removed` (`removed`),
  KEY `idx_volume_state` (`state`),
  KEY `idx_volume_uri` (`uri`),
  CONSTRAINT `fk_volume__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume__deployment_unit_id` FOREIGN KEY (`deployment_unit_id`) REFERENCES `deployment_unit` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume__environment_id` FOREIGN KEY (`environment_id`) REFERENCES `stack` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume__storage_driver_id` FOREIGN KEY (`storage_driver_id`) REFERENCES `storage_driver` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume__storage_pool_id` FOREIGN KEY (`storage_pool_id`) REFERENCES `storage_pool` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume__volume_template_id` FOREIGN KEY (`volume_template_id`) REFERENCES `volume_template` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `volume`
--

LOCK TABLES `volume` WRITE;
/*!40000 ALTER TABLE `volume` DISABLE KEYS */;
/*!40000 ALTER TABLE `volume` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `volume_storage_pool_map`
--

DROP TABLE IF EXISTS `volume_storage_pool_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `volume_storage_pool_map` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` mediumtext,
  `volume_id` bigint(20) DEFAULT NULL,
  `storage_pool_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_volume_storage_pool_map_uuid` (`uuid`),
  KEY `fk_volume_storage_pool_map__storage_pool_id` (`storage_pool_id`),
  KEY `fk_volume_storage_pool_map__volume_id` (`volume_id`),
  KEY `idx_volume_storage_pool_map_name` (`name`),
  KEY `idx_volume_storage_pool_map_remove_time` (`remove_time`),
  KEY `idx_volume_storage_pool_map_removed` (`removed`),
  KEY `idx_volume_storage_pool_map_state` (`state`),
  CONSTRAINT `fk_volume_storage_pool_map__storage_pool_id` FOREIGN KEY (`storage_pool_id`) REFERENCES `storage_pool` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume_storage_pool_map__volume_id` FOREIGN KEY (`volume_id`) REFERENCES `volume` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `volume_storage_pool_map`
--

LOCK TABLES `volume_storage_pool_map` WRITE;
/*!40000 ALTER TABLE `volume_storage_pool_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `volume_storage_pool_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `volume_template`
--

DROP TABLE IF EXISTS `volume_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `volume_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `kind` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  `remove_time` datetime DEFAULT NULL,
  `data` text,
  `driver` varchar(255) DEFAULT NULL,
  `environment_id` bigint(20) DEFAULT NULL,
  `external` bit(1) NOT NULL DEFAULT b'0',
  `per_container` bit(1) NOT NULL DEFAULT b'0',
  `cluster_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_volume_template_uuid` (`uuid`),
  KEY `fk_volume_template__account_id` (`account_id`),
  KEY `fk_volume_template__cluster_id` (`cluster_id`),
  KEY `fk_volume_template__creator_id` (`creator_id`),
  KEY `fk_volume_template__environment_id` (`environment_id`),
  KEY `idx_volume_template_name` (`name`),
  KEY `idx_volume_template_remove_time` (`remove_time`),
  KEY `idx_volume_template_removed` (`removed`),
  KEY `idx_volume_template_state` (`state`),
  CONSTRAINT `fk_volume_template__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume_template__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume_template__creator_id` FOREIGN KEY (`creator_id`) REFERENCES `account` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_volume_template__environment_id` FOREIGN KEY (`environment_id`) REFERENCES `stack` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `volume_template`
--

LOCK TABLES `volume_template` WRITE;
/*!40000 ALTER TABLE `volume_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `volume_template` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2017-08-14 11:01:25
