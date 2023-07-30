package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.nikitacartes.easyauth.storage.database.MongoDB;
import xyz.nikitacartes.easyauth.storage.database.MySQL;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;

@ConfigSerializable
public class StorageConfigV1 extends Config {
    public String databaseType = "leveldb";
    public MySqlConfig mySqlConfig = new MySqlConfig();
    public MongoDBConfig mongoDBConfig = new MongoDBConfig();
    public boolean useSimpleAuthDb = false;

    public static StorageConfigV1 load() {
        return loadConfig(StorageConfigV1.class, "storage.conf");
    }

    public static StorageConfigV1 create() {
        return createConfig(StorageConfigV1.class);
    }

    protected String getConfigPath() {
        return "main.conf";
    }

    protected String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("databaseType", databaseType);
        configValues.put("mySql.host", mySqlConfig.mysqlHost);
        configValues.put("mySql.user", mySqlConfig.mysqlUser);
        configValues.put("mySql.password", mySqlConfig.mysqlPassword);
        configValues.put("mySql.database", mySqlConfig.mysqlDatabase);
        configValues.put("mySql.table", mySqlConfig.mysqlTable);
        configValues.put("mongoDB.connectionString", mongoDBConfig.mongodbConnectionString);
        configValues.put("mongoDB.database", mongoDBConfig.mongodbDatabase);
        configValues.put("useSimpleAuthDb", useSimpleAuthDb);
        String configTemplate = Resources.toString(getResource("config/" + getConfigPath()), UTF_8);
        return new StringSubstitutor(configValues).replace(configTemplate);
    }

    @ConfigSerializable
    public static class MySqlConfig {
        public String mysqlHost = "localhost";
        public String mysqlUser = "root";
        public String mysqlPassword = "password";
        public String mysqlDatabase = "easyauth";
        public String mysqlTable = "easyauth";
    }

    @ConfigSerializable
    public static class MongoDBConfig {
        public String mongodbConnectionString = "mongodb://username:password@host:port/?options";
        public String mongodbDatabase = "easyauth";
    }
}
