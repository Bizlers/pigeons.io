package com.bizlers.pigeons.api.server.internal;

import java.util.HashMap;
import java.util.Map;

import com.bizlers.pigeons.api.server.persistent.models.PigeonInfo;

public class MemoryPigeonStore implements PigeonStore {

	private Map<Long, PigeonInfo> pigeons;

	public MemoryPigeonStore() {
		pigeons = new HashMap<Long, PigeonInfo>();
	}

	@Override
	public synchronized void insert(PigeonInfo pigeon) {
		pigeons.put(pigeon.getPigeonId(), pigeon);
	}

	@Override
	public synchronized void update(PigeonInfo pigeon) {
		pigeons.put(pigeon.getPigeonId(), pigeon);
	}

	@Override
	public synchronized PigeonInfo get(long id) {
		return pigeons.get(id);
	}

	@Override
	public synchronized void delete(long id) {
		pigeons.remove(id);
	}
}
