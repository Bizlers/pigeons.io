package com.bizlers.pigeons.models;

import java.util.Date;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "Dali", date = "2012-12-06T01:55:41.227+0530")
@StaticMetamodel(Pigeon.class)
public class Pigeon_ {
	public static volatile SingularAttribute<Pigeon, Long> id;
	public static volatile SingularAttribute<Pigeon, String> region;
	public static volatile SingularAttribute<Pigeon, Long> appId;
	public static volatile SingularAttribute<Pigeon, String> ip;
	public static volatile SingularAttribute<Pigeon, Integer> port;
	public static volatile SingularAttribute<Pigeon, String> userName;
	public static volatile SingularAttribute<Pigeon, String> password;
	public static volatile SingularAttribute<Pigeon, String> status;
	public static volatile SingularAttribute<Pigeon, String> clientId;
	public static volatile SingularAttribute<Pigeon, String> pigeonType;
	public static volatile SingularAttribute<Pigeon, String> brokerName;
	public static volatile SingularAttribute<Pigeon, Date> created;
	public static volatile SingularAttribute<Pigeon, Date> lastUpdated;
	public static volatile SingularAttribute<Pigeon, Integer> version;
	public static volatile SingularAttribute<Pigeon, Long> agentId;
	public static volatile SingularAttribute<Pigeon, Integer> redirectPort;
}