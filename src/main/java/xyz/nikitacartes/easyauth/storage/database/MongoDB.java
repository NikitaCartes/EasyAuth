package xyz.nikitacartes.easyauth.storage.database;

import com.mongodb.MongoClientException;
import com.mongodb.MongoCommandException;
import com.mongodb.client.*;
import com.mongodb.client.model.InsertOneModel;
import net.minecraft.util.Uuids;
import org.bson.Document;
import xyz.nikitacartes.easyauth.config.StorageConfigV1;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

public class MongoDB implements DbApi {
    private final StorageConfigV1 config;
    private MongoCollection<Document> collection;
    private MongoClient mongoClient;

    public MongoDB(StorageConfigV1 config) {
        this.config = config;
    }

    public void connect() throws DBApiException {
        LogDebug("You are using Mongo DB");
        try {
            mongoClient = MongoClients.create(config.mongodb.mongodbConnectionString);
            MongoDatabase database = mongoClient.getDatabase(config.mongodb.mongodbDatabase);
            collection = database.getCollection("players");
        } catch (MongoClientException | MongoCommandException e) {
            throw new DBApiException("Failed connecting to MongoDB", e);
        }
    }

    public void close() {
        mongoClient.close();
        LogInfo("Database connection closed successfully.");
        mongoClient = null;
        collection = null;
    }

    public boolean isClosed() { return mongoClient == null; }

    @Override
    public void registerUser(PlayerEntryV1 data) {
        Document document = new Document("username", data.username)
                .append("username_lower", data.usernameLowerCase)
                .append("uuid", data.uuid)
                .append("data", data.toJson());
        try {
            collection.insertOne(document);
        } catch (MongoCommandException e) {
            LogError("Failed to insert data into MongoDB: " + data, e);
        }
    }

    public @Nullable PlayerEntryV1 getUserData(String username) {
        MongoCursor<Document> findIterable;
        if (extendedConfig.allowCaseInsensitiveUsername) {
            findIterable = collection.find(eq("username", username)).iterator();
        } else {
            findIterable = collection.find(eq("username_lower", username.toLowerCase(Locale.ENGLISH))).iterator();
        }
        PlayerEntryV1 playerEntry = null;
        if (findIterable.hasNext()) {
            Document document = findIterable.next();
            playerEntry = new PlayerEntryV1(document.getString("username"),
                                            document.getString("username_lower"),
                                            document.getString("uuid"),
                                            document.getString("data"));
        }
        while (findIterable.hasNext()) {
            Document document = findIterable.next();
            String dbUsername = document.getString("username");
            if (dbUsername.equals(username)) {
                playerEntry = new PlayerEntryV1(dbUsername,
                                                document.getString("username_lower"),
                                                document.getString("uuid"),
                                                document.getString("data"));
                break;
            }
        }
        return playerEntry;
    }

    public @Nonnull PlayerEntryV1 getUserDataOrCreate(String username) {
        PlayerEntryV1 playerEntry = getUserData(username);
        if (playerEntry == null) {
            playerEntry = new PlayerEntryV1(username);
            registerUser(playerEntry);
        }
        return playerEntry;
    }

    public void deleteUserData(String username) {
        collection.deleteOne(eq("username", username));
    }

    @Override
    public void updateUserData(PlayerEntryV1 data) {
        Document document = new Document("username", data.username)
                .append("username_lower", data.usernameLowerCase)
                .append("uuid", data.uuid)
                .append("data", data.toJson());
        collection.replaceOne(eq("username", data.username), document);
    }

    @Override
    public HashMap<String, PlayerEntryV1> getAllData() {
        HashMap<String, PlayerEntryV1> registeredPlayers = new HashMap<>();
        collection.find().forEach(document -> {
            String username = document.getString("username");
            if (username == null) return;
            String username_lower = document.getString("username_lower");
            String uuid = document.getString("uuid");
            String data = document.getString("data");
            registeredPlayers.put(username, new PlayerEntryV1(username, username_lower, uuid, data));
        });
        return registeredPlayers;
    }

    @Override
    public void migrateFromV1(HashMap<String, String> userCache) {
        List<InsertOneModel<Document>> writeList = new ArrayList<>();
        userCache.forEach((username, uuid) -> {
            MongoCursor<Document> findIterable;
            String data = null;

            findIterable = collection.find(eq("UUID", uuid)).iterator();
            if (findIterable.hasNext()) {
                data = findIterable.next().toJson();
            } else {
                String lowerCaseUsername = username.toLowerCase(Locale.ENGLISH);
                String lowerCaseUuid = Uuids.getOfflinePlayerUuid(lowerCaseUsername).toString();
                findIterable = collection.find(eq("UUID", lowerCaseUuid)).iterator();
                if (findIterable.hasNext()) {
                    data = findIterable.next().toJson();
                }
            }
            if (data != null) {
                PlayerEntryV1 playerEntry = migrateFromV1(data, username);
                writeList.add(new InsertOneModel<>(new Document("username", playerEntry.username)
                        .append("username_lower", playerEntry.usernameLowerCase)
                        .append("uuid", playerEntry.uuid)
                        .append("data", playerEntry.toJson())));
            }
        });
        if (!writeList.isEmpty()) collection.bulkWrite(writeList);
    }

    @Override
    public int executeRawUpdate(String sql) throws DBApiException {
        throw new DBApiException("MongoDB does not support SQL queries", null);
    }
    
    @Override
    public Connection getConnection() throws DBApiException {
        throw new DBApiException("MongoDB does not provide SQL connections", null);
    }
    
    @Override
    public String getDatabaseType() {
        return "mongodb";
    }

}
