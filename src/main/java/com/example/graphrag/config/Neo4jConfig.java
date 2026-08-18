package com.example.graphrag.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class Neo4jConfig {

    @Value("${spring.neo4j.uri:bolt://localhost:7688}")
    private String uri;

    @Value("${spring.neo4j.authentication.username:neo4j}")
    private String username;

    @Value("${spring.neo4j.authentication.password:password}")
    private String password;

    @Value("${spring.neo4j.pool.max-connection-pool-size:50}")
    private int maxConnectionPoolSize;

    @Value("${spring.neo4j.pool.connection-acquisition-timeout-ms:60000}")
    private long connectionAcquisitionTimeoutMs;

    @Value("${spring.neo4j.pool.idle-time-before-connection-test-ms:30000}")
    private long idleTimeBeforeConnectionTestMs;

    @Bean
    @ConditionalOnMissingBean
    public Driver neo4jDriver() {
        Config config = Config.builder()
                .withMaxConnectionPoolSize(maxConnectionPoolSize)
                .withConnectionAcquisitionTimeout(connectionAcquisitionTimeoutMs, TimeUnit.MILLISECONDS)
                .withConnectionTimeout(30, TimeUnit.SECONDS)
                .withMaxConnectionLifetime(10, TimeUnit.MINUTES)
                .withConnectionLivenessCheckTimeout(idleTimeBeforeConnectionTestMs, TimeUnit.MILLISECONDS)
                .withLogging(Logging.slf4j())
                .build();

        return GraphDatabase.driver(uri, AuthTokens.basic(username, password), config);
    }
}
