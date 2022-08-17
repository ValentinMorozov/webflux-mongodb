package com.example.mongoReactive.repository;

import com.example.mongoReactive.config.MongoConfig;
import com.example.mongoReactive.util.ReactorCollectionMongoDB;
import org.springframework.stereotype.Service;

@Service
public class CheckRepository extends ReactorCollectionMongoDB {

    CheckRepository(MongoConfig mongoConfig) {
        super(mongoConfig.getCollectionName());
    }
}
