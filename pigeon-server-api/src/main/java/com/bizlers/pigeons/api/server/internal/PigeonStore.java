package com.bizlers.pigeons.api.server.internal;

import com.bizlers.pigeons.api.server.persistent.models.PigeonInfo;

public interface PigeonStore {

	public void insert(PigeonInfo pigeon);

	public void update(PigeonInfo pigeon);

	public void delete(long id);

	public PigeonInfo get(long id);
}
