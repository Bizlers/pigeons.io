package com.bizlers.pigeons.api.server;

import com.bizlers.pigeons.api.server.persistent.models.PigeonV2;

public interface PigeonServerUser {

	/**
	 * Sets the {@link PigeonListener} object to listen the client connection
	 * state and message delivery states
	 * 
	 * @param pigeonListener
	 */
	public void setPigeonListener(PigeonListener pigeonListener);

	/**
	 * Connect Method to connect to Pigeon system.
	 * 
	 * @throws PigeonServerException
	 */
	public void connect() throws PigeonServerException;
	
	/**
	 * Connect Method to connect to Pigeon System.
	 * 
	 * @param waitForConnection
	 *            time in millisecond the api will wait to setup connection to
	 *            the pigeons system.
	 * @throws PigeonServerException
	 */
	public void connect(boolean waitForConnection) throws PigeonServerException;
	
	/**
	 * Disconnect from the pigeons system.
	 * 
	 * @throws PigeonServerException
	 */
	public void disconnect() throws PigeonServerException;

	/**
	 * isConnected method to check whether connection is Active or deActive.
	 * 
	 * @return boolean.
	 */
	public boolean isConnected();

	public PigeonV2 getPigeon() throws PigeonServerException;

	public PigeonV2 getPigeon(int port) throws PigeonServerException;

	public PigeonV2 getPigeon(String region, int port)
			throws PigeonServerException;

	/**
	 * sendMessageWithoutAck method to send messsage to User with PigeonId . No
	 * acknowlegment of this message will be received
	 * 
	 * @param pigeonId
	 *            pigeonId.
	 * @param message
	 *            message.
	 * @return messageId.
	 * @throws PigeonServerException
	 */
	public long sendMessageWithoutAck(long pigeonId, String message)
			throws PigeonServerException;

	/**
	 * sendMessage method to send messsage to User with PigeonId . Acknowlegment
	 * of this message will be received
	 * 
	 * @param pigeonId
	 *            pigeonId.
	 * @param message
	 *            message.
	 * @return messageId.
	 * @throws PigeonServerException
	 */
	public long sendMessage(long pigeonId, String message)
			throws PigeonServerException;

	/**
	 * publish method to multicast messsage to all Users which are subscribe to
	 * given topic.
	 * 
	 * @param topic
	 *            topic.
	 * @param message
	 *            message.
	 * @throws PigeonServerException
	 */
	public void publish(String topic, String message)
			throws PigeonServerException;

}
