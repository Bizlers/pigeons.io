package com.bizlers.pigeons.api.server;

/**
 * @author saurabh
 * 
 */
public abstract class PigeonListener {
	
	public int id;
	
	public PigeonListener(int id) {
		this.id = id;
	}

	public abstract void clientConnected(long pigeonId);

	public abstract void clientDisconnected(long pigeonId,String status);

	public abstract void messageDelivered(long messageId, long pigeonId);

	public abstract void messageLost(long messageId, long pigeonId);

	public abstract void connectionLost();
}
