package com.bizlers.pigeons.api.server.internal;

import java.util.List;

import com.bizlers.pigeons.api.server.persistent.models.MessageInfo;

public interface MessageStore {

	public void insert(MessageInfo message) ;

	public List<MessageInfo> get(long currentTime, long interval) ;

	public void delete(long currentTime, long interval) ;

	public void delete(long id) ;
	
	public void delete(long id, boolean physical);

	public MessageInfo get(long id);

}
