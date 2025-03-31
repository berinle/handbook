package com.trvcloud.handbook.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource, Environment env) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .locations("classpath:db/migration")
            .load();
        
        flyway.migrate();
        return flyway;
    }
} 