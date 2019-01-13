package com.bizlers.pigeons.api.server;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Pigeon {

	public static final String STATUS_CREATED = "C";

	public static final String STATUS_ALLOTED = "A";

	public static final String STATUS_ONLINE = "O";

	public static final String STATUS_DELETED = "D";

	private String region;

	private String ip;

	private int port;

	private String userName;

	private String password;

	private String status;

	private String clientId;

	private int version;

	public Pigeon() {

	}

	public Pigeon(String region, String ip, int port, String userName,
			String password, String status, String clientId, int version) {
		this.region = region;
		this.ip = ip;
		this.port = port;
		this.clientId = clientId;
		this.status = status;
		this.password = password;
		this.userName = userName;
		this.version = version;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String toString() {
		return " Version : " + version + " ClientId : " + clientId + " IP : "
				+ ip + " Port : " + port + " UserName : " + userName
				+ " Password : " + password + " Status : " + status;
	}
}