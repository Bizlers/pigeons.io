package com.bizlers.pigeons.dao;

import java.util.List;

import com.bizlers.pigeons.models.Pigeon;
import com.bizlers.pigeons.utils.PigeonServerException;

public interface PigeonDAO {

	public void createPigeon(Pigeon pigeon);

	public int deleteAllPigeons(String brokerName);

	public Pigeon getPigeonbyClientId(String clientid)
			throws PigeonServerException;

	public void updatePigeon(Pigeon pigeon);

	public boolean deletePigeon(Pigeon pigeon);

	public Pigeon getPublishPigeon(long appId);

	public Pigeon getPublishPigeon(long appId, String region);

	public List<Pigeon> getCreatedPigeons(String type);

	public int getPigeonCount(String brokerName);

	public void activatePigeons(String clientId) throws PigeonServerException;

	public List<Pigeon> getPigeonsToDestroy(int seconds);

	public Pigeon getOldPigeon(String clientId) throws PigeonServerException;

	public List<Pigeon> getPigeonList(long agentId);

	public void deleteAllPigeonsOfAgent(long agentId);

	public Pigeon getPublishPigeons(long appId, String brokerName);

	public Pigeon getReplacePigeon(long appId, String clientId);

	public Pigeon getPigeon(long appId, String region, int redirectPort, boolean isRoundRobinFetch);

	public Pigeon getPigeon(long appId, int redirectPort, boolean isRoundRobinFetch);

}
