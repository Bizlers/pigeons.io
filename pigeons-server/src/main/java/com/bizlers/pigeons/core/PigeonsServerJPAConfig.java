package com.bizlers.pigeons.core;

import java.io.IOException;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mchange.v2.c3p0.DriverManagerDataSource;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:pigeons.properties")
public class PigeonsServerJPAConfig {
	
	@Value("${javax.persistence.jdbc.driver}")
	private String driverClassName;

	@Value("${javax.persistence.jdbc.url}")
	private String url;

	@Value("${javax.persistence.jdbc.user}")
	private String userName;

	@Value("${javax.persistence.jdbc.password}")
	private String password;
	
	@Bean
	public LocalContainerEntityManagerFactoryBean pigeonsServerEmf() throws IOException {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setPersistenceUnitName("com.bizlers.pigeons");
		em.setDataSource(pigeonsServerDataSource());
		em.setPackagesToScan("com.bizlers.pigeons.models");
		
		JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		em.setJpaProperties(hibernateProperties());

		return em;
	}

	@Bean
	public DataSource pigeonsServerDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClass(driverClassName);
		dataSource.setJdbcUrl(url);
		dataSource.setUser(userName);
		dataSource.setPassword(password);
		return dataSource;
	}

	@Bean(name="pigeonServerTxManager")
	public PlatformTransactionManager pigeonsServerTxManager(@Qualifier("pigeonsServerEmf") EntityManagerFactory emf) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(emf);
		return transactionManager;
	}

	@Bean
	public PersistenceExceptionTranslationPostProcessor pigeonsServerExceptionTranslation() {
		return new PersistenceExceptionTranslationPostProcessor();
	}

	@Bean
	public Properties hibernateProperties() throws IOException {
	    Properties authCommonsProperties = PropertiesLoaderUtils.loadProperties(
	        new ClassPathResource("/pigeons.properties"));
	    Properties props = new Properties();
	    for(Object key: authCommonsProperties.keySet()) {
	    	if(((String) key).startsWith("hibernate.")) {
	    		props.put(key, authCommonsProperties.get(key));
	    	}
	    }
	    return props;
	}

}
