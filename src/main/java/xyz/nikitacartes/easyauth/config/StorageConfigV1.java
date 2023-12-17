package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;

@ConfigSerializable
public class StorageConfigV1 extends ConfigTemplate {
    public String databaseType = "leveldb";
    public MySqlConfig mysql = new MySqlConfig();
    public MongoDBConfig mongodb = new MongoDBConfig();
    public boolean useSimpleauthDb = false;

    public StorageConfigV1() {
        super("storage.conf");
    }

    public static StorageConfigV1 load() {
        StorageConfigV1 config = loadConfig(StorageConfigV1.class, "storage.conf");
        if (config == null) {
            config = new StorageConfigV1();
            config.save();
        }
        return config;
    }

    protected String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("databaseType", wrapIfNecessary(databaseType));
        configValues.put("mySql.host", wrapIfNecessary(mysql.mysqlHost));
        configValues.put("mySql.user", wrapIfNecessary(mysql.mysqlUser));
        configValues.put("mySql.password", wrapIfNecessary(mysql.mysqlPassword));
        configValues.put("mySql.database", wrapIfNecessary(mysql.mysqlDatabase));
        configValues.put("mySql.table", wrapIfNecessary(mysql.mysqlTable));
        configValues.put("mongoDB.connectionString", wrapIfNecessary(mongodb.mongodbConnectionString));
        configValues.put("mongoDB.database", wrapIfNecessary(mongodb.mongodbDatabase));
        configValues.put("useSimpleAuthDb", wrapIfNecessary(useSimpleauthDb));
        String configTemplate = Resources.toString(getResource("config/" + configPath), UTF_8);
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
