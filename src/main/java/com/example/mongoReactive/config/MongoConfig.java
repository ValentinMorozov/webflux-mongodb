package com.example.mongoReactive.config;

import com.mongodb.reactivestreams.client.MongoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.example.mongoReactive.repository")
public class MongoConfig extends AbstractReactiveMongoConfiguration
{
    @Value("${local.mongo.port:27017}")
    private String port;

    @Value("${local.mongo.dbname}")
    private String dbName;

    @Value("${local.mongo.collection-name}")
    private String collectionName;

    public String getCollectionName() {
        return collectionName;
    }

    @Bean
    @Override
    public MongoClient reactiveMongoClient() {
        return MongoClients.create();
    }

    @Override
    protected String getDatabaseName() {
        return dbName;
    }

    @Bean
    MongoDatabase mongoDatabase() {
        return reactiveMongoClient().getDatabase(getDatabaseName());
    }
}