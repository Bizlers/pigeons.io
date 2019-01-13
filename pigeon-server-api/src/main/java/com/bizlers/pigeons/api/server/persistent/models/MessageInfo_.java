package com.bizlers.pigeons.api.server.persistent.models;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(MessageInfo.class)
public class MessageInfo_ {
	public static volatile SingularAttribute<MessageInfo, Long> messageId;
	public static volatile SingularAttribute<MessageInfo, Long> pigeonId;
	public static volatile SingularAttribute<MessageInfo, Long> timestamp;
	public static volatile SingularAttribute<MessageInfo, Integer> pigeonListenerId;
	public static volatile SingularAttribute<MessageInfo, Integer> state;
}