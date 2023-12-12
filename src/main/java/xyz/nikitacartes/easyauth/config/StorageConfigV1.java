package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;

@ConfigSerializable
public class StorageConfigV1 extends ConfigTemplate {
    public String databaseType = "leveldb";
    public MySqlConfig mySqlConfig = new MySqlConfig();
    public MongoDBConfig mongoDBConfig = new MongoDBConfig();
    public boolean useSimpleAuthDb = false;

    public StorageConfigV1() {
        super("storage.conf");
    }

    public static StorageConfigV1 load() {
        StorageConfigV1 config = loadConfig(StorageConfigV1.class, "storage.conf");
        return config != null ? config : new StorageConfigV1();
    }

    protected String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("databaseType", wrapIfNecessary(databaseType));
        configValues.put("mySql.host", wrapIfNecessary(mySqlConfig.mysqlHost));
        configValues.put("mySql.user", wrapIfNecessary(mySqlConfig.mysqlUser));
        configValues.put("mySql.password", wrapIfNecessary(mySqlConfig.mysqlPassword));
        configValues.put("mySql.database", wrapIfNecessary(mySqlConfig.mysqlDatabase));
        configValues.put("mySql.table", wrapIfNecessary(mySqlConfig.mysqlTable));
        configValues.put("mongoDB.connectionString", wrapIfNecessary(mongoDBConfig.mongodbConnectionString));
        configValues.put("mongoDB.database", wrapIfNecessary(mongoDBConfig.mongodbDatabase));
        configValues.put("useSimpleAuthDb", wrapIfNecessary(useSimpleAuthDb));
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
