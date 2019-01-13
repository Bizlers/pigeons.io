package com.bizlers.pigeons.api.server.support.persistent.models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity(name = "pigeon_usage")
public class PigeonUsage {

	@Id
	@Column(name = "user_id", nullable = false)
	private long userId;

	@Column(name = "pigeon_id", nullable = false)
	private long pigeonId;

	@Column(name = "pigeon_count", nullable = false)
	private int pigeonCount;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "retrieved_time", nullable = false)
	private Date retrievedTime;

	public PigeonUsage() {
	}

	public PigeonUsage(long userId, long pigeonId, int pigeonCount,
			Date retrievedTime) {
		this.userId = userId;
		this.pigeonCount = pigeonCount;
		this.pigeonId = pigeonId;
		this.retrievedTime = retrievedTime;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getPigeonId() {
		return pigeonId;
	}

	public void setPigeonId(long pigeonId) {
		this.pigeonId = pigeonId;
	}

	public int getPigeonCount() {
		return pigeonCount;
	}

	public void setPigeonCount(int pigeonCount) {
		this.pigeonCount = pigeonCount;
	}

	public Date getRetrievedTime() {
		return retrievedTime;
	}

	public void setRetrievedTime(Date retreivedTime) {
		this.retrievedTime = retreivedTime;
	}
}
