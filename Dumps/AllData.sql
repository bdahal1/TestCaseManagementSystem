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
INSERT INTO `case_steps` VALUES (1,18),(1,19),(1,20),(1,21),(2,22),(5,30),(6,36),(7,45),(7,46),(8,48),(12,52),(12,53),(13,56),(14,58),(18,60),(21,63),(23,65),(25,66),(25,67),(24,68),(25,69),(16,70),(26,71),(26,72),(19,73),(20,74),(20,75),(21,76),(22,77),(22,78),(24,79),(27,80),(2,81),(28,82);
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
INSERT INTO `department` VALUES (5,'Designer'),(2,'Development'),(3,'Finance'),(4,'Project Management'),(1,'QA');
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
  `issue_counter` int(11) NOT NULL,
  PRIMARY KEY (`pro_id`),
  UNIQUE KEY `UK_ftx6v9g0jvrhnrt8sxb6hel45` (`pro_initials`),
  UNIQUE KEY `UK_627g7odihfyq2241ucc2a4ltj` (`pro_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES (1,'HIGI','Higi',16),(2,'VAL','Valenz',5),(3,'CAL','Clarify',1),(4,'CCL','Clean Claim',8);
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
INSERT INTO `project_tests` VALUES (1,1),(2,1),(3,1),(4,1),(5,1),(6,1),(7,1),(8,1),(12,2),(13,1),(14,1),(16,3),(17,2),(18,4),(19,4),(20,4),(21,4),(22,4),(23,2),(24,2),(25,4),(26,1),(27,1),(28,1),(29,1),(30,1),(31,1),(33,4);
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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refresh_token`
--

LOCK TABLES `refresh_token` WRITE;
/*!40000 ALTER TABLE `refresh_token` DISABLE KEYS */;
INSERT INTO `refresh_token` VALUES (8,'2025-07-10 07:02:47.580207','8be80362-9f0d-4db9-818b-428d91aebd91','spandey'),(9,'2025-07-11 06:38:10.083920','341e7d78-3a12-4c0c-8822-daac46c7ff62','bdahal'),(10,'2025-07-12 03:55:04.615296','53da22d9-9f2c-4f0b-b82a-046054a7d64e','rkhatri');
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (3,'ROLE_ProjectManager'),(2,'ROLE_QAEngineer'),(1,'ROLE_QAManager');
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
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tags`
--

LOCK TABLES `tags` WRITE;
/*!40000 ALTER TABLE `tags` DISABLE KEYS */;
INSERT INTO `tags` VALUES (60,'login'),(59,'regression'),(58,'smoke');
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
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_case`
--

LOCK TABLES `test_case` WRITE;
/*!40000 ALTER TABLE `test_case` DISABLE KEYS */;
INSERT INTO `test_case` VALUES (1,'Bibek Dahal','2025-06-23 12:56:34.000000','Bibek Dahal','2025-07-10 11:27:53.000000','Verify that user is able to login using correct username and password','HIGI-1'),(2,'Bibek Dahal','2025-06-24 11:13:34.000000','Bibek Dahal','2025-07-10 11:28:08.000000','Verify that user is not loggedin using wrong username and password','HIGI-2'),(3,'Bibek Dahal','2025-06-24 11:16:51.000000','Bibek Dahal','2025-06-26 12:10:40.000000','Verify login with all caps','HIGI-3'),(4,'Bibek Dahal','2025-06-24 11:21:14.000000','Bibek Dahal','2025-06-26 12:05:17.000000','Verify login with random value','HIGI-4'),(5,'Bibek Dahal','2025-06-24 11:21:44.000000','Bibek Dahal','2025-06-26 12:07:29.000000','Verify login with international id','HIGI-5'),(6,'Bibek Dahal','2025-06-24 11:50:54.000000','Bibek Dahal','2025-06-26 12:46:09.000000','Not a test','HIGI-6'),(7,'Bibek Dahal','2025-06-24 12:30:49.000000','Bibek Dahal','2025-06-24 12:57:33.000000','Verify this test','HIGI-7'),(8,'Bibek Dahal','2025-06-24 12:56:35.000000','Bibek Dahal','2025-06-24 18:06:53.000000','Verify api request from activity progress','HIGI-8'),(12,'Bibek Dahal','2025-06-25 14:32:33.000000','Bibek Dahal','2025-06-25 15:04:21.000000','Verify that user is able to login with correct username and password for valenz','VAL-1'),(13,'Bibek Dahal','2025-06-25 15:05:00.000000','Bibek Dahal','2025-07-08 14:57:38.000000','Verify this is fixed test case in the fixed position','HIGI-9'),(14,'Bibek Dahal','2025-06-25 15:14:14.000000','Bibek Dahal','2025-06-25 15:20:41.000000','New Higi step check','HIGI-10'),(16,'Bibek Dahal','2025-06-25 15:16:30.000000','Bibek Dahal','2025-06-30 12:35:32.000000','Hello Clarify this is my test','CAL-1'),(17,'Bibek Dahal','2025-06-25 15:23:52.000000','Bibek Dahal','2025-06-25 15:23:52.000000','Verify that user is able to login with correct username and password 3','VAL-2'),(18,'Bibek Dahal','2025-06-25 15:40:27.000000','Bibek Dahal','2025-06-25 15:40:27.000000','Verify login with international id 1234','CCL-1'),(19,'Bibek Dahal','2025-06-25 15:44:29.000000','Swastik Pandey','2025-07-08 12:57:13.000000','Verify my test case','CCL-2'),(20,'Bibek Dahal','2025-06-25 15:47:01.000000','Swastik Pandey','2025-07-08 12:57:47.000000','Verify fixing random test case','CCL-3'),(21,'Bibek Dahal','2025-06-25 15:49:10.000000','Swastik Pandey','2025-07-08 12:58:04.000000','Verify jpt test','CCL-4'),(22,'Bibek Dahal','2025-06-25 15:53:23.000000','Swastik Pandey','2025-07-08 12:58:28.000000','Random test value','CCL-5'),(23,'Bibek Dahal','2025-06-25 15:56:12.000000','Bibek Dahal','2025-07-08 15:43:07.000000','Verify that user should be able to something from somewhere','VAL-3'),(24,'Bibek Dahal','2025-06-25 15:57:47.000000','Bibek Dahal','2025-07-08 15:43:56.000000','Verify that this is a test of a tester in a testing site','VAL-4'),(25,'Bibek Dahal','2025-06-25 16:06:05.000000','Bibek Dahal','2025-06-26 12:48:31.000000','Clean claim hello ','CCL-6'),(26,'Bibek Dahal','2025-07-08 12:39:49.000000','Bibek Dahal','2025-07-08 12:39:49.000000','Verify one perfect Test','HIGI-11'),(27,'Bibek Dahal','2025-07-09 14:55:53.000000','Bibek Dahal','2025-07-09 14:55:53.000000','Verify latest test case to add so that we can check everything','HIGI-12'),(28,'Bibek Dahal','2025-07-10 13:41:25.000000','Bibek Dahal','2025-07-10 14:06:18.000000','Test','HIGI-13'),(29,'Bibek Dahal','2025-07-10 14:01:15.000000','Bibek Dahal','2025-07-10 14:23:41.000000','sdfdsf','HIGI-14'),(30,'Bibek Dahal','2025-07-10 14:23:54.000000','Bibek Dahal','2025-07-10 14:23:54.000000','dfsafsdfsd','HIGI-15'),(31,'Bibek Dahal','2025-07-10 14:24:04.000000','Bibek Dahal','2025-07-10 14:24:04.000000','uhjgfrthjhgt','HIGI-16'),(33,'Bibek Dahal','2025-07-10 14:29:22.000000','Bibek Dahal','2025-07-10 14:29:22.000000','Verify that user is able to login with correct username and password ccl','CCL-8');
/*!40000 ALTER TABLE `test_case` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_case_executions`
--

DROP TABLE IF EXISTS `test_case_executions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_case_executions` (
  `tce_id` int(11) NOT NULL AUTO_INCREMENT,
  `case_id` int(11) NOT NULL,
  `test_execution_id` int(11) NOT NULL,
  `result_comment` varchar(255) DEFAULT NULL,
  `result_status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`tce_id`),
  KEY `FKn9xb9rs5nd3x5hl34top0oq3n` (`case_id`),
  KEY `FKixctwxo3w2ph5oifc1fqn1lad` (`test_execution_id`),
  CONSTRAINT `FKixctwxo3w2ph5oifc1fqn1lad` FOREIGN KEY (`test_execution_id`) REFERENCES `test_executions` (`execution_id`),
  CONSTRAINT `FKn9xb9rs5nd3x5hl34top0oq3n` FOREIGN KEY (`case_id`) REFERENCES `test_case` (`tc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_case_executions`
--

LOCK TABLES `test_case_executions` WRITE;
/*!40000 ALTER TABLE `test_case_executions` DISABLE KEYS */;
INSERT INTO `test_case_executions` VALUES (2,26,1,NULL,NULL),(3,3,1,'','FAIL'),(4,4,1,NULL,NULL),(5,5,1,'','SKIPPED'),(9,13,1,NULL,NULL),(10,1,1,'hello dasfd sadff sdfsdf','FAIL'),(11,2,1,'my comment','PASS'),(12,27,1,'','PASS'),(15,4,2,NULL,NULL),(16,3,2,NULL,NULL),(17,2,2,NULL,NULL);
/*!40000 ALTER TABLE `test_case_executions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_case_folders`
--

DROP TABLE IF EXISTS `test_case_folders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_case_folders` (
  `test_folder_id` int(11) NOT NULL,
  `case_id` int(11) NOT NULL,
  PRIMARY KEY (`test_folder_id`,`case_id`),
  KEY `FKa3av9lyb4mvd1nbunpio5vab2` (`case_id`),
  CONSTRAINT `FK90l1qn12qc21umxaj3c7vxasn` FOREIGN KEY (`test_folder_id`) REFERENCES `test_folders` (`folder_id`),
  CONSTRAINT `FKa3av9lyb4mvd1nbunpio5vab2` FOREIGN KEY (`case_id`) REFERENCES `test_case` (`tc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_case_folders`
--

LOCK TABLES `test_case_folders` WRITE;
/*!40000 ALTER TABLE `test_case_folders` DISABLE KEYS */;
INSERT INTO `test_case_folders` VALUES (2,1),(1,2),(1,3),(2,4),(4,5),(2,6),(1,7),(2,8),(9,12),(4,13),(2,14),(8,17),(7,18),(7,19),(7,20),(7,22),(8,23),(8,24),(7,25),(1,26),(1,27);
/*!40000 ALTER TABLE `test_case_folders` ENABLE KEYS */;
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
INSERT INTO `test_case_tags` VALUES (1,58),(1,59),(1,60),(2,58),(13,59),(20,58),(20,59),(22,59),(23,58),(26,58),(27,59),(28,60),(29,60),(30,59),(31,58),(33,58);
/*!40000 ALTER TABLE `test_case_tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_executions`
--

DROP TABLE IF EXISTS `test_executions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_executions` (
  `execution_id` int(11) NOT NULL AUTO_INCREMENT,
  `execution_name` varchar(255) NOT NULL,
  `project_id` int(11) NOT NULL,
  `execution_status` varchar(255) NOT NULL,
  PRIMARY KEY (`execution_id`),
  UNIQUE KEY `UKrsf9hdwleanoknsikajd0edrb` (`execution_name`,`project_id`),
  KEY `FK87yuid19ngqt7y7ktdanrylqm` (`project_id`),
  CONSTRAINT `FK87yuid19ngqt7y7ktdanrylqm` FOREIGN KEY (`project_id`) REFERENCES `project` (`pro_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_executions`
--

LOCK TABLES `test_executions` WRITE;
/*!40000 ALTER TABLE `test_executions` DISABLE KEYS */;
INSERT INTO `test_executions` VALUES (1,'Execution1',1,'IN_PROGRESS'),(2,'Execution2',1,'IN_PROGRESS'),(3,'Execution3',1,'IN_PROGRESS');
/*!40000 ALTER TABLE `test_executions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_folders`
--

DROP TABLE IF EXISTS `test_folders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_folders` (
  `folder_id` int(11) NOT NULL AUTO_INCREMENT,
  `folder_name` varchar(255) NOT NULL,
  `project_id` int(11) NOT NULL,
  PRIMARY KEY (`folder_id`),
  UNIQUE KEY `UK_b5a0dob7srxdi2sapigah9gl` (`folder_name`),
  UNIQUE KEY `UKt6esetnqfcvabt7c1wss81l3w` (`folder_name`,`project_id`),
  KEY `FKl37c3oad93cpxk7xxo6oau755` (`project_id`),
  CONSTRAINT `FKl37c3oad93cpxk7xxo6oau755` FOREIGN KEY (`project_id`) REFERENCES `project` (`pro_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_folders`
--

LOCK TABLES `test_folders` WRITE;
/*!40000 ALTER TABLE `test_folders` DISABLE KEYS */;
INSERT INTO `test_folders` VALUES (4,'Contact us',1),(2,'DashboardHigi',1),(9,'DashboardVal',2),(1,'LoginHigi',1),(7,'LoginSuite CCL',4),(8,'LoginVal',2);
/*!40000 ALTER TABLE `test_folders` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_steps`
--

LOCK TABLES `test_steps` WRITE;
/*!40000 ALTER TABLE `test_steps` DISABLE KEYS */;
INSERT INTO `test_steps` VALUES (18,'hsdfjahajaks','','sdfa','action',1,'Bibek Dahal','2025-06-24 11:10:15.000000','Bibek Dahal','2025-07-10 11:27:53.000000'),(19,'sdfdsf','','sdf','action',2,'Bibek Dahal','2025-06-24 11:10:15.000000','Bibek Dahal','2025-07-10 11:27:53.000000'),(20,'sdfds','','dfd','action',3,'Bibek Dahal','2025-06-24 11:10:15.000000','Bibek Dahal','2025-07-10 11:27:53.000000'),(21,'sdfasdf','','step','step',4,'Bibek Dahal','2025-06-24 11:10:15.000000','Bibek Dahal','2025-07-10 11:27:53.000000'),(22,'hello','','data1','Hello',1,'Bibek Dahal','2025-06-24 11:13:57.000000','Bibek Dahal','2025-07-10 11:28:08.000000'),(30,'yoyo','','','yo',1,'Bibek Dahal','2025-06-24 11:56:48.000000','Bibek Dahal','2025-06-26 12:07:29.000000'),(36,'ddd','','data','dddd',3,'Bibek Dahal','2025-06-24 12:15:37.000000','Bibek Dahal','2025-06-26 12:46:10.000000'),(45,'sadfas','','dafd','sdfa',9,'Bibek Dahal','2025-06-24 12:43:06.000000','Bibek Dahal','2025-06-24 12:57:34.000000'),(46,'sfdgsdf','','','dgdsfgsd',10,'Bibek Dahal','2025-06-24 12:43:06.000000','Bibek Dahal','2025-06-24 12:57:34.000000'),(48,'yoyo','','yo yo ','yo yo ',2,'Bibek Dahal','2025-06-24 12:56:55.000000','Bibek Dahal','2025-06-24 18:06:54.000000'),(52,'wewewe','','wewew','weweww',4,'Bibek Dahal','2025-06-25 15:05:00.000000','Bibek Dahal','2025-06-25 15:05:00.000000'),(53,'wer','','sddf','dfsdf',3,'Bibek Dahal','2025-06-25 15:05:01.000000','Bibek Dahal','2025-06-25 15:05:01.000000'),(56,'erterfg',NULL,'etret','yutrty',3,'Bibek Dahal','2025-06-25 15:15:20.000000','Bibek Dahal','2025-07-08 14:57:39.000000'),(58,'action',NULL,'data 1 action','Val step1',1,'Bibek Dahal','2025-06-25 15:23:53.000000','Bibek Dahal','2025-06-25 15:23:53.000000'),(60,'asdjkhfniasdbfiao',NULL,'asdjkhfiusabd fibasd','sl;wefjilsdjfoisdj oin',1,'Bibek Dahal','2025-06-25 15:44:30.000000','Bibek Dahal','2025-06-25 15:44:30.000000'),(63,'asd',NULL,'asd','dddd',1,'Bibek Dahal','2025-06-25 15:55:05.000000','Swastik Pandey','2025-07-08 12:58:04.000000'),(64,'sadfvas hello',NULL,'sdfas','sdfasdfa',7,'Bibek Dahal','2025-06-25 15:57:32.000000','Bibek Dahal','2025-06-25 15:57:32.000000'),(65,'User should be able to something',NULL,'my data','This is my step',1,'Bibek Dahal','2025-06-25 16:05:27.000000','Bibek Dahal','2025-07-08 15:43:08.000000'),(66,'data',NULL,'claim','hello clean',1,'Bibek Dahal','2025-06-25 16:06:05.000000','Bibek Dahal','2025-06-26 12:48:32.000000'),(67,'sadfa',NULL,'dsfas','new claim data',2,'Bibek Dahal','2025-06-25 16:06:23.000000','Bibek Dahal','2025-06-26 12:48:32.000000'),(68,'User should be able to so many things',NULL,'data','my step',1,'Bibek Dahal','2025-06-25 16:06:34.000000','Bibek Dahal','2025-07-08 15:43:56.000000'),(69,'ijiojoij',NULL,'iojoijoi','kojiokjio',3,'Bibek Dahal','2025-06-26 12:48:32.000000','Bibek Dahal','2025-06-26 12:48:32.000000'),(70,'test',NULL,'test','my test',1,'Bibek Dahal','2025-06-30 12:35:32.000000','Bibek Dahal','2025-06-30 12:35:32.000000'),(71,'output',NULL,'data','my step',1,'Bibek Dahal','2025-07-08 12:39:50.000000','Bibek Dahal','2025-07-08 12:39:50.000000'),(72,'output2',NULL,'data2','step 2',2,'Bibek Dahal','2025-07-08 12:39:50.000000','Bibek Dahal','2025-07-08 12:39:50.000000'),(73,'asdf',NULL,'asd','asd',1,'Swastik Pandey','2025-07-08 12:57:13.000000','Swastik Pandey','2025-07-08 12:57:13.000000'),(74,'asdf',NULL,'sdf','gdffs',1,'Swastik Pandey','2025-07-08 12:57:47.000000','Swastik Pandey','2025-07-08 12:57:47.000000'),(75,'asdfasf',NULL,'dsfas','sadfafd',2,'Swastik Pandey','2025-07-08 12:57:47.000000','Swastik Pandey','2025-07-08 12:57:47.000000'),(76,'asdfa',NULL,'asdfa','dfasfda',2,'Swastik Pandey','2025-07-08 12:58:04.000000','Swastik Pandey','2025-07-08 12:58:04.000000'),(77,'ertgret',NULL,'asdffgf','asdf',1,'Swastik Pandey','2025-07-08 12:58:28.000000','Swastik Pandey','2025-07-08 12:58:28.000000'),(78,'sdgfds',NULL,'werewre','trterwfd',2,'Swastik Pandey','2025-07-08 12:58:28.000000','Swastik Pandey','2025-07-08 12:58:28.000000'),(79,'Output should be available',NULL,'new data','My other step',2,'Bibek Dahal','2025-07-08 15:43:56.000000','Bibek Dahal','2025-07-08 15:43:56.000000'),(80,'sdfkajskl',NULL,'sdfgsd','my step',1,'Bibek Dahal','2025-07-09 14:55:53.000000','Bibek Dahal','2025-07-09 14:55:53.000000'),(81,'sdfds',NULL,'asdfa','asdfa',2,'Bibek Dahal','2025-07-10 11:28:08.000000','Bibek Dahal','2025-07-10 11:28:08.000000'),(82,'bfdgf',NULL,'3erwr','fdfgsd',1,'Bibek Dahal','2025-07-10 13:41:39.000000','Bibek Dahal','2025-07-10 14:06:18.000000');
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Bibek',_binary '','Dahal','$2a$12$wb3w7gVD6VfVWIfTx8BKQOlEAEHLIvAoMG6OipBr0oRbcf08AnJsK','bdahal'),(2,'Ramesh',_binary '','Khatri','$2a$10$sl2AMdU/xI90eXpThI8r9e.UGdnZgXLqgim6bnGlKGg4fXOj2Lp8e','rkhatri'),(3,'Swastik',_binary '','Pandey','$2a$10$SrqLwNSLIVnQr/I0KYrpQurWcC8.u1ePCq70ISNgYYrVYF0rz4MLC','spandey'),(5,'Binaya',_binary '','Dahal','$2a$10$dDoJU7wJ0LJaBdKAoZWdT.GTIQs3Lu0nSGjzPmhcAKCaE4QwIlapy','bidahal');
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
INSERT INTO `users_department` VALUES (1,1),(2,1),(3,1),(5,2);
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
INSERT INTO `users_projects` VALUES (1,1),(1,2),(1,3),(1,4),(2,1),(2,2),(3,4),(5,1),(5,2),(5,3),(5,4);
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
INSERT INTO `users_roles` VALUES (1,1),(2,2),(3,3),(5,1);
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

-- Dump completed on 2025-07-10 14:41:16
