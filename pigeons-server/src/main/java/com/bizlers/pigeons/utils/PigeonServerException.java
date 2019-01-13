package com.bizlers.pigeons.utils;

public class PigeonServerException extends Exception {

	private static final long serialVersionUID = 1L;

	private String errorMessage;

	public PigeonServerException() {
		super();
		errorMessage = "unknown";
	}

	public PigeonServerException(String errorMessage) {
		super(errorMessage);
		this.errorMessage = errorMessage;
	}

	public PigeonServerException(Throwable cause) {
		super(cause);
	}

	public PigeonServerException(String errorMessage, Throwable cause) {
		super(errorMessage, cause);
		this.errorMessage = errorMessage;
	}

	public String getError() {
		return errorMessage;
	}

	@Override
	public String toString() {
		return errorMessage;
	}
}
