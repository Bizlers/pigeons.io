CREATE TABLE `pigeon_usage` (
  `user_id` bigint(20) NOT NULL ,
  `pigeon_id` bigint(20) NOT NULL,
  `retrieved_time` timestamp NULL DEFAULT NULL,
  `pigeon_count` int DEFAULT NULL,
  PRIMARY KEY (`user_id`)
);