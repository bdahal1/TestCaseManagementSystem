-- MySQL dump 10.13  Distrib 8.0.30, for macos12 (x86_64)
--
-- Host: localhost    Database: tcms
-- ------------------------------------------------------
-- Server version	8.0.23

LOCK TABLES `department` WRITE;

INSERT INTO `department`(dep_id,dep_name) VALUES (1,'QA');

UNLOCK TABLES;

LOCK TABLES `project` WRITE;

INSERT INTO `project`(pro_id,pro_name) VALUES (1,'Higi');

UNLOCK TABLES;


LOCK TABLES `refresh_token` WRITE;

INSERT INTO `refresh_token`
(ref_id,username,token,expiry_date) VALUES (1,'bdahal','hello','2022-01-02 14:00:45');

UNLOCK TABLES;

LOCK TABLES `role` WRITE;

INSERT INTO `role`(role_id,role_name) VALUES (2,'ROLE_QAEngineer'),(1,'ROLE_QAManager');

UNLOCK TABLES;

LOCK TABLES `test_case` WRITE;

INSERT INTO `test_case`(tc_id,tc_name,tc_created_by,tc_created_date,tc_modified_by,tc_modified_date) VALUES (1,'Verify that user is able to login to given application','Bibek Dahal','2022-01-01 14:00:45','Ramesh Khatri','2022-01-02 14:00:45'),(2,'Verify that user is able to login to given application without application','Bibek Dahal','2022-01-01 14:00:45','Ramesh Khatri','2022-01-02 14:00:45');

UNLOCK TABLES;




LOCK TABLES `users` WRITE;

INSERT INTO `users` VALUES (1,'Bibek','Dahal','$2a$12$p6yKh9nzxeU7i5tdKEC.EuL1EqS886mtPbmBPtQqH50YRhY7cqJES','bdahal'),(2,'Sugat','Paneru','$2a$12$V..aQ2IjTpLBdBf13Lu5ieyizpMgCfxZY8xNjQzQWRKLmT35AVxQi','spaneru');

UNLOCK TABLES;

-- LOCK TABLES `test_steps` WRITE;
-- UNLOCK TABLES;


LOCK TABLES `users_projects` WRITE;

INSERT INTO `users_projects`(up_id,user_id,pro_id) VALUES (1,1,1),(2,2,1);

UNLOCK TABLES;


LOCK TABLES `users_roles` WRITE;

INSERT INTO `users_roles`(ur_id,user_id,role_id) VALUES (1,1,1),(2,1,2),(3,2,2);

UNLOCK TABLES;


-- Dump completed on 2022-12-01  9:15:10
