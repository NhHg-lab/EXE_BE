package com.teaverse.compensation.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.util.StringUtils;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    @Bean
    public MongoClient mongoClient(
            @Value("${app.mongo.host}") String host,
            @Value("${app.mongo.port}") int port,
            @Value("${app.mongo.root-username}") String username,
            @Value("${app.mongo.root-password}") String password,
            @Value("${app.mongo.auth-database}") String authDatabase
    ) {
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyToClusterSettings(settings -> settings
                        .hosts(List.of(new ServerAddress(host, port)))
                        .serverSelectionTimeout(5, TimeUnit.SECONDS))
                .applyToSocketSettings(settings -> settings
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS));

        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            builder.credential(MongoCredential.createCredential(username, authDatabase, password.toCharArray()));
        }

        return MongoClients.create(builder.build());
    }
}
