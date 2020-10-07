package org.samo_lego.simpleauth.storage.database;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;


import static org.samo_lego.simpleauth.SimpleAuth.config;

public class MongoDB {
    private static MongoCollection<Document> collection;

    public static void initialize() {
        /*mongoClient = MongoClients.create(
                String.format(
                        "mongodb://%s:%s@%s:%s/?authSource=db1&ssl=%s",
                        config.mongoDBCredentials.username,
                        config.mongoDBCredentials.password,
                        config.mongoDBCredentials.host,
                        config.mongoDBCredentials.port,
                        config.mongoDBCredentials.useSsl

                )
        );*/
        MongoClient mongoClient = MongoClients.create(String.format("mongodb://%s:%s", config.mongoDBCredentials.host, config.mongoDBCredentials.port));
        MongoDatabase database = mongoClient.getDatabase(config.mongoDBCredentials.databaseName);
        collection = database.getCollection("players");
    }
}
