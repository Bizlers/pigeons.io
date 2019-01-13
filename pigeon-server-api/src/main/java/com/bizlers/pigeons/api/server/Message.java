package com.bizlers.pigeons.api.server;

public class Message {

	private long appId;

	private long messageId;

	private String message;

	private boolean acknowledgement;

	private long pairId;

	private String publishBroker;

	public Message() {
	}

	public Message(long appId, long messageId, String message,
			boolean acknowledgement, long pairId) {
		this.appId = appId;
		this.messageId = messageId;
		this.message = message;
		this.acknowledgement = acknowledgement;
		this.pairId = pairId;
	}

	public long getAppId() {
		return appId;
	}

	public void setAppId(long appId) {
		this.appId = appId;
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

	public boolean isAcknowledgement() {
		return acknowledgement;
	}

	public void setAcknowledgement(boolean acknowledgement) {
		this.acknowledgement = acknowledgement;
	}

	public long getPairId() {
		return pairId;
	}

	public void setPairId(long pairId) {
		this.pairId = pairId;
	}

	public String getPublishBroker() {
		return publishBroker;
	}

	public void setPublishBroker(String publishBroker) {
		this.publishBroker = publishBroker;
	}
}
