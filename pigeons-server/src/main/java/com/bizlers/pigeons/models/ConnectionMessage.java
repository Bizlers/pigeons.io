package com.bizlers.pigeons.models;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ConnectionMessage implements Delayed {

	private static final long ACK_TIMEOUT = 120 * 1000;

	public static final char CONNECT_MESSAGE = 'C';
	public static final char ALLOCATE_MESSAGE = 'A';
	public static final char DISCONNECT_MESSAGE = 'D';

	private long pigeonId;
	private long appId;
	private char messageType;
	private long sentTime;

	public ConnectionMessage(long pigeonId, char messageType) {
		this.pigeonId = pigeonId;
		this.messageType = messageType;
	}

	public ConnectionMessage(long appId, long pigeonId, char messageType) {
		this.appId = appId;
		this.pigeonId = pigeonId;
		this.messageType = messageType;
	}

	public long getPigeonId() {
		return pigeonId;
	}

	public void setPigeonId(long pigeonId) {
		this.pigeonId = pigeonId;
	}

	public long getAppId() {
		return appId;
	}

	public void setAppId(long appId) {
		this.appId = appId;
	}

	public char getMessageType() {
		return messageType;
	}

	public void setMessageType(char messageType) {
		this.messageType = messageType;
	}

	public long getSentTime() {
		return sentTime;
	}

	public void setSentTime(long sentTime) {
		this.sentTime = sentTime;
	}

	@Override
	public long getDelay(TimeUnit timeUnit) {
		return ((sentTime + ACK_TIMEOUT) - System.currentTimeMillis());
	}

	@Override
	public int compareTo(Delayed o) {
		if (this.sentTime < ((ConnectionMessage) o).sentTime) {
			return -1;
		}
		if (this.sentTime > ((ConnectionMessage) o).sentTime) {
			return 1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConnectionMessage) {
			ConnectionMessage message = (ConnectionMessage) obj;
			return (this.pigeonId == message.pigeonId && this.messageType == message.messageType);
		}
		return false;
	}

	@Override
	public String toString() {
		return "ConnectionMessage [pigeonId=" + pigeonId + ", messageType="
				+ messageType + "]";
	}
}
