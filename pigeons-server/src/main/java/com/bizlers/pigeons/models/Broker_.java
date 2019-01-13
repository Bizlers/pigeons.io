package com.bizlers.pigeons.models;

import java.util.Date;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "Dali", date = "2012-11-27T17:29:49.272+0530")
@StaticMetamodel(Broker.class)
public class Broker_ {
	public static volatile SingularAttribute<Broker, Long> id;
	public static volatile SingularAttribute<Broker, Long> agentId;
	public static volatile SingularAttribute<Broker, String> ip;
	public static volatile SingularAttribute<Broker, Integer> port;
	public static volatile SingularAttribute<Broker, Integer> maxLimit;
	public static volatile SingularAttribute<Broker, Integer> alloted;
	public static volatile SingularAttribute<Broker, Integer> pigeonsCreated;
	public static volatile SingularAttribute<Broker, String> region;
	public static volatile SingularAttribute<Broker, String> status;
	public static volatile SingularAttribute<Broker, Date> created;
	public static volatile SingularAttribute<Broker, Date> lastUpdated;
	public static volatile SingularAttribute<Broker, String> brokerName;
	public static volatile ListAttribute<Broker, Broker> connectedTo;
	public static volatile SingularAttribute<Broker, Integer> bridgeCount;
	public static volatile SingularAttribute<Broker, String> brokerType;
	public static volatile SingularAttribute<Broker, Integer> version;
	public static volatile SingularAttribute<Broker, Integer> redirectPort;
}