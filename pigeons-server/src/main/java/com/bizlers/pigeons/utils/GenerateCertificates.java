package com.bizlers.pigeons.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.bizlers.pigeons.models.Application;
import com.jcabi.aspects.Loggable;

public enum GenerateCertificates {
	INSTANCE;

	private static final String CA_CERTIFICATE_PATH = "caCertificatePath";
	private static final String CERTIFICATE_GENERATION_PATH = "certificateGenerationPath";
	private static final String KEY_LENGTH = "keyLength";
	private static final String CAKEY_PASSWORD = "caKeyPassword";
	private static final String CLIENTKEY_PASSWORD = "clientKeyPassword";
	private static final String COUNTRY_NAME = "countryName";
	private static final String STATE_NAME = "stateName";
	private static final String CITY_NAME = "cityName";
	private static final String ORGANISATION_NAME = "organisationName";
	private static final String ORGANISATION_UNIT_NAME = "organisationUnitName";
	private static final String COMMON_NAME = "commonName";
	private static final String CLIENT_KEY = "client.key";
	private static final String CLIENT_CSR = "client.csr";
	private static final String CLIENT_CERTIFICATE = "client.crt";
	private static final String CA_KEY = "ca.key";
	private static final String CA_CERTIFICATE = "ca.crt";
	private static final String KEYSTORE_NAME = "keystore.jks";
	private static final String PKCIS_NAME = "client.p12";
	private static final String SCRIPTS_PATH = "scriptsPath";
	private String caCertificatePath;
	private String certificateGenerationPath;
	private String keyLength;
	private String caKeyPassword;
	private String clientKeyPassword;
	private String countryName;
	private String stateName;
	private String cityName;
	private String organisationName;
	private String organisationUnitName;
	private String commonName;
	private String scriptsPath;

	@Loggable(value = Loggable.DEBUG)
	GenerateCertificates() {
		try {
			Properties properties = new Properties();
			InputStream inputStream = Thread.currentThread()
					.getContextClassLoader()
					.getResourceAsStream("security.properties");
			properties.load(inputStream);
			caCertificatePath = properties.getProperty(CA_CERTIFICATE_PATH);
			certificateGenerationPath = properties
					.getProperty(CERTIFICATE_GENERATION_PATH);
			keyLength = properties.getProperty(KEY_LENGTH);
			caKeyPassword = properties.getProperty(CAKEY_PASSWORD);
			clientKeyPassword = properties.getProperty(CLIENTKEY_PASSWORD);
			countryName = properties.getProperty(COUNTRY_NAME);
			stateName = properties.getProperty(STATE_NAME);
			cityName = properties.getProperty(CITY_NAME);
			organisationName = properties.getProperty(ORGANISATION_NAME);
			organisationUnitName = properties
					.getProperty(ORGANISATION_UNIT_NAME);
			commonName = properties.getProperty(COMMON_NAME);
			scriptsPath = properties.getProperty(SCRIPTS_PATH);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void generate(Application application, int days) {
		try {
			File file = new File(certificateGenerationPath + "/"
					+ application.getAppId());
			if (!file.isDirectory()) {
				file.mkdir();
			}
			String[] command = { "expect", scriptsPath + "/generate.exp",
					file.getAbsolutePath() + "/" + CLIENT_KEY, keyLength,
					clientKeyPassword,
					file.getAbsolutePath() + "/" + CLIENT_CSR, countryName,
					stateName, cityName, organisationName,
					organisationUnitName, commonName, application.getEmailId(),
					caCertificatePath + "/" + CA_CERTIFICATE,
					caCertificatePath + "/" + CA_KEY,
					file.getAbsolutePath() + "/" + CLIENT_CERTIFICATE,
					String.valueOf(days),
					file.getAbsolutePath() + "/" + PKCIS_NAME,
					file.getAbsolutePath() + "/" + KEYSTORE_NAME,
					caKeyPassword, scriptsPath + "/createpkcis.sh" };
			Runtime.getRuntime().exec(command);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
