package com.bizlers.pigeons.commommodels;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Broker {
	public static final String STATUS_ONLINE = "O";
	public static final String STATUS_OFFLINE = "S";

	public static final String BROKER_TYPE_LEVEL_L0 = "L0";
	public static final String BROKER_TYPE_LEVEL_L1 = "L1";
	public static final String BROKER_TYPE_LEVEL_L2 = "L2";
	public static final String BROKER_TYPE_STATUS_LISTENER = "SL";

	private long agentId;

	private String ip;

	private int port;

	private int maxLimit;

	private int alloted;

	private int pigeonsCreated;

	private String region;

	private String status;

	private String brokerName;

	private List<Broker> connectedTo;

	private String brokerType;

	private int version;

	private int redirectPort;

	public Broker() {
	}

	public Broker(long agentId, String ip, int port, int maxLimit, int alloted,
			int pigeonsCreated, String region, String status,
			String brokerName, String brokerType, int redirectPort) {
		this.agentId = agentId;
		this.ip = ip;
		this.port = port;
		this.maxLimit = maxLimit;
		this.alloted = alloted;
		this.pigeonsCreated = pigeonsCreated;
		this.region = region;
		this.status = status;
		this.brokerName = brokerName;
		this.brokerType = brokerType;
		this.redirectPort = redirectPort;
	}

	public Broker(long agentId, String ip, int port, int maxLimit, int alloted,
			int pigeonsCreated, String region, String status,
			String brokerName, List<Broker> connectedTo, String brokerType,
			int redirectPort) {
		this.agentId = agentId;
		this.ip = ip;
		this.port = port;
		this.maxLimit = maxLimit;
		this.alloted = alloted;
		this.pigeonsCreated = pigeonsCreated;
		this.region = region;
		this.status = status;
		this.brokerName = brokerName;
		this.connectedTo = connectedTo;
		this.brokerType = brokerType;
		this.redirectPort = redirectPort;
	}

	public long getAgentId() {
		return agentId;
	}

	public void setAgentId(long agentId) {
		this.agentId = agentId;
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

	public int getMaxLimit() {
		return maxLimit;
	}

	public void setMaxLimit(int maxlimit) {
		this.maxLimit = maxlimit;
	}

	public int getAlloted() {
		return alloted;
	}

	public void setAlloted(int alloted) {
		this.alloted = alloted;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getBrokerName() {
		return brokerName;
	}

	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
	}

	@com.fasterxml.jackson.annotation.JsonIgnore
	public List<Broker> getConnectedTo() {
		return connectedTo;
	}

	@com.fasterxml.jackson.annotation.JsonIgnore
	public void setConnectedTo(List<Broker> connectedTo) {
		this.connectedTo = connectedTo;
	}

	public String getBrokerType() {
		return brokerType;
	}

	public void setBrokerType(String brokerType) {
		this.brokerType = brokerType;
	}

	public int getPigeonsCreated() {
		return pigeonsCreated;
	}

	public void setPigeonsCreated(int pigeonsCreated) {
		this.pigeonsCreated = pigeonsCreated;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public int getRedirectPort() {
		return redirectPort;
	}

	public void setRedirectPort(int redirectPort) {
		this.redirectPort = redirectPort;
	}

	public void setBrokers(String brokers) {
		if (brokers != null) {
			String broker[] = brokers.split(":");
			if (connectedTo == null && brokers.length() != 0) {
				connectedTo = new ArrayList<Broker>();
				for (int i = 0; i < broker.length; i++) {
					try {
						connectedTo.add(new Broker(0, null, 0, 0, 0, 0, null, null, broker[i], null, 0));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public String getBrokers() {
		String brokers = "";
		if (connectedTo != null && connectedTo.size() != 0) {
			brokers = connectedTo.get(0).getBrokerName();
			for (int i = 1; i < connectedTo.size(); i++) {
				brokers = brokers + ":" + connectedTo.get(i).getBrokerName();
			}
		}
		return brokers;
	}

	@Override
	public String toString() {
		return ip + ":" + port;
	}

}
