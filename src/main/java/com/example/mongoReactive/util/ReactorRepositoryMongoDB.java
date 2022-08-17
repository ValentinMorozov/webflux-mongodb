package com.example.mongoReactive.util;

import com.mongodb.MongoWriteException;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.Objects.isNull;

public class ReactorRepositoryMongoDB {
    private MongoDatabase mongoDatabase;

    @Autowired
    void setMongoDatabase(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    public MongoCollection<Document> getCollection() {
        return null;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public Mono<Document> save(String id, Document document)  throws IllegalObjectIdException {
        return save(idDocument(id), document);
    }

    public Mono<Document> save(Document key, Document document) {
        UpdateOptions options = new UpdateOptions().upsert(true);
        Document update = new Document("$set", document);
        return
            Mono.from(getCollection().updateOne(key, update, options))
                .onErrorResume(MongoWriteException.class, e ->
                        e.getCode() == 11000
                            ? Mono.from(getCollection().updateOne(key, update, options.upsert(false)))
                            : Mono.error(e)
                    )
                .map(r -> isNull(r.getUpsertedId())
                    ? new Document("Updated",key.get("_id").toString())
                    : new Document("Inserted", r.getUpsertedId().asObjectId().getValue().toString())
        );
    }

    public Flux<Document> findById(String id)  throws IllegalObjectIdException {
        return Flux.from(getCollection().find(idDocument(id) ));
    }
    public Flux<Document> find(Document expression) {
        return Flux.from(getCollection().find(expression));
    }

    public Flux<Document> findAll() {
        return Flux.from(getCollection().find());
    }

    private Document idDocument(String id) throws IllegalObjectIdException {
        try {
            return new Document("_id",
                    id.isEmpty()
                            ? new ObjectId()
                            : new ObjectId(id));
        }
        catch(Exception e) {
            throw new IllegalObjectIdException("Error", e);
        }
    }
}
