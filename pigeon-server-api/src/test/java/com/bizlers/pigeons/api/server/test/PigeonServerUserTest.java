package com.bizlers.pigeons.api.server.test;

import com.bizlers.pigeons.api.server.*;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class PigeonServerUserTest extends TestCase {

	private PigeonServerUser user;
	
	private static final int PIGEON_LISTENER_ID = 10;

	public void testConnect() {
		try {
			PigeonServerUserConfig config = PigeonServerUserConfig.Builder
					.createFromFile("pigeon_server_user.properties").build();
			user = DefaultPigeonServerUser.fromConfig(config);
			user.setPigeonListener(new MyPigeonListener(PIGEON_LISTENER_ID));
			user.connect();
			assert (user.isConnected());
		} catch (PigeonServerException e) {
			throw new AssertionFailedError();
		}
	}

	private class MyPigeonListener extends PigeonListener {

		public MyPigeonListener(int id) {
			super(id);
		}
		
		@Override
		public void clientConnected(long pigeonId) {
			System.out.println("Connected" + pigeonId);

		}

		@Override
		public void clientDisconnected(long pigeonId, String status) {
			System.out.println("Disconnected" + pigeonId);

		}

		@Override
		public void messageDelivered(long messageId, long pigeonId) {
			System.out.println("messageDelivered");

		}

		@Override
		public void messageLost(long messageId, long pigeonId) {
			System.out.println("messageLost");

		}

		@Override
		public void connectionLost() {
			System.out.println("messageLost");

		}
	}
}
