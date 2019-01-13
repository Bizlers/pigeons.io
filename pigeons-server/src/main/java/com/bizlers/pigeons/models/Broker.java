package com.bizlers.pigeons.models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlRootElement
@Entity(name = "broker")
public class Broker {

	public static final int BROKERNAME_LENGTH = 4;
	public static final int IP_LENGTH = 255;
	public static final int RETRYCOUNT = 5;
	public static final String BROKERNAME_PATTERN = "^[a-zA-Z0-9]*$";

	public static final String STATUS_ONLINE = "O";
	public static final String STATUS_DELETED = "D";
	public static final String STATUS_OFFLINE = "S";
	public static final String STATUS_ALL = "ALL";

	public static final String TYPE_L0 = "L0";
	public static final String TYPE_L1 = "L1";
	public static final String TYPE_L2 = "L2";

	@Id
	@TableGenerator(name = "brokerSeqStore", table = "pigeons_seq_store", pkColumnName = "pigeons_seq_name", pkColumnValue = "broker.broker_id", valueColumnName = "pigeons_seq_value", initialValue = 1, allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "brokerSeqStore")
	@Column(name = "broker_id", nullable = false)
	private long id;

	@Version
	@Column(name = "version")
	private int version;

	@Column(name = "agent_id")
	private long agentId;

	@Column(name = "broker_ip")
	private String ip;

	@Column(name = "broker_port")
	private int port;

	@Column(name = "max_limit")
	private int maxLimit;

	@Column(name = "alloted")
	private int alloted;

	@Column(name = "pigeons_created", nullable = false)
	private int pigeonsCreated;

	@Column(name = "region")
	private String region;

	@Column(name = "status", nullable = false)
	private String status;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false)
	private Date created;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false)
	private Date lastUpdated;

	@Column(name = "broker_name", nullable = false, unique=true)
	private String brokerName;

	@OneToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "broker_connection", joinColumns = { @JoinColumn(name = "broker_id", referencedColumnName = "broker_id") }, inverseJoinColumns = { @JoinColumn(name = "connected_broker_id", referencedColumnName = "broker_id") })
	private List<Broker> connectedTo;

	@Column(name = "broker_type")
	private String brokerType;

	@Column(name = "bridge_count")
	private int bridgeCount;

	@Column(name = "redirect_port")
	private int redirectPort;
	
	@Transient
	private String connectedBrokerNames; 
	
	public Broker() {
		
	}
	
	@JsonIgnore
	public long getId() {
		return id;
	}

	@JsonIgnore
	public void setId(long id) {
		this.id = id;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
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

	public void setMaxLimit(int maxLimit) {
		this.maxLimit = maxLimit;
	}

	public int getAlloted() {
		return alloted;
	}

	public void setAlloted(int alloted) {
		this.alloted = alloted;
	}

	@JsonIgnore
	public int getPigeonsCreated() {
		return pigeonsCreated;
	}

	@JsonIgnore
	public void setPigeonsCreated(int pigeonsCreated) {
		this.pigeonsCreated = pigeonsCreated;
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

	@JsonIgnore
	public Date getCreated() {
		return created;
	}

	@JsonIgnore
	public void setCreated(Date created) {
		this.created = created;
	}

	@JsonIgnore
	public Date getLastUpdated() {
		return lastUpdated;
	}

	@JsonIgnore
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public String getBrokerName() {
		return brokerName;
	}

	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
	}

	@JsonIgnore
	public List<Broker> getConnectedTo() {
		return connectedTo;
	}

	@JsonIgnore
	public void setConnectedTo(List<Broker> connectedTo) {
		this.connectedTo = connectedTo;
	}

	public String getBrokerType() {
		return brokerType;
	}

	public void setBrokerType(String brokerType) {
		this.brokerType = brokerType;
	}

	@JsonIgnore
	public int getBridgeCount() {
		return bridgeCount;
	}

	@JsonIgnore
	public void setBridgeCount(int bridgeCount) {
		this.bridgeCount = bridgeCount;
	}

	public int getRedirectPort() {
		return redirectPort;
	}

	public void setRedirectPort(int redirectPort) {
		this.redirectPort = redirectPort;
	}

	public void setBrokers(String brokers) {
		this.connectedBrokerNames = brokers.isEmpty() ? null : brokers.trim();
	}

	public String getBrokers() {
		String brokerNames = null;
		if (connectedBrokerNames == null) {
			if (connectedTo != null && connectedTo.size() > 0) {
				StringBuilder builder = new StringBuilder(connectedTo.get(0).getBrokerName());
				for (int i = 1; i < connectedTo.size(); i++) {
					builder.append(":").append(connectedTo.get(i).getBrokerName());
				}
				brokerNames = builder.toString();
			} 
		} else {
			brokerNames = connectedBrokerNames;
		}
		return brokerNames;
	}

	@JsonIgnore
	public void copyObject(Broker broker) {
		this.agentId = broker.agentId;
		this.alloted = broker.alloted;
		this.brokerName = broker.brokerName;
		this.created = broker.created;
		this.pigeonsCreated = broker.pigeonsCreated;
		this.id = broker.id;
		this.ip = broker.ip;
		this.lastUpdated = broker.lastUpdated;
		this.maxLimit = broker.maxLimit;
		this.port = broker.port;
		this.region = broker.region;
		this.status = broker.status;
		this.connectedTo = broker.connectedTo;
		this.brokerType = broker.brokerType;
		this.bridgeCount = broker.bridgeCount;
		this.version = broker.version;
		this.redirectPort = broker.redirectPort;

	}

	@Override
	public String toString() {
		return "Broker [id=" + id + ", version=" + version + ", agentId=" + agentId + ", ip=" + ip + ", port=" + port
				+ ", maxLimit=" + maxLimit + ", alloted=" + alloted + ", pigeonsCreated=" + pigeonsCreated + ", region="
				+ region + ", status=" + status + ", created=" + created + ", lastUpdated=" + lastUpdated
				+ ", brokerName=" + brokerName + ", connectedTo=" + connectedTo + ", brokerType=" + brokerType
				+ ", bridgeCount=" + bridgeCount + ", redirectPort=" + redirectPort + "]";
	}


}