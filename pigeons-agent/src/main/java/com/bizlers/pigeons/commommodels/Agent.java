package com.bizlers.pigeons.commommodels;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Agent {
	public static final String STATUS_ONLINE = "O";
	public static final String STATUS_OFFLINE = "S";
	public static final String STATUS_DELETED = "D";

	private long agentId;
	private long accountId;
	private String agentName;
	private String hostName;
	private String ip;
	private String status;
	private int statusListenerBrokerPort;
	private int version;

	public Agent() {
	}

	public Agent(long agentId, long accountId, String agentName,
			String hostName, String ip, String status,
			int statusListenerBrokerPort) {
		this.agentId = agentId;
		this.accountId = accountId;
		this.agentName = agentName;
		this.hostName = hostName;
		this.ip = ip;
		this.status = status;
		this.statusListenerBrokerPort = statusListenerBrokerPort;
	}

	public long getAgentId() {
		return agentId;
	}

	public void setAgentId(long agentId) {
		this.agentId = agentId;
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public int getStatusListenerBrokerPort() {
		return statusListenerBrokerPort;
	}

	public void setStatusListenerBrokerPort(int statusListenerBrokerPort) {
		this.statusListenerBrokerPort = statusListenerBrokerPort;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String toString() {

		return "AgentId :" + agentId + " Account Id : " + accountId
				+ "Agentname: " + agentName + " HostName: " + hostName
				+ " IP: " + ip + " Status: " + status
				+ " StatusListenerBrokerPort: " + statusListenerBrokerPort
				+ " Version: " + version;
	}

}
