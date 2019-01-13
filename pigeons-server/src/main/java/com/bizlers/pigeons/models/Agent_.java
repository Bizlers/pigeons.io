package com.bizlers.pigeons.models;

import java.util.Date;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "Dali", date = "2012-12-15T17:40:03.243+0530")
@StaticMetamodel(Agent.class)
public class Agent_ {
	public static volatile SingularAttribute<Agent, Long> agentId;
	public static volatile SingularAttribute<Agent, String> agentName;
	public static volatile SingularAttribute<Agent, String> hostName;
	public static volatile SingularAttribute<Agent, String> ip;
	public static volatile SingularAttribute<Agent, Long> accountId;
	public static volatile SingularAttribute<Agent, String> status;
	public static volatile SingularAttribute<Agent, Integer> statusListenerBrokerPort;
	public static volatile SingularAttribute<Agent, Date> created;
	public static volatile SingularAttribute<Agent, Date> lastUpdated;
	public static volatile SingularAttribute<Agent, Integer> version;
}