package com.bizlers.pigeons.api.server.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bizlers.pigeons.api.server.persistent.models.MessageInfo;

public class MemoryMessageStore implements MessageStore {

	private Map<Long, MessageInfo> messages;

	public MemoryMessageStore() {
		messages = new HashMap<Long, MessageInfo>();
	}

	@Override
	public synchronized void insert(MessageInfo message) {
		messages.put(message.getMessageId(), message);
	}

	@Override
	public synchronized void delete(long id) {
		messages.get(id).setState(MessageInfo.STATE_DELETED);
	}

	@Override
	public synchronized void delete(long tmstamp, long interval) {
		for (Iterator<Long> iterator = messages.keySet().iterator(); iterator
				.hasNext();) {
			Long id = (Long) iterator.next();
			MessageInfo msg = messages.get(id);
			if ((msg.getTimestamp() + interval) < tmstamp) {
				msg.setState(MessageInfo.STATE_DELETED);
			}
		}
	}

	@Override
	public synchronized MessageInfo get(long id) {
		return messages.get(id);
	}

	@Override
	public synchronized List<MessageInfo> get(long tmstamp, long interval) {
		List<MessageInfo> result = new ArrayList<MessageInfo>();
		for (Iterator<Long> iterator = messages.keySet().iterator(); iterator.hasNext();) {
			Long id = (Long) iterator.next();
			MessageInfo msg = messages.get(id);
			if (msg.getState() != MessageInfo.STATE_DELETED) {
				if ((msg.getTimestamp() + interval) < tmstamp) {
					result.add(msg);
				}
			}
		}
		return result;
	}

	@Override
	public void delete(long id, boolean physical) {
		if (physical) {
			messages.remove(id);
		} else {
			messages.get(id).setState(MessageInfo.STATE_DELETED);
		}
	}
}
