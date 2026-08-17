package com.gii.api.config;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Profile("test")
@EnableTransactionManagement
public class PostgresConfig {

  @Bean
  public DataSource dataSource() {
    return new AbstractDataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        throw new SQLException("Local fake DataSource: JDBC disabled");
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLException("Local fake DataSource: JDBC disabled");
      }
    };
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
    emf.setDataSource(dataSource);
    emf.setPersistenceUnitName("default");
    emf.setPackagesToScan("com.gii.common.entity");
    emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    emf.setPersistenceProvider(new HibernatePersistenceProvider());

    Properties jpaProperties = new Properties();
    jpaProperties.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
    jpaProperties.setProperty("jakarta.persistence.database-product-name", "PostgreSQL");
    jpaProperties.setProperty("jakarta.persistence.database-major-version", "15");
    jpaProperties.setProperty("jakarta.persistence.database-minor-version", "0");
    jpaProperties.setProperty("hibernate.hbm2ddl.auto", "none");
    emf.setJpaProperties(jpaProperties);

    return emf;
  }

  @Bean
  public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }
}
