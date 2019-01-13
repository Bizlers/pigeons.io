package com.bizlers.pigeons.agent.core;

public class PigeonAgentException extends Exception {

	private static final long serialVersionUID = 1L;

	String errorMessage;

	public PigeonAgentException() {
		super();
		errorMessage = "unknown";
	}

	public PigeonAgentException(String errorMessage) {
		super(errorMessage);
		this.errorMessage = errorMessage;
	}

	public PigeonAgentException(Throwable cause) {
		super(cause);
	}

	public PigeonAgentException(String errorMessage, Throwable cause) {
		super(errorMessage, cause);
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString() {
		return errorMessage;
	}

	public String getError() {
		return errorMessage;
	}
}
