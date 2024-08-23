package com.techelevator.dao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;

@Configuration
@ComponentScan(basePackages = {"com.techelevator.tenmo.dao", "com.techelevator.dao"})
public class TestingDatabaseConfig {

    private static final String DB_HOST =
            Objects.requireNonNullElse(System.getenv("DB_HOST"), "localhost");
    private static final String DB_PORT =
            Objects.requireNonNullElse(System.getenv("DB_PORT"), "5432");
    private static final String DB_NAME =
            Objects.requireNonNullElse(System.getenv("DB_NAME"), "tenmo");
    private static final String DB_USERNAME =
            Objects.requireNonNullElse(System.getenv("DB_USERNAME"), "postgres");
    private static final String DB_PASSWORD =
            Objects.requireNonNullElse(System.getenv("DB_PASSWORD"), "postgres1");

    private SingleConnectionDataSource adminDataSource;
    private JdbcTemplate adminJdbcTemplate;

    @PostConstruct
    public void setup() {
        if (System.getenv("DB_HOST") == null) {
            adminDataSource = new SingleConnectionDataSource();
            adminDataSource.setUrl("jdbc:postgresql://localhost:5432/postgres");
            adminDataSource.setUsername("postgres");
            adminDataSource.setPassword("postgres1");
            adminJdbcTemplate = new JdbcTemplate(adminDataSource);
            adminJdbcTemplate.update("DROP DATABASE IF EXISTS \"" + DB_NAME + "\";");
            adminJdbcTemplate.update("CREATE DATABASE \"" + DB_NAME + "\";");
        }
    }

    @Bean
    public DataSource dataSource() throws SQLException {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setUrl(String.format("jdbc:postgresql://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME));
        dataSource.setUsername(DB_USERNAME);
        dataSource.setPassword(DB_PASSWORD);
        dataSource.setAutoCommit(false);

        ScriptUtils.executeSqlScript(dataSource.getConnection(), new ClassPathResource("test-data.sql"));

        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @PreDestroy
    public void cleanup() {
        if (adminDataSource != null) {
            adminJdbcTemplate.update("DROP DATABASE \"" + DB_NAME + "\";");
            adminDataSource.destroy();
        }
    }
}
