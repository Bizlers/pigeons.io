package com.bizlers.pigeons.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TableGenerator;
import javax.xml.bind.annotation.XmlRootElement;

@Entity(name = "application")
@XmlRootElement
public class Application {

	public static final int DEACTIVATED = 3;

	public static final int OFFLINE = 2;

	public static final int ONLINE = 1;

	@Id
	@TableGenerator(name = "applicationSeqStore", table = "pigeons_seq_store", pkColumnName = "pigeons_seq_name", pkColumnValue = "application.app_id", valueColumnName = "pigeons_seq_value", initialValue = 1, allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "applicationSeqStore")
	@Column(name = "app_id", nullable = false)
	private long appId;

	@Column(name = "account_id", nullable = false, unique = false)
	private long accountId;

	@Column(name = "email_id", nullable = false, unique = false)
	private String emailId;

	@Column(name = "state", nullable = false)
	private int state;

	public Application() {
	}

	public Application(long appId, String emailId, int state, long accountId) {
		this.appId = appId;
		this.emailId = emailId;
		this.state = state;
		this.accountId = accountId;
	}

	public long getAppId() {
		return appId;
	}

	public void setAppId(long appId) {
		this.appId = appId;
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}

	public String toString() {
		return " ApplicationId : " + appId + " emailId : " + emailId
				+ " State : " + state;
	}
}