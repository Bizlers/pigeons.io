package com.bizlers.pigeons.api.server.support.core;

import com.bizlers.pigeons.api.server.PigeonServerUserConfig;

public class SupportPigeonServerUserConfig extends PigeonServerUserConfig {

	private int pigeonMaxLimit;

	private long pigeonResetInterval;

	public SupportPigeonServerUserConfig(PigeonServerUserConfig config,
			int pigeonMaxLimit, long pigeonResetInterval) {
		super(config);
		this.pigeonMaxLimit = pigeonMaxLimit;
		this.pigeonResetInterval = pigeonResetInterval;
	}

	public int getPigeonMaxLimit() {
		return pigeonMaxLimit;
	}

	public long getPigeonResetInterval() {
		return pigeonResetInterval;
	}
}
