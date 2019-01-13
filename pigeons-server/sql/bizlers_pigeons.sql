use bizlers_pigeons;

CREATE TABLE `agent` (
  `agent_id` bigint(20) NOT NULL,
  `machine_name` varchar(255) DEFAULT NULL,
  `machine_ip` varchar(255) DEFAULT NULL,
  `status` char(1) DEFAULT NULL,
  `created` timestamp NULL DEFAULT NULL,
  `last_updated` timestamp NULL DEFAULT NULL,
  `agent_name` varchar(255) DEFAULT NULL,
  `agent_port` int(11) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  `account_id` bigint(20) NOT NULL,
  PRIMARY KEY (`agent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `broker_connection` (
  `broker_id` int(11) DEFAULT NULL,
  `connected_broker_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `broker` (
  `broker_id` bigint(20) NOT NULL,
  `agent_id` varchar(20) DEFAULT NULL,
  `broker_ip` varchar(255) DEFAULT NULL,
  `broker_port` varchar(6) DEFAULT NULL,
  `max_limit` int(11) DEFAULT NULL,
  `alloted` int(11) DEFAULT NULL,
  `pigeons_created` int(11) DEFAULT NULL,
  `region` varchar(20) DEFAULT NULL,
  `status` char(1) DEFAULT NULL,
  `created` timestamp NULL DEFAULT NULL,
  `last_updated` timestamp NULL DEFAULT NULL,
  `broker_name` varchar(20) DEFAULT NULL,
  `bridge_count` int(11) DEFAULT NULL,
  `broker_type` varchar(2) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  `redirect_port` int(11) DEFAULT NULL,
  PRIMARY KEY (`broker_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `pigeon` (
  `id` bigint(20) NOT NULL,
  `region` varchar(20) DEFAULT NULL,
  `app_id` bigint(20) DEFAULT NULL,
  `pigeon_ip` varchar(255) DEFAULT NULL,
  `pigeon_port` varchar(6) DEFAULT NULL,
  `pigeon_username` varchar(45) DEFAULT NULL,
  `pigeon_passwd` varchar(45) DEFAULT NULL,
  `pigeon_status` char(1) DEFAULT NULL,
  `client_id` varchar(20) DEFAULT NULL,
  `created` timestamp NULL DEFAULT NULL,
  `last_updated` timestamp NULL DEFAULT NULL,
  `pigeon_type` varchar(2) DEFAULT NULL,
  `brokername` varchar(20) DEFAULT NULL,
  `agent_id` bigint(20) NOT NULL,
  `version` int(11) DEFAULT NULL,
  `redirect_port` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;





CREATE TABLE `application` (
  `app_id` bigint(20) NOT NULL,
  `email_id` varchar(255) DEFAULT NULL,
  `state` int(11) DEFAULT NULL,
   `account_id` bigint(20) NOT NULL,
  PRIMARY KEY (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `table_sessions` (
  `account_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(108) NOT NULL,
  `timestamp` DATETIME NOT NULL,
  PRIMARY KEY (`account_id`)
);

CREATE TABLE `table_role` (
  `role_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role` varchar(20) NOT NULL,
  PRIMARY KEY (`role_id`)
);

CREATE TABLE `table_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `role` varchar(20) NOT NULL,  
  PRIMARY KEY (`id`)
);

create table pigeons_seq_store
(
   `id` bigint(20) NOT NULL AUTO_INCREMENT,
	pigeons_seq_name varchar(255) not null,
	pigeons_seq_value bigint not null,
	primary key(id)
);

INSERT INTO pigeons_seq_store (pigeons_seq_name,pigeons_seq_value) VALUES ('agent.agent_id', 0);
INSERT INTO pigeons_seq_store (pigeons_seq_name,pigeons_seq_value) VALUES ('broker.broker_id', 0);
INSERT INTO pigeons_seq_store (pigeons_seq_name,pigeons_seq_value) VALUES ('connection_message.message_id', 0);
INSERT INTO pigeons_seq_store (pigeons_seq_name,pigeons_seq_value) VALUES ('pigeon.id', 0);
INSERT INTO pigeons_seq_store (pigeons_seq_name,pigeons_seq_value) VALUES ('application.app_id', 0);
