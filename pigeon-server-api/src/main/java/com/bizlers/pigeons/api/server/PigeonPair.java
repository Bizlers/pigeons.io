package com.bizlers.pigeons.api.server;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PigeonPair {

	private long appId;

	private long pigeonPairId;

	private Pigeon pigeon;

	private Pigeon mirrorPigeon;

	public PigeonPair() {
	}

	public PigeonPair(long appId, long pigeonPairId, Pigeon pigeon,
			Pigeon mirrorPigeon) {
		this.appId = appId;
		this.pigeonPairId = pigeonPairId;
		this.pigeon = pigeon;
		this.mirrorPigeon = mirrorPigeon;
	}

	public long getAppId() {
		return appId;
	}

	public void setAppId(long appId) {
		this.appId = appId;
	}

	public long getPigeonPairId() {
		return pigeonPairId;
	}

	public void setPigeonPairId(long pigeonPairId) {
		this.pigeonPairId = pigeonPairId;
	}

	public Pigeon getPigeon() {
		return pigeon;
	}

	public void setPigeon(Pigeon pigeon) {
		this.pigeon = pigeon;
	}

	public Pigeon getMirrorPigeon() {
		return mirrorPigeon;
	}

	public void setMirrorPigeon(Pigeon mirrorPigeon) {
		this.mirrorPigeon = mirrorPigeon;
	}

	public String toString() {
		return " Pigeon PairId : " + pigeonPairId + " Pigeon : " + pigeon
				+ " Mirror Pigeon : " + mirrorPigeon;
	}

}