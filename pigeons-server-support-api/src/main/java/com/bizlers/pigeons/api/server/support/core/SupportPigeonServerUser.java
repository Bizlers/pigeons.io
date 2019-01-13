package com.bizlers.pigeons.api.server.support.core;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.bizlers.pigeons.api.server.DefaultPigeonServerUser;
import com.bizlers.pigeons.api.server.PigeonListener;
import com.bizlers.pigeons.api.server.PigeonServerException;
import com.bizlers.pigeons.api.server.PigeonServerUser;
import com.bizlers.pigeons.api.server.persistent.models.PigeonV2;
import com.bizlers.pigeons.api.server.support.persistent.dao.PigeonUsageStore;
import com.bizlers.pigeons.api.server.support.persistent.models.PigeonUsage;
import com.jcabi.aspects.Loggable;

public class SupportPigeonServerUser implements PigeonServerUser {

	private PigeonUsageStore pigeonUsageStore = new PigeonUsageStore();

	private PigeonServerUser pigeonServerUser;

	private int pigeonMaxLimit;

	private long pigeonResetInterval;

	@Loggable(value = Loggable.DEBUG, limit = 5, unit = TimeUnit.SECONDS)
	public SupportPigeonServerUser(SupportPigeonServerUserConfig config) throws PigeonServerException {
		pigeonServerUser = DefaultPigeonServerUser.fromConfig(config);
		this.pigeonMaxLimit = config.getPigeonMaxLimit();
		this.pigeonResetInterval = config.getPigeonResetInterval();
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public PigeonV2 getPigeon(long userId) throws PigeonServerException {
		return getPigeon("", 0, userId);
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public PigeonV2 getPigeon(String region, long userId)
			throws PigeonServerException {
		return getPigeon(region, 0, userId);
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public PigeonV2 getPigeon(int port, long userId)
			throws PigeonServerException {
		return getPigeon("", port, userId);
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public PigeonV2 getPigeon(String region, int port, long userId)
			throws PigeonServerException {
		if (isPigeonRequestValid(userId)) {
			PigeonV2 pigeon = pigeonServerUser.getPigeon(region, port);
			if (pigeon != null) {
				storePigeon(pigeon, userId);
				return pigeon;
			}
		}
		return null;
	}

	private void storePigeon(PigeonV2 pigeon, long userId) {
		PigeonUsage pigeonUsage = pigeonUsageStore.getPigeonUsage(userId);
		if (pigeonUsage != null) {
			if (pigeonUsage.getPigeonCount() < pigeonMaxLimit) {
				pigeonUsage.setPigeonCount(pigeonUsage.getPigeonCount() + 1);
			} else {
				pigeonUsage.setPigeonCount(1);
			}
			pigeonUsage.setPigeonId(pigeon.getId());
			pigeonUsage.setRetrievedTime(new Date());
			pigeonUsageStore.updatePigeonUsage(pigeonUsage);
		} else {
			pigeonUsageStore.createPigeonUsage(new PigeonUsage(userId, pigeon
					.getId(), 1, new Date()));
		}
	}

	private boolean isPigeonRequestValid(long userId) {
		PigeonUsage pigeonUsage = pigeonUsageStore.getPigeonUsage(userId);
		if (pigeonUsage != null) {
			long timeDiferenceInMinutes = (new Date().getTime() - pigeonUsage
					.getRetrievedTime().getTime()) / (1000 * 60);
			if (timeDiferenceInMinutes < pigeonResetInterval
					&& pigeonUsage.getPigeonCount() >= pigeonMaxLimit) {
				return false;
			}
		}
		return true;
	}

	public void disconnect() throws PigeonServerException {
		pigeonServerUser.disconnect();
	}

	public boolean isConnected() {
		return pigeonServerUser.isConnected();
	}

	public long sendMessageWithoutAck(long pigeonId, String message)
			throws PigeonServerException {
		return pigeonServerUser.sendMessageWithoutAck(pigeonId, message);
	}

	public long sendMessage(long pigeonId, String message)
			throws PigeonServerException {
		return pigeonServerUser.sendMessage(pigeonId, message);
	}

	public void publish(String topic, String message)
			throws PigeonServerException {
		pigeonServerUser.publish(topic, message);
	}

	@Override
	public PigeonV2 getPigeon() throws PigeonServerException {
		return pigeonServerUser.getPigeon();
	}

	@Override
	public PigeonV2 getPigeon(int port) throws PigeonServerException {
		return pigeonServerUser.getPigeon(port);
	}

	@Override
	public PigeonV2 getPigeon(String region, int port)
			throws PigeonServerException {
		return pigeonServerUser.getPigeon(region, port);
	}

	@Override
	public void setPigeonListener(PigeonListener pigeonListener) {
		pigeonServerUser.setPigeonListener(pigeonListener);
	}

	@Override
	public void connect() throws PigeonServerException {
		pigeonServerUser.connect();
	}

	@Override
	public void connect(boolean waitForConnection) throws PigeonServerException {
		pigeonServerUser.connect(waitForConnection);
	}
}
