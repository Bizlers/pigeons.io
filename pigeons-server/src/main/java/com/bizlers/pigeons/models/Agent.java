package com.bizlers.pigeons.models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity(name = "agent")
@XmlRootElement
public class Agent {

	public static final int HOSTNAME_LENGTH = 255;
	public static final int IP_LENGTH = 255;
	public static final int AGENTNAME_LENGTH = 255;

	public static final String STATUS_ONLINE = "O";
	public static final String STATUS_DELETED = "D";
	public static final String STATUS_OFFLINE = "S";

	@Id
	@Column(name = "agent_id", nullable = false)
	@TableGenerator(name = "agentSeqStore", table = "pigeons_seq_store", pkColumnName = "pigeons_seq_name", pkColumnValue = "agent.agent_id", valueColumnName = "pigeons_seq_value", initialValue = 1, allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "agentSeqStore")
	private long agentId;

	@Version
	@Column(name = "version", nullable = false)
	private int version;

	@Column(name = "account_id", nullable = false, unique = false)
	private long accountId;

	@Column(name = "agent_name", nullable = false)
	private String agentName;

	@Column(name = "machine_name", nullable = false)
	private String hostName;

	@Column(name = "machine_ip", nullable = false)
	private String ip;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "agent_port", nullable = false)
	private int statusListenerBrokerPort;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false)
	private Date created;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false)
	private Date lastUpdated;

	public long getAgentId() {
		return agentId;
	}

	public void setAgentId(long agentId) {
		this.agentId = agentId;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
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

	public int getStatusListenerBrokerPort() {
		return statusListenerBrokerPort;
	}

	public void setStatusListenerBrokerPort(int port) {
		this.statusListenerBrokerPort = port;
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
	public void setLastUpdated(Date lastupdated) {
		this.lastUpdated = lastupdated;
	}

	@JsonIgnore
	public void copyObject(Agent ai) {
		this.agentId = ai.agentId;
		this.created = ai.created;
		this.hostName = ai.hostName;
		this.ip = ai.ip;
		this.lastUpdated = ai.lastUpdated;
		this.status = ai.status;
		this.agentName = ai.agentName;
		this.statusListenerBrokerPort = ai.statusListenerBrokerPort;
		this.version = ai.version;
	}

	@Override
	public String toString() {
		return "Agent [agentId=" + agentId + ", version=" + version + ", accountId=" + accountId + ", agentName="
				+ agentName + ", hostName=" + hostName + ", ip=" + ip + ", status=" + status
				+ ", statusListenerBrokerPort=" + statusListenerBrokerPort + "]";
	}

	
}