package com.bizlers.pigeons.api.server;

import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.eclipse.paho.client.mqttv3.MqttException;

public class PigeonServerException extends Exception {

	private static final long serialVersionUID = 1L;

	/*
	 * Reason codes mapped to MqttExceptions. Reserved 1 - 64.
	 */
	public static final short REASON_CODE_BROKER_UNAVAILABLE = 1;

	public static final short REASON_CODE_CLIENT_ALREADY_DISCONNECTED = 2;

	public static final short REASON_CODE_CLIENT_CLOSED = 3;

	public static final short REASON_CODE_CLIENT_CONNECTED = 4;

	public static final short REASON_CODE_CLIENT_DISCONNECT_PROHIBITED = 5;

	public static final short REASON_CODE_CLIENT_DISCONNECTING = 6;

	public static final short REASON_CODE_CLIENT_EXCEPTION = 7;

	public static final short REASON_CODE_CLIENT_NOT_CONNECTED = 8;

	public static final short REASON_CODE_CLIENT_TIMEOUT = 9;

	public static final short REASON_CODE_CONNECT_IN_PROGRESS = 10;

	public static final short REASON_CODE_CONNECTION_LOST = 11;

	public static final short REASON_CODE_FAILED_AUTHENTICATION = 12;

	public static final short REASON_CODE_INVALID_CLIENT_ID = 13;

	public static final short REASON_CODE_INVALID_MESSAGE = 14;

	public static final short REASON_CODE_INVALID_PROTOCOL_VERSION = 15;

	public static final short REASON_CODE_MAX_INFLIGHT = 16;

	public static final short REASON_CODE_NO_MESSAGE_IDS_AVAILABLE = 17;

	public static final short REASON_CODE_NOT_AUTHORIZED = 18;

	public static final short REASON_CODE_SERVER_CONNECT_ERROR = 19;

	public static final short REASON_CODE_SOCKET_FACTORY_MISMATCH = 20;

	public static final short REASON_CODE_SSL_CONFIG_ERROR = 21;

	public static final short REASON_CODE_SUBSCRIBE_FAILED = 22;

	public static final short REASON_CODE_TOKEN_INUSE = 23;

	public static final short REASON_CODE_UNEXPECTED_ERROR = 24;

	// Other reason codes
	public static final short REASON_CODE_INVALID_PIGEON_ID = 65;

	public static final short REASON_CODE_NULL_PIGEON = 66;

	public static final short REASON_CODE_NULL_PIGEON_LISTENER = 67;

	public static final short REASON_CODE_PUBLISHER_NOT_CONNECTED = 68;

	public static final short REASON_CODE_PUBLISH_PIGEON_FOUND = 69;

	public static final short REASON_CODE_PUBLISH_PIGEON_NOT_FOUND = 70;

	public static final short REASON_CODE_SOCKET_EXCEPTION = 71;

	public static final short REASON_CODE_SOCKET_TIMEOUT = 72;

	private int reasonCode;

	public PigeonServerException(String message) {
		super(message);
	}

	public PigeonServerException(String message, int reasonCode) {
		super(message);
		this.reasonCode = reasonCode;
	}

	public PigeonServerException(String message, Exception exception) {
		super(message, exception.getCause());
		if (exception instanceof MqttException) {
			int reasonCode = ((MqttException) exception).getReasonCode();
			if (reasonCode > 0) {
				this.reasonCode = getReasonCode(((MqttException) exception)
						.getReasonCode());
			} else if (exception.getCause() instanceof SocketTimeoutException) {
				this.reasonCode = REASON_CODE_SOCKET_TIMEOUT;
			} else if (exception.getCause() instanceof SocketException) {
				this.reasonCode = REASON_CODE_SOCKET_EXCEPTION;
			}
		}
	}

	public int getReasonCode() {
		return reasonCode;
	}

	private int getReasonCode(int mqttReasonCode) {
		switch (mqttReasonCode) {
		case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
			return REASON_CODE_BROKER_UNAVAILABLE;

		case MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED:
			return REASON_CODE_CLIENT_ALREADY_DISCONNECTED;

		case MqttException.REASON_CODE_CLIENT_CLOSED:
			return REASON_CODE_CLIENT_CLOSED;

		case MqttException.REASON_CODE_CLIENT_CONNECTED:
			return REASON_CODE_CLIENT_CONNECTED;

		case MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED:
			return REASON_CODE_CLIENT_DISCONNECT_PROHIBITED;

		case MqttException.REASON_CODE_CLIENT_DISCONNECTING:
			return REASON_CODE_CLIENT_DISCONNECTING;

		case MqttException.REASON_CODE_CLIENT_EXCEPTION:
			return REASON_CODE_CLIENT_EXCEPTION;

		case MqttException.REASON_CODE_CLIENT_NOT_CONNECTED:
			return REASON_CODE_CLIENT_NOT_CONNECTED;

		case MqttException.REASON_CODE_CLIENT_TIMEOUT:
			return REASON_CODE_CLIENT_TIMEOUT;

		case MqttException.REASON_CODE_CONNECT_IN_PROGRESS:
			return REASON_CODE_CONNECT_IN_PROGRESS;

		case MqttException.REASON_CODE_CONNECTION_LOST:
			return REASON_CODE_CONNECTION_LOST;

		case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
			return REASON_CODE_FAILED_AUTHENTICATION;

		case MqttException.REASON_CODE_INVALID_CLIENT_ID:
			return REASON_CODE_INVALID_CLIENT_ID;

		case MqttException.REASON_CODE_INVALID_MESSAGE:
			return REASON_CODE_INVALID_MESSAGE;

		case MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION:
			return REASON_CODE_INVALID_PROTOCOL_VERSION;

		case MqttException.REASON_CODE_MAX_INFLIGHT:
			return REASON_CODE_MAX_INFLIGHT;

		case MqttException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE:
			return REASON_CODE_NO_MESSAGE_IDS_AVAILABLE;

		case MqttException.REASON_CODE_NOT_AUTHORIZED:
			return REASON_CODE_NOT_AUTHORIZED;

		case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
			return REASON_CODE_SERVER_CONNECT_ERROR;

		case MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH:
			return REASON_CODE_SOCKET_FACTORY_MISMATCH;

		case MqttException.REASON_CODE_SSL_CONFIG_ERROR:
			return REASON_CODE_SSL_CONFIG_ERROR;

		case MqttException.REASON_CODE_SUBSCRIBE_FAILED:
			return REASON_CODE_SUBSCRIBE_FAILED;

		case MqttException.REASON_CODE_TOKEN_INUSE:
			return REASON_CODE_TOKEN_INUSE;
		}
		return REASON_CODE_UNEXPECTED_ERROR;
	}
}
