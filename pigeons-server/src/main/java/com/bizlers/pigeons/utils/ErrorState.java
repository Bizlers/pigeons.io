package com.bizlers.pigeons.utils;

public enum ErrorState {
	SUCCESS(0, "Success."), INVALID_ADDRESS(1,
			"Adress shoud not be null or greater than 250 characters."), INVALID_EMAILID(
			2, "Emailid is invalid."), INVALID_EMAILID_LEN(3,
			"Emailid should be not more than 255."), EXCEPTION(4,
			"Exception occurred in Class  ."), FAILURE(5, "Failure  ."), INVALID_USER(
			6, "User doesn't exit."), DUPLICATE_USER(7, "User already exists."), USER_DELETED(
			8, "User is deleted."), INVALID_APIKEY(9,
			"APIkey is null or should be not more than 100 characters. "), INVALID_ID(
			10, "ID is null. "), PIGEON_DELETED(11, "Pigeon is deleted."), INVALID_PIGEON(
			12, "Pigeon doesn't exist"), DUPLICATE_PIGEON(13,
			"Pigeon already Exists."), INVALID_BROKER(14,
			"Broker doesn't Exists."), BROKER_DELETED(15, "Broker is deleted."), INVALID_AGENT(
			16, "Agent doesn't exists"), AGENT_DELETED(17, "Agent is deleted."), REGISTRY_DELETED(
			18, "Registry is deleted."), INVALID_REGISTRY(19,
			"Registry doesn't Exists."), INVALID_USERNAME(20,
			"Username should not be null or greater than 50 characters."), INVALID_REQUEST(
			21, "Invalid request."), MAX_RETRY(22,
			"Max generation of key acheived."), INVALID_MCNAME(23,
			"Machine name should be greater than zero and less than 255 characters."), INVALID_IP(
			24, "IP is not valid ."), INVALID_PORT(25, "Invalid Port."), INVALID_REGION(
			26, "Invalid Region."), INVALID_CLIENTID(27, "Invalid clientid."), PARAM_REQ(
			28, "Kindly provide required parameter."), EMAIL_EXISTS(29,
			"Email already exists."), INVALID_MAXLIMIT(30,
			"Invalid Maximum number."), DUPLICATE_RECORDS(31,
			"Multiple records founds"), INVALID_AGENTNAME(32,
			"Invalid agentname."), INVALID_BROKERNAME(33, "Invalid brokername"), NORECORDSFOUND(
			34, "No records founds."), INVALID_CLIENTID_LEN(35,
			"Client is null or length is invalid"), INVALID_BROKERNAME_LEN(36,
			"brokername is null or length is invalid"), INVALID_AGENTNAME_LEN(
			37, "Agentname is null or length is invalid"), INVALID_BROKERTYPE(
			38, "Invalid broker type."), INVALID_NETWORK(38, "Invalid network."), DUPLICATE_BROKER(
			39, "Broker already exists"), DUPLICATE_AGENT(40,
			"Agent already exists"), INVALID_PARENT_BROKER(41,
			"Invalid Parent Broker."), DUPLICATE_APPLICATION(42,
			"Application already registered."), INVALID_AGENTID(43,
			"Invalid agentid.");
	private int code;
	private String message;

	ErrorState(final int code, final String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
