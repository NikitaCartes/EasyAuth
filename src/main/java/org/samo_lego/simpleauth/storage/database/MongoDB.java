package org.samo_lego.simpleauth.storage.database;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import org.bson.Document;
import org.samo_lego.simpleauth.storage.PlayerCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static org.samo_lego.simpleauth.SimpleAuth.config;

public class MongoDB {
    private static MongoCollection<Document> collection;
    private static MongoClient mongoClient;

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
        mongoClient = MongoClients.create(String.format("mongodb://%s:%d", config.mongoDBCredentials.host, config.mongoDBCredentials.port));
        MongoDatabase database = mongoClient.getDatabase(config.mongoDBCredentials.databaseName);
        collection = database.getCollection("players");
    }

    public static boolean isUserRegistered(String uuid) {
        return collection.find(eq("UUID", uuid)).iterator().hasNext();
    }

    public static void deleteUserData(String uuid) {
        collection.deleteOne(eq("UUID", uuid));
    }

    public static String getUserData(String uuid){
        if(isUserRegistered(uuid)) {
            Document data = collection.find(eq("UUID", uuid)).iterator().next();
            return data.toJson();
        }
        return "";
    }

    public static void saveFromCache(HashMap<String, PlayerCache> playerCacheMap) {
        List<InsertOneModel<Document>> writeList = new ArrayList<>();
        List<ReplaceOneModel<Document>> updateList = new ArrayList<>();
        playerCacheMap.forEach((uuid, playerCache) -> {
            // Save as BSON not JSON stringified
            Document lastLocation = new Document("x", playerCache.lastX)
                    .append("y", playerCache.lastY)
                    .append("z", playerCache.lastZ)
                    .append("dimension", playerCache.lastDim);

            if(!isUserRegistered(uuid)) {
                writeList.add(new InsertOneModel<>(
                        new Document("UUID", uuid)
                            .append("password", playerCache.password)
                            .append("lastLocation", lastLocation)
                        )
                );
            }
            else {
                updateList.add(new ReplaceOneModel<>(eq("UUID", uuid),
                        new Document("UUID", uuid)
                                .append("password", playerCache.password)
                                .append("lastLocation", lastLocation)
                        )
                );
            }
        });
        if(!writeList.isEmpty())
            collection.bulkWrite(writeList);
        if(!updateList.isEmpty())
            collection.bulkWrite(updateList);


    }

    public static boolean close() {
        mongoClient.close();
        return true;
    }

    public static boolean isClosed() {
        return mongoClient == null;
    }
}
