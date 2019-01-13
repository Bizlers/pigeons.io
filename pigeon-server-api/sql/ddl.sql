
CREATE TABLE `message_info` (
  `message_id` bigint(20) NOT NULL,
  `pigeon_id` bigint(20) NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  `pigeon_listener_id` int(11) DEFAULT NULL,
  `state` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`message_id`)
) 

CREATE TABLE `pigeon_info` (
  `pigeon_id` bigint(20) NOT NULL ,
  `status` int(20) NOT NULL,
  `client_id` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`pigeon_id`)
);