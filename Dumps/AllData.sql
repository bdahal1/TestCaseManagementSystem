-- MySQL dump 10.13  Distrib 8.0.30, for macos12 (x86_64)
--
-- Host: localhost    Database: tcms
-- ------------------------------------------------------
-- Server version	8.0.23

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
-- Dumping data for table `case_steps`
--

LOCK TABLES `case_steps` WRITE;
/*!40000 ALTER TABLE `case_steps` DISABLE KEYS */;
/*!40000 ALTER TABLE `case_steps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
INSERT INTO `department` VALUES (1,'QA');
/*!40000 ALTER TABLE `department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES (1,'Higi');
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `project_tests`
--

LOCK TABLES `project_tests` WRITE;
/*!40000 ALTER TABLE `project_tests` DISABLE KEYS */;
INSERT INTO `project_tests` VALUES (1,1),(2,1),(4,1),(5,1),(7,1),(8,1);
/*!40000 ALTER TABLE `project_tests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `refresh_token`
--

LOCK TABLES `refresh_token` WRITE;
/*!40000 ALTER TABLE `refresh_token` DISABLE KEYS */;
INSERT INTO `refresh_token` VALUES (3,'2022-12-28 04:35:40.917530','5a50b4f2-f92d-4687-a71e-03e9fb968679','bdahal');
/*!40000 ALTER TABLE `refresh_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (2,'ROLE_QAEngineer'),(1,'ROLE_QAManager');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `test_case`
--

LOCK TABLES `test_case` WRITE;
/*!40000 ALTER TABLE `test_case` DISABLE KEYS */;
INSERT INTO `test_case` VALUES (1,'Bibek Dahal','2022-01-01 14:00:45.000000','Ramesh Khatri','2022-01-02 14:00:45.000000','Verify that user is able to login to given application'),(2,'Bibek Dahal','2022-01-01 14:00:45.000000','Bibek Dahal','2022-12-23 13:00:34.000000','Verify new testcase'),(4,'Bibek Dahal','2022-12-23 12:37:54.000000','Bibek Dahal','2022-12-23 13:01:21.000000','Verify that user should be able to get to dashboard and perform certain task'),(5,'Bibek Dahal','2022-12-23 12:38:02.000000','Bibek Dahal','2022-12-23 12:38:02.000000','Verify new testcase'),(7,'Bibek Dahal','2022-12-26 10:20:59.000000','Bibek Dahal','2022-12-26 10:21:22.000000','Verify that user should be able to get to Hellopage and perform certain task'),(8,'Bibek Dahal','2022-12-26 10:28:58.000000','Bibek Dahal','2022-12-26 10:29:13.000000','Verify that user should be able to get to Random page and perform certain task');
/*!40000 ALTER TABLE `test_case` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `test_steps`
--

LOCK TABLES `test_steps` WRITE;
/*!40000 ALTER TABLE `test_steps` DISABLE KEYS */;
/*!40000 ALTER TABLE `test_steps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Bibek','Dahal','$2a$12$p6yKh9nzxeU7i5tdKEC.EuL1EqS886mtPbmBPtQqH50YRhY7cqJES','bdahal',_binary ''),(2,'Sugat','Paneru','$2a$12$V..aQ2IjTpLBdBf13Lu5ieyizpMgCfxZY8xNjQzQWRKLmT35AVxQi','spaneru',_binary ''),(3,'Ramesh','Khatri','$2a$10$EMsSkcy5p4LqAyjgWenyjuFZph0FGM27L8ehbIR0CtI.wfLctgsl2','rkhatri',_binary ''),(4,'Kritartha','Acharya','$2a$10$YjustHNhc6cbyyawZITY8eYmKRP34ocnfoiKk0.Q5KrJNV6iabI06','kacharya',_binary '\0');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users_department`
--

LOCK TABLES `users_department` WRITE;
/*!40000 ALTER TABLE `users_department` DISABLE KEYS */;
INSERT INTO `users_department` VALUES (1,1),(2,1),(3,1),(4,1);
/*!40000 ALTER TABLE `users_department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users_projects`
--

LOCK TABLES `users_projects` WRITE;
/*!40000 ALTER TABLE `users_projects` DISABLE KEYS */;
INSERT INTO `users_projects` VALUES (1,1),(2,1),(3,1),(4,1);
/*!40000 ALTER TABLE `users_projects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users_roles`
--

LOCK TABLES `users_roles` WRITE;
/*!40000 ALTER TABLE `users_roles` DISABLE KEYS */;
INSERT INTO `users_roles` VALUES (1,1),(2,2),(3,2),(4,2);
/*!40000 ALTER TABLE `users_roles` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-12-26 12:11:45
