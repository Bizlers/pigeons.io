package com.bizlers.pigeons.api.server;

public class BroadcastMessage {

	private long messageId;
	private String message;

	public BroadcastMessage() {

	}

	public BroadcastMessage(long messageId, String message) {
		this.message = message;
		this.messageId = messageId;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
