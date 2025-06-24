-- MySQL dump 10.13  Distrib 8.0.42, for macos15 (x86_64)
--
-- Host: localhost    Database: tcms
-- ------------------------------------------------------
-- Server version	8.0.11

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `case_steps`
--

DROP TABLE IF EXISTS `case_steps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `case_steps` (
  `case_id` int(11) NOT NULL,
  `step_id` int(11) NOT NULL,
  PRIMARY KEY (`case_id`,`step_id`),
  UNIQUE KEY `UK_iku8lj60ve9qq5sslv2vhmblt` (`step_id`),
  CONSTRAINT `FK9dd1w56oenn1fqcltufwibkgb` FOREIGN KEY (`case_id`) REFERENCES `test_case` (`tc_id`),
  CONSTRAINT `FKbogqtjpqtwmh2wqih3cbb1or0` FOREIGN KEY (`step_id`) REFERENCES `test_steps` (`ts_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `case_steps`
--

LOCK TABLES `case_steps` WRITE;
/*!40000 ALTER TABLE `case_steps` DISABLE KEYS */;
INSERT INTO `case_steps` VALUES (1,18),(1,19),(1,20),(1,21),(2,22),(5,30),(6,36),(7,45),(7,46),(8,48);
/*!40000 ALTER TABLE `case_steps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `dep_id` int(11) NOT NULL AUTO_INCREMENT,
  `dep_name` varchar(255) NOT NULL,
  PRIMARY KEY (`dep_id`),
  UNIQUE KEY `UK_h00y249mf2imwi7dagicx0xku` (`dep_name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
INSERT INTO `department` VALUES (1,'QA');
/*!40000 ALTER TABLE `department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project` (
  `pro_id` int(11) NOT NULL AUTO_INCREMENT,
  `pro_initials` varchar(255) NOT NULL,
  `pro_name` varchar(255) NOT NULL,
  PRIMARY KEY (`pro_id`),
  UNIQUE KEY `UK_ftx6v9g0jvrhnrt8sxb6hel45` (`pro_initials`),
  UNIQUE KEY `UK_627g7odihfyq2241ucc2a4ltj` (`pro_name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES (1,'HIGI','Higi');
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_tests`
--

DROP TABLE IF EXISTS `project_tests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_tests` (
  `case_id` int(11) NOT NULL,
  `pro_id` int(11) NOT NULL,
  PRIMARY KEY (`pro_id`,`case_id`),
  UNIQUE KEY `UK_99b85hahkcmry8s5hjhu9ycgr` (`case_id`),
  CONSTRAINT `FK7alel3yj33l2jx9rste8ik27w` FOREIGN KEY (`case_id`) REFERENCES `test_case` (`tc_id`),
  CONSTRAINT `FK9i590h2o2kttuwaloxn6mo17s` FOREIGN KEY (`pro_id`) REFERENCES `project` (`pro_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_tests`
--

LOCK TABLES `project_tests` WRITE;
/*!40000 ALTER TABLE `project_tests` DISABLE KEYS */;
INSERT INTO `project_tests` VALUES (1,1),(2,1),(3,1),(4,1),(5,1),(6,1),(7,1),(8,1);
/*!40000 ALTER TABLE `project_tests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `refresh_token`
--

DROP TABLE IF EXISTS `refresh_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_token` (
  `ref_id` int(11) NOT NULL AUTO_INCREMENT,
  `expiry_date` datetime(6) DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ref_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refresh_token`
--

LOCK TABLES `refresh_token` WRITE;
/*!40000 ALTER TABLE `refresh_token` DISABLE KEYS */;
INSERT INTO `refresh_token` VALUES (1,'2025-06-25 07:03:29.359689','bf3f414e-2b25-47a5-8e78-5337bfdeb440','bdahal');
/*!40000 ALTER TABLE `refresh_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `role_id` int(11) NOT NULL AUTO_INCREMENT,
  `role_name` varchar(255) NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `UK_iubw515ff0ugtm28p8g3myt0h` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (2,'ROLE_QAEngineer'),(1,'ROLE_QAManager');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tags`
--

DROP TABLE IF EXISTS `tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tags` (
  `tag_id` int(11) NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(255) NOT NULL,
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `UK_2c6s9hekidseaj5vbgb3pgy3k` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tags`
--

LOCK TABLES `tags` WRITE;
/*!40000 ALTER TABLE `tags` DISABLE KEYS */;
/*!40000 ALTER TABLE `tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_case`
--

DROP TABLE IF EXISTS `test_case`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_case` (
  `tc_id` int(11) NOT NULL AUTO_INCREMENT,
  `tc_created_by` varchar(255) NOT NULL,
  `tc_created_date` datetime(6) NOT NULL,
  `tc_modified_by` varchar(255) NOT NULL,
  `tc_modified_date` datetime(6) NOT NULL,
  `tc_name` varchar(255) NOT NULL,
  `tc_proj_id` varchar(255) NOT NULL,
  PRIMARY KEY (`tc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_case`
--

LOCK TABLES `test_case` WRITE;
/*!40000 ALTER TABLE `test_case` DISABLE KEYS */;
INSERT INTO `test_case` VALUES (1,'Bibek Dahal','2025-06-23 12:56:34.000000','Bibek Dahal','2025-06-24 11:47:48.000000','Verify that user is able to login using correct username and password','HIGI-1'),(2,'Bibek Dahal','2025-06-24 11:13:34.000000','Bibek Dahal','2025-06-24 11:13:57.000000','Verify that user is not loggedin using wrong username and password','HIGI-2'),(3,'Bibek Dahal','2025-06-24 11:16:51.000000','Bibek Dahal','2025-06-24 11:16:51.000000','Verify login with all caps','HIGI-3'),(4,'Bibek Dahal','2025-06-24 11:21:14.000000','Bibek Dahal','2025-06-24 11:21:14.000000','Verify login with random value','HIGI-4'),(5,'Bibek Dahal','2025-06-24 11:21:44.000000','Bibek Dahal','2025-06-24 11:56:48.000000','Verify login with international id','HIGI-5'),(6,'Bibek Dahal','2025-06-24 11:50:54.000000','Bibek Dahal','2025-06-24 12:57:42.000000','Not a test','HIGI-6'),(7,'Bibek Dahal','2025-06-24 12:30:49.000000','Bibek Dahal','2025-06-24 12:57:33.000000','Verify this test','HIGI-7'),(8,'Bibek Dahal','2025-06-24 12:56:35.000000','Bibek Dahal','2025-06-24 18:06:53.000000','Verify api request from activity progress','HIGI-8');
/*!40000 ALTER TABLE `test_case` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_case_tags`
--

DROP TABLE IF EXISTS `test_case_tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_case_tags` (
  `case_id` int(11) NOT NULL,
  `tag_case_id` int(11) NOT NULL,
  PRIMARY KEY (`tag_case_id`,`case_id`),
  KEY `FKe07942wref9gwl31plbuoi7i1` (`case_id`),
  CONSTRAINT `FKe07942wref9gwl31plbuoi7i1` FOREIGN KEY (`case_id`) REFERENCES `test_case` (`tc_id`),
  CONSTRAINT `FKe1fevdicod48b4782pbsysat8` FOREIGN KEY (`tag_case_id`) REFERENCES `tags` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_case_tags`
--

LOCK TABLES `test_case_tags` WRITE;
/*!40000 ALTER TABLE `test_case_tags` DISABLE KEYS */;
/*!40000 ALTER TABLE `test_case_tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_steps`
--

DROP TABLE IF EXISTS `test_steps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_steps` (
  `ts_id` int(11) NOT NULL AUTO_INCREMENT,
  `ts_expected_output` varchar(255) NOT NULL,
  `ts_remarks` varchar(255) DEFAULT NULL,
  `ts_test_data` varchar(255) DEFAULT NULL,
  `ts_step_desc` varchar(255) NOT NULL,
  `ts_order` int(11) NOT NULL,
  `ts_created_by` varchar(255) NOT NULL,
  `ts_created_date` datetime(6) NOT NULL,
  `ts_modified_by` varchar(255) NOT NULL,
  `ts_modified_date` datetime(6) NOT NULL,
  PRIMARY KEY (`ts_id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_steps`
--

LOCK TABLES `test_steps` WRITE;
/*!40000 ALTER TABLE `test_steps` DISABLE KEYS */;
INSERT INTO `test_steps` VALUES (18,'Verify that user is able to login with correct username and password 3',NULL,NULL,'action',1,'Bibek Dahal','2025-06-24 11:10:15.000000','Bibek Dahal','2025-06-24 11:10:15.000000'),(19,'Verify that user is able to login with correct username and password 3',NULL,NULL,'action',2,'Bibek Dahal','2025-06-24 11:10:15.000000','Bibek Dahal','2025-06-24 11:10:15.000000'),(20,'Verify that user is able to login with correct username and password 3',NULL,NULL,'action',3,'Bibek Dahal','2025-06-24 11:10:15.000000','Bibek Dahal','2025-06-24 11:10:15.000000'),(21,'step','step',NULL,'step',4,'Bibek Dahal','2025-06-24 11:10:15.000000','Bibek Dahal','2025-06-24 11:10:15.000000'),(22,'hello','hello',NULL,'Hello',1,'Bibek Dahal','2025-06-24 11:13:57.000000','Bibek Dahal','2025-06-24 11:13:57.000000'),(30,'yoyo',NULL,NULL,'yo',1,'Bibek Dahal','2025-06-24 11:56:48.000000','Bibek Dahal','2025-06-24 11:56:48.000000'),(36,'ddd','data','data','dddd',3,'Bibek Dahal','2025-06-24 12:15:37.000000','Bibek Dahal','2025-06-24 12:57:43.000000'),(45,'sadfas','dafd','dafd','sdfa',9,'Bibek Dahal','2025-06-24 12:43:06.000000','Bibek Dahal','2025-06-24 12:57:34.000000'),(46,'sfdgsdf','dsata','','dgdsfgsd',10,'Bibek Dahal','2025-06-24 12:43:06.000000','Bibek Dahal','2025-06-24 12:57:34.000000'),(48,'yoyo',NULL,'yo yo ','yo yo ',2,'Bibek Dahal','2025-06-24 12:56:55.000000','Bibek Dahal','2025-06-24 18:06:54.000000');
/*!40000 ALTER TABLE `test_steps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `u_id` int(11) NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) NOT NULL,
  `u_is_active` bit(1) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `u_password` varchar(255) NOT NULL,
  `u_name` varchar(255) NOT NULL,
  PRIMARY KEY (`u_id`),
  UNIQUE KEY `UK_ne67ypm5gdgewqyys8903yr4i` (`u_name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Bibek',_binary '','Dahal','$2a$12$wb3w7gVD6VfVWIfTx8BKQOlEAEHLIvAoMG6OipBr0oRbcf08AnJsK','bdahal');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users_department`
--

DROP TABLE IF EXISTS `users_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_department` (
  `user_id` int(11) NOT NULL,
  `dep_id` int(11) NOT NULL,
  PRIMARY KEY (`dep_id`,`user_id`),
  UNIQUE KEY `UK_ga6l8u5gggxuhiblsdtuoyyww` (`user_id`),
  CONSTRAINT `FK56jjjh5dh6dghtmcr4vfcb34g` FOREIGN KEY (`dep_id`) REFERENCES `department` (`dep_id`),
  CONSTRAINT `FK8tlbnn1iybfk41x4iluj9ywx7` FOREIGN KEY (`user_id`) REFERENCES `users` (`u_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users_department`
--

LOCK TABLES `users_department` WRITE;
/*!40000 ALTER TABLE `users_department` DISABLE KEYS */;
/*!40000 ALTER TABLE `users_department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users_projects`
--

DROP TABLE IF EXISTS `users_projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_projects` (
  `user_id` int(11) NOT NULL,
  `pro_id` int(11) NOT NULL,
  PRIMARY KEY (`pro_id`,`user_id`),
  KEY `FKen924y69h6d6chaojjgqfaow8` (`user_id`),
  CONSTRAINT `FKen924y69h6d6chaojjgqfaow8` FOREIGN KEY (`user_id`) REFERENCES `users` (`u_id`),
  CONSTRAINT `FKf1loykyevwhfd53jt92ih8211` FOREIGN KEY (`pro_id`) REFERENCES `project` (`pro_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users_projects`
--

LOCK TABLES `users_projects` WRITE;
/*!40000 ALTER TABLE `users_projects` DISABLE KEYS */;
/*!40000 ALTER TABLE `users_projects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users_roles`
--

DROP TABLE IF EXISTS `users_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_roles` (
  `user_id` int(11) NOT NULL,
  `role_id` int(11) NOT NULL,
  PRIMARY KEY (`role_id`,`user_id`),
  KEY `FK2o0jvgh89lemvvo17cbqvdxaa` (`user_id`),
  CONSTRAINT `FK2o0jvgh89lemvvo17cbqvdxaa` FOREIGN KEY (`user_id`) REFERENCES `users` (`u_id`),
  CONSTRAINT `FKt4v0rrweyk393bdgt107vdx0x` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users_roles`
--

LOCK TABLES `users_roles` WRITE;
/*!40000 ALTER TABLE `users_roles` DISABLE KEYS */;
INSERT INTO `users_roles` VALUES (1,1);
/*!40000 ALTER TABLE `users_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'tcms'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-06-24 18:11:22
