package xyz.nikitacartes.easyauth.storage.database;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import org.bson.Document;
import xyz.nikitacartes.easyauth.storage.PlayerCache;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.logError;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.logInfo;

public class MongoDB implements DbApi {
    private final MongoCollection<Document> collection;
    private final MongoClient mongoClient;

    public MongoDB() {
        mongoClient = MongoClients.create(URLEncoder.encode(config.main.MongoDBConnectionString, StandardCharsets.UTF_8));
        MongoDatabase database = mongoClient.getDatabase(config.main.MongoDBDatabase);
        collection = database.getCollection("players");
    }

    public void close() {
        mongoClient.close();
        logInfo("Database connection closed successfully.");
    }

    public boolean isClosed() {
        return mongoClient == null;
    }

    @Override
    public boolean registerUser(String uuid, String data) {
        logError("RegisterUser isn't implemented in MongoDB");
        return false;
    }

    public boolean isUserRegistered(String uuid) {
        return collection.find(eq("UUID", uuid)).iterator().hasNext();
    }

    public void deleteUserData(String uuid) {
        collection.deleteOne(eq("UUID", uuid));
    }

    @Override
    public void updateUserData(String uuid, String data) {
        logError("updateUserData isn't implemented in MongoDB");
    }

    public String getUserData(String uuid) {
        if (isUserRegistered(uuid)) {
            Document data = collection.find(eq("UUID", uuid)).iterator().next();
            return data.toJson();
        }
        return "";
    }

    public void saveAll(HashMap<String, PlayerCache> playerCacheMap) {
        List<InsertOneModel<Document>> writeList = new ArrayList<>();
        List<ReplaceOneModel<Document>> updateList = new ArrayList<>();
        playerCacheMap.forEach((uuid, playerCache) -> {
            // Save as BSON not JSON stringified
            if (!isUserRegistered(uuid)) {
                writeList.add(new InsertOneModel<>(new Document("UUID", uuid).append("password", playerCache.password)));
            } else {
                updateList.add(new ReplaceOneModel<>(eq("UUID", uuid), new Document("UUID", uuid).append("password", playerCache.password).append("is_authenticated", playerCache.isAuthenticated).append("last_ip", playerCache.lastIp).append("valid_until", playerCache.validUntil).append("last_kicked", playerCache.lastKicked)));
            }
        });
        if (!writeList.isEmpty()) collection.bulkWrite(writeList);
        if (!updateList.isEmpty()) collection.bulkWrite(updateList);
    }
}
