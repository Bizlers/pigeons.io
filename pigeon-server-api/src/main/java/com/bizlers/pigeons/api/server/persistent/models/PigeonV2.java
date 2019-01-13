package com.bizlers.pigeons.api.server.persistent.models;

import javax.xml.bind.annotation.XmlRootElement;

import com.bizlers.pigeons.api.server.Pigeon;
import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlRootElement
public class PigeonV2 {

	public static final String STATUS_CREATED = "C";
	public static final String STATUS_ALLOTED = "A";
	public static final String STATUS_ONLINE = "O";
	public static final String STATUS_DELETED = "D";

	private long appId;
	private long id;
	private String region;
	private String ip;
	private int port;
	private String userName;
	private String password;
	private String status;
	private String clientId;
	private int version;

	public PigeonV2() {
	}

	public PigeonV2(long appId, long id, String region, String ip, int port,
			String userName, String password, String status, String clientId,
			int version) {
		this.appId = appId;
		this.id = id;
		this.region = region;
		this.ip = ip;
		this.port = port;
		this.userName = userName;
		this.password = password;
		this.status = status;
		this.clientId = clientId;
		this.version = version;
	}

	public long getAppId() {
		return appId;
	}

	public void setAppId(long appId) {
		this.appId = appId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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

	@Override
	public String toString() {
		return "PigeonV2 [appId=" + appId + ", id=" + id + ", ip=" + ip
				+ ", port=" + port + ", password=" + password + ", status="
				+ status + ", clientId=" + clientId + ", version=" + version
				+ "]";
	}

	@JsonIgnore
	public Pigeon getPigeonV1() {
		return new Pigeon(region, ip, port, userName, password, status,
				clientId, version);
	}

}
