package com.bizlers.pigeons.api.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

public class BrokerSSLConnector {
	private static final String FORMAT_JKS = "JKS";

	public BrokerSSLConnector() {

	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public static SSLSocketFactory getSocketFactory(final String keyStorePath,
			final String storePassword, final String tlsVersion)
			throws PigeonServerException {
		SSLContext context = null;
		try {
			// Load keystore.
			InputStream storeInputStream = new FileInputStream(keyStorePath);
			KeyStore keyStore = KeyStore.getInstance(FORMAT_JKS);
			keyStore.load(storeInputStream, storePassword.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore);
			KeyManagerFactory kmf = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, storePassword.toCharArray());

			// Finally, create SSL socket factory.
			context = SSLContext.getInstance(tlsVersion);
			context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		} catch (GeneralSecurityException e) {
			String errorMessage = "Failed to get SocketFactory. ";
			Logger.error(BrokerSSLConnector.class, errorMessage
					+ "Exception %[exception]s", e);
			throw new PigeonServerException(errorMessage, e);
		} catch (IOException e) {
			String errorMessage = "Failed to get SocketFactory. ";
			Logger.error(BrokerSSLConnector.class, errorMessage
					+ "Exception %[exception]s", e);
			throw new PigeonServerException(errorMessage, e);
		}
		return context.getSocketFactory();
	}
}
