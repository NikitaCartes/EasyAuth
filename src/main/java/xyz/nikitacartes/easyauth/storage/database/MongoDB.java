package xyz.nikitacartes.easyauth.storage.database;

import com.mongodb.MongoClientException;
import com.mongodb.MongoCommandException;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.InsertOneModel;
import net.minecraft.util.Uuids;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nikitacartes.easyauth.config.StorageConfigV1;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

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
            ConnectionString connString = new ConnectionString(config.mongodb.mongodbConnectionString);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .build();
            mongoClient = MongoClients.create(settings);
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
        LogDebug("Registering new player " + data.username + ": " + data.toJson());
        try {
            Document document = new Document("username", data.username)
                    .append("username_lower", data.usernameLowerCase)
                    .append("uuid", data.uuid == null ? null : data.uuid.toString())
                    .append("data", data.toJson())
                    .append("last_ip", data.lastIp);
            if (collection.insertOne(document).getInsertedId() == null) {
                LogError("Failed to insert data: " + data.toJson());
            }
        } catch (MongoCommandException e) {
            LogError("Failed to insert data: " + data.toJson(), e);
        }
    }

    public @Nullable PlayerEntryV1 getUserData(String username) {
        MongoCursor<Document> findIterable;
        try {
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
            LogDebug("Retrieved player data for " + username + ": " + (playerEntry != null ? playerEntry.toJson() : "null"));
            return playerEntry;
        } catch (Exception e) {
            LogError("Error retrieving user data for " + username, e);
            return null;
        }
    }

    public @NotNull PlayerEntryV1 getUserDataOrCreate(String username) {
        PlayerEntryV1 playerEntry = getUserData(username);
        if (playerEntry == null) {
            playerEntry = new PlayerEntryV1(username);
            registerUser(playerEntry);
        }
        return playerEntry;
    }

    public boolean deleteUserData(String username) {
        LogDebug("Deleting player data for " + username);
        try {
            if (collection.deleteOne(eq("username", username)).getDeletedCount() == 0) {
                LogError("Failed to delete data for username: " + username);
                return false;
            }
            return true;
        } catch (Exception e) {
            LogError("Error deleting user data", e);
            return false;
        }
    }

    @Override
    public boolean updateUserData(PlayerEntryV1 data) {
        LogDebug("Updating player data for " + data.username + ": " + data.toJson());
        try {
            Document document = new Document("username", data.username)
                    .append("username_lower", data.usernameLowerCase)
                    .append("uuid", data.uuid == null ? null : data.uuid.toString())
                    .append("data", data.toJson())
                    .append("last_ip", data.lastIp);
            if (collection.replaceOne(eq("username", data.username), document).getModifiedCount() == 0) {
                LogError("Failed to update data: " + data.toJson());
                return false;
            }
            return true;
        } catch (Exception e) {
            LogError("Error updating user data: " + data.toJson(), e);
            return false;
        }
    }

    @Override
    public HashMap<String, PlayerEntryV1> getAllData() {
        HashMap<String, PlayerEntryV1> registeredPlayers = new HashMap<>();
        try {
            collection.find().forEach(document -> {
                String username = document.getString("username");
                if (username == null) return;
                String username_lower = document.getString("username_lower");
                String uuid = document.getString("uuid");
                String data = document.getString("data");
                registeredPlayers.put(username, new PlayerEntryV1(username, username_lower, uuid, data));
            });
        } catch (Exception e) {
            LogError("Error retrieving all user data", e);
        }
        return registeredPlayers;
    }

    @Override
    public int countAccountsByIp(String ipAddress) {
        try {
            long count = collection.countDocuments(eq("last_ip", ipAddress));
            LogDebug("Counted " + count + " accounts for IP " + ipAddress);
            return (int) count;
        } catch (Exception e) {
            LogError("Error counting accounts by IP", e);
            return 0;
        }
    }

    @Override
    public List<String> getUsernamesByIp(String ipAddress) {
        List<String> usernames = new ArrayList<>();
        try {
            collection.find(eq("last_ip", ipAddress)).forEach(document -> {
                String username = document.getString("username");
                if (username != null) {
                    usernames.add(username);
                }
            });
            LogDebug("Found " + usernames.size() + " usernames for IP " + ipAddress);
        } catch (Exception e) {
            LogError("Error getting usernames by IP", e);
        }
        return usernames;
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
    public void migrateFromV4() {
        LogInfo("Migrating IPs from JSON to field...");
        try {
            for (Document document : collection.find()) {
                String data = document.getString("data");
                if (data != null) {
                    // Create dummy entry to parse JSON
                    PlayerEntryV1 entry = new PlayerEntryV1(document.getString("username"), document.getString("username_lower"), null, data);
                    collection.updateOne(eq("_id", document.get("_id")), new Document("$set", new Document("last_ip", entry.lastIp)));
                }
            }
            LogInfo("Migrated IPs successfully.");
        } catch (Exception e) {
            LogError("Error migrating IPs", e);
        }
    }

}
