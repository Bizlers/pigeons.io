package com.bizlers.pigeons.agent.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

import com.jcabi.aspects.Loggable;

public class BrokerSSLConnector {

	private static final String FORMAT_JKS = "JKS";

	private static final String FORMAT_PKIX = "PKIX";

	public BrokerSSLConnector() {

	}

	@Loggable(value = Loggable.DEBUG)
	public static SSLSocketFactory getSocketFactory(
			final String caCertificateFile, final String clientCertificateFile,
			final String keyFile, final String password, final String tlsVersion)
			throws PigeonAgentException, IOException, GeneralSecurityException {
		SSLContext context = null;
		Security.addProvider(new BouncyCastleProvider());

		// Load CA certificate
		PEMReader reader = new PEMReader(new InputStreamReader(
				new ByteArrayInputStream(
						FileUtils.readFileToByteArray(new File(
								caCertificateFile)))));
		X509Certificate caCertificate = (X509Certificate) reader.readObject();
		reader.close();

		// Load client certificate
		reader = new PEMReader(
				new InputStreamReader(new ByteArrayInputStream(
						FileUtils.readFileToByteArray(new File(
								clientCertificateFile)))));
		X509Certificate clientCertificate = (X509Certificate) reader
				.readObject();
		reader.close();

		// Load client private key
		reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(
				FileUtils.readFileToByteArray(new File(keyFile)))),
				new PasswordFinder() {
					public char[] getPassword() {
						return password.toCharArray();
					}
				});
		KeyPair key = (KeyPair) reader.readObject();
		reader.close();

		// CA certificate is used to authenticate server
		KeyStore caKeyStore = KeyStore.getInstance(FORMAT_JKS);
		caKeyStore.load(null, null);
		caKeyStore.setCertificateEntry("ca-certificate", caCertificate);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(FORMAT_PKIX);
		tmf.init(caKeyStore);

		// Client key and certificates are sent to server so it can
		// authenticate client
		KeyStore keyStore = KeyStore.getInstance(FORMAT_JKS);
		keyStore.load(null, null);
		keyStore.setCertificateEntry("certificate", clientCertificate);
		keyStore.setKeyEntry("private-key", key.getPrivate(),
				password.toCharArray(),
				new java.security.cert.Certificate[] { clientCertificate });
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(FORMAT_PKIX);
		kmf.init(keyStore, password.toCharArray());

		// Finally, create SSL socket factory
		context = SSLContext.getInstance(tlsVersion);
		context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return context.getSocketFactory();
	}
}
