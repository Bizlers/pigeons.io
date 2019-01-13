package com.bizlers.pigeons.api.server;

import com.bizlers.pigeons.api.server.persistent.models.PigeonInfo;

public class PublishItem {

	private PigeonInfo pigeon;

	private String message;

	private boolean acknowledgement;

	private long messageId;

	private boolean multiCast;

	private String topic;

	private int qos;

	private boolean retained;

	public PublishItem(long messageId, PigeonInfo pigeon, String message,
			boolean acknowledgement, boolean multiCast) {
		this.pigeon = pigeon;
		this.message = message;
		this.acknowledgement = acknowledgement;
		this.messageId = messageId;
		this.multiCast = multiCast;
	}

	public PublishItem(String topic, String message, int qos,
			boolean retained, boolean multiCast) {
		this.topic = topic;
		this.message = message;
		this.qos = qos;
		this.retained = retained;
		this.multiCast = multiCast;
	}

	public PigeonInfo getPigeon() {
		return pigeon;
	}

	public String getMessage() {
		return message;
	}

	public boolean isAcknowledgement() {
		return acknowledgement;
	}

	public long getMessageId() {
		return messageId;
	}

	public boolean isMultiCast() {
		return multiCast;
	}

	public String getTopic() {
		return topic;
	}

	public int getQos() {
		return qos;
	}

	public boolean isRetained() {
		return retained;
	}
}
