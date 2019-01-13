package com.bizlers.pigeons.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

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

	private static final String CA_CERTIFICATE_FILE = "caCertificateFile";
	private static final String KEY_FILE = "keyFile";
	private static final String CLIENT_CERTIFICATE_FILE = "clientCertificateFile";
	private static final String BROKER_PASSWORD = "brokerPassword";
	private static final String TLS_VERSION = "tlsVersion";

	@Loggable(value = Loggable.DEBUG)
	public static SSLSocketFactory getSocketFactory()
			throws PigeonServerException {
		SSLContext context = null;
		try {
			Security.addProvider(new BouncyCastleProvider());
			Properties properties = new Properties();
			InputStream inputStream = Thread.currentThread()
					.getContextClassLoader()
					.getResourceAsStream("brokerssl.properties");
			properties.load(inputStream);
			String CaFile = properties.getProperty(CA_CERTIFICATE_FILE);
			String CertificateFile = properties
					.getProperty(CLIENT_CERTIFICATE_FILE);
			String keyFile = properties.getProperty(KEY_FILE);
			final String mosquittoPassword = properties
					.getProperty(BROKER_PASSWORD);
			String tlsVersion = properties.getProperty(TLS_VERSION);
			// Load CA certificate.
			PEMReader reader = new PEMReader(new InputStreamReader(
					new ByteArrayInputStream(
							FileUtils.readFileToByteArray(new File(CaFile)))));
			X509Certificate caCert = (X509Certificate) reader.readObject();
			reader.close();

			// Load Client certificate.
			reader = new PEMReader(new InputStreamReader(
					new ByteArrayInputStream(
							FileUtils.readFileToByteArray(new File(
									CertificateFile)))));
			X509Certificate cert = (X509Certificate) reader.readObject();
			reader.close();

			// Load Client private key.
			reader = new PEMReader(new InputStreamReader(
					new ByteArrayInputStream(
							FileUtils.readFileToByteArray(new File(keyFile)))),
					new PasswordFinder() {
						public char[] getPassword() {
							return mosquittoPassword.toCharArray();
						}
					});
			KeyPair key = (KeyPair) reader.readObject();
			reader.close();

			// CA certificate is used to authenticate server.
			KeyStore caKeyStore = KeyStore.getInstance("JKS");
			caKeyStore.load(null, null);
			caKeyStore.setCertificateEntry("ca-certificate", caCert);
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance("PKIX");
			trustManagerFactory.init(caKeyStore);

			// Client key and certificates are sent to server so it can
			// authenticate
			// client.
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(null, null);
			ks.setCertificateEntry("certificate", cert);
			ks.setKeyEntry("private-key", key.getPrivate(),
					mosquittoPassword.toCharArray(),
					new java.security.cert.Certificate[] { cert });
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance("PKIX");
			keyManagerFactory.init(ks, mosquittoPassword.toCharArray());

			// Finally, create SSL socket factory.
			context = SSLContext.getInstance(tlsVersion);
			context.init(keyManagerFactory.getKeyManagers(),
					trustManagerFactory.getTrustManagers(), null);
		} catch (UnrecoverableKeyException e) {
			throw new PigeonServerException(e);
		} catch (KeyManagementException e) {
			throw new PigeonServerException(e);
		} catch (KeyStoreException e) {
			throw new PigeonServerException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new PigeonServerException(e);
		} catch (CertificateException e) {
			throw new PigeonServerException(e);
		} catch (IOException e) {
			throw new PigeonServerException(e);
		}
		return context.getSocketFactory();
	}
}
