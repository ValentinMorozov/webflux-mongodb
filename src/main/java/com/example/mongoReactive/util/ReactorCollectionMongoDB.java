package com.example.mongoReactive.util;

import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.Document;

import javax.annotation.PostConstruct;

public class ReactorCollectionMongoDB extends ReactorRepositoryMongoDB {
    private MongoCollection<Document> collection;
    private String collectionName;

    public ReactorCollectionMongoDB (String collectionName) {
        super();
        this.collectionName = collectionName;
    }

    @PostConstruct
    public void init() throws ClassNotFoundException
    {
        this.collection = getMongoDatabase().getCollection(collectionName);
    }

    @Override
    public MongoCollection<Document> getCollection() {
        return collection;
    }
}
