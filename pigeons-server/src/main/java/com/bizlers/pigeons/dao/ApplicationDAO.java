package com.bizlers.pigeons.dao;

import com.bizlers.pigeons.models.Application;
import com.bizlers.pigeons.utils.PigeonServerException;

public interface ApplicationDAO {

	public void create(Application application);

	public Application get(long appId) throws PigeonServerException;

	public Application get(String emailId) throws PigeonServerException;

	public Application getByAccountId(long accountId);

	public void update(Application application);

	public void delete(Application application);

}
