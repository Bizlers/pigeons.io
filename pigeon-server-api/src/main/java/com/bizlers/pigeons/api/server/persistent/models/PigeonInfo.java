package com.bizlers.pigeons.api.server.persistent.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "pigeon_info")
public class PigeonInfo {

	@Id
	@Column(name = "pigeon_id", nullable = false)
	private long pigeonId;

	@Column(name = "status", nullable = false)
	private int status;

	@Column(name = "client_id", nullable = false)
	private String clientId;

	public PigeonInfo() {
	}

	public PigeonInfo(long pigeonId, String clientId, int status) {
		this.pigeonId = pigeonId;
		this.clientId = clientId;
		this.status = status;
	}

	public long getPigeonId() {
		return pigeonId;
	}

	public void setPigeonId(long pigeonId) {
		this.pigeonId = pigeonId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}
