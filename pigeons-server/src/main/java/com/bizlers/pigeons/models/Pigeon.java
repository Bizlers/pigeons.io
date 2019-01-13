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

@XmlRootElement
@Entity(name = "pigeon")
public class Pigeon {

	public static final int CLIENTID_LENGTH = 20;
	public static final int PIGEONCOUNT = 100;
	public static final int RETRYCOUNT = 5;
	public static final int SLEEP = 10000;
	public static final String CLIENTID_PATTERN = "^[a-zA-Z0-9]*/[a-zA-Z0-9]*/[a-zA-Z0-9]*$";

	public static final String STATUS_ONLINE = "O";
	public static final String STATUS_DELETED = "D";
	public static final String STATUS_ALLOTED = "A";
	public static final String STATUS_CREATED = "C";
	public static final String STATUS_READY = "R";
	public static final String TYPE_L0 = "L0";
	public static final String TYPE_L1 = "L1";
	public static final String TYPE_L2 = "L2";

	@Id
	@TableGenerator(name = "pigeonSeqStore", table = "pigeons_seq_store", pkColumnName = "pigeons_seq_name", pkColumnValue = "pigeon.id", valueColumnName = "pigeons_seq_value", initialValue = 1, allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "pigeonSeqStore")
	@Column(name = "id", nullable = false)
	private long id;

	@Version
	@Column(name = "version", nullable = false)
	private int version;

	@Column(name = "region", nullable = false)
	private String region;

	@Column(name = "app_id", nullable = false)
	private long appId;

	@Column(name = "pigeon_ip", nullable = false)
	private String ip;

	@Column(name = "pigeon_port", nullable = false)
	private int port;

	@Column(name = "pigeon_username", nullable = false)
	private String userName;

	@Column(name = "pigeon_passwd", nullable = false)
	private String password;

	@Column(name = "pigeon_status", nullable = false)
	private String status;

	@Column(name = "client_id", nullable = false)
	private String clientId;

	@Column(name = "pigeon_type", nullable = false)
	private String pigeonType;

	@Column(name = "brokername", nullable = false)
	private String brokerName;

	@Column(name = "agent_id", nullable = false)
	private long agentId;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false)
	private Date created;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false)
	private Date lastUpdated;

	@Column(name = "redirect_port", nullable = false)
	private int redirectPort;

	public Pigeon() {
	}

	public Pigeon(String ipAddress, int port, String region, String pigeonType,
			String brokerName, long agentId, int redirectPort) {
		this.ip = ipAddress;
		this.port = port;
		this.region = region;
		this.pigeonType = pigeonType;
		this.brokerName = brokerName;
		this.agentId = agentId;
		this.redirectPort = redirectPort;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public long getAppId() {
		return appId;
	}

	public void setAppId(long appId) {
		this.appId = appId;
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

	@JsonIgnore
	public String getPigeonType() {
		return pigeonType;
	}

	@JsonIgnore
	public void setPigeonType(String pigeonType) {
		this.pigeonType = pigeonType;
	}

	@JsonIgnore
	public String getBrokerName() {
		return brokerName;
	}

	@JsonIgnore
	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
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

	@JsonIgnore
	public int getRedirectPort() {
		return redirectPort;
	}

	@JsonIgnore
	public void setRedirectPort(int redirectPort) {
		this.redirectPort = redirectPort;
	}

	@JsonIgnore
	public long getAgentId() {
		return agentId;
	}

	@JsonIgnore
	public void setAgentId(long agentId) {
		this.agentId = agentId;
	}

	@JsonIgnore
	public void copyObject(Pigeon pigeon) {
		this.clientId = pigeon.clientId;
		this.created = pigeon.created;
		this.id = pigeon.id;
		this.ip = pigeon.ip;
		this.lastUpdated = pigeon.lastUpdated;
		this.password = pigeon.password;
		this.port = pigeon.port;
		this.region = pigeon.region;
		this.appId = pigeon.appId;
		this.userName = pigeon.userName;
		this.status = pigeon.status;
		this.brokerName = pigeon.brokerName;
		this.pigeonType = pigeon.pigeonType;
		this.version = pigeon.version;
		this.agentId = pigeon.agentId;
		this.redirectPort = pigeon.redirectPort;
	}

	@Override
	public String toString() {
		return "Pigeon [id=" + id + ", version=" + version + ", region="
				+ region + ", appId=" + appId + ", ip=" + ip + ", port=" + port
				+ ", userName=" + userName + ", password=" + password
				+ ", status=" + status + ", clientId=" + clientId
				+ ", pigeonType=" + pigeonType + ", brokerName=" + brokerName
				+ ", agentId=" + agentId + ", created=" + created
				+ ", lastUpdated=" + lastUpdated + " RedirectPort= "
				+ redirectPort + "]";
	}
}
