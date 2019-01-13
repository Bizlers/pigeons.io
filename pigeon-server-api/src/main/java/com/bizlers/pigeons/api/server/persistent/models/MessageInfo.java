package com.bizlers.pigeons.api.server.persistent.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "message_info")
public class MessageInfo {
	
	public static final int STATE_DELETED = 1;
	
	@Id
	@Column(name = "message_id", nullable = false)
	private long messageId;

	@Column(name = "timestamp", nullable = false)
	private long timestamp;

	@Column(name = "pigeon_id", nullable = false)
	private long pigeonId;

	@Column(name = "pigeon_listener_id")
	private int pigeonListenerId;
	
	@Column(name = "state", nullable = false, columnDefinition="default '0'")
	private int state;
	
	public MessageInfo() {
	}

	public MessageInfo(long messageId, long timestamp, long pigeonId, int pigeonListenerId) {
		this.messageId = messageId;
		this.timestamp = timestamp;
		this.pigeonId = pigeonId;
		this.pigeonListenerId = pigeonListenerId;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getPigeonId() {
		return pigeonId;
	}

	public void setPigeonId(long pigeonId) {
		this.pigeonId = pigeonId;
	}

	public int getPigeonListenerId() {
		return pigeonListenerId;
	}

	public void setPigeonListenerId(int pigeonListenerId) {
		this.pigeonListenerId = pigeonListenerId;
	}
	
	public void setState(int state) {
		this.state = state;
	}
	
	public int getState() {
		return state;
	}
}
