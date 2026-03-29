package xyz.nikitacartes.easyauth.config;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.storage.database.*;

@ConfigSerializable
public class StorageConfigV1 extends ConfigTemplate {

    @Comment("""
            Database type. Can be sqlite, mysql, mongodb or postgresql. SQLite is set by default.""")
    public String databaseType = "sqlite";

    @Comment("""
            
            SQLite configuration.""")
    public SQLiteConfig sqlite = new SQLiteConfig();

    @Comment("""
            
            MySQL configuration.""")
    public MySqlConfig mysql = new MySqlConfig();

    @Comment("""
            
            MongoDB configuration.""")
    public MongoDBConfig mongodb = new MongoDBConfig();

    @Comment("""
            
            PostgreSQL configuration.""")
    public PostgreSQLConfig postgresql = new PostgreSQLConfig();

    public StorageConfigV1() {
        super("storage.conf", """
                ##                          ##
                ##         EasyAuth         ##
                ##  Storage Configuration   ##
                ##                          ##
                
                Note: If your string contains special characters, you should enclose it in double quotes.""");
    }

    public static StorageConfigV1 create() {
        StorageConfigV1 config = loadConfig(StorageConfigV1.class, "storage.conf");
        if (config == null) {
            config = new StorageConfigV1();
            config.save();
        }
        return config;
    }

    public static StorageConfigV1 load() {
        StorageConfigV1 config = loadConfig(StorageConfigV1.class, "storage.conf");
        if (config == null) {
            throw new RuntimeException("storage.conf was not found. To regenerate the config files, delete the existing main.conf");
        }
        return config;
    }

    @Override
    public void save() {
        save(StorageConfigV1.class, this);
    }

    @ConfigSerializable
    public static class MySqlConfig {
        @Comment("""
                
                MySQL host.""")
        public String mysqlHost = "localhost";

        @Comment("""
                
                MySQL user.""")
        public String mysqlUser = "root";

        @Comment("""
                
                MySQL password.""")
        public String mysqlPassword = "password";

        @Comment("""
                
                MySQL database.""")
        public String mysqlDatabase = "easyauth";

        @Comment("""
                
                MySQL table name.""")
        public String mysqlTable = "easyauth";
    }

    @ConfigSerializable
    public static class MongoDBConfig {
        @Comment("""
                
                MongoDB connection string.""")
        public String mongodbConnectionString = "mongodb://username:password@host:port/?options";

        @Comment("""
                
                MongoDB database name.""")
        public String mongodbDatabase = "easyauth";
    }

    @ConfigSerializable
    public static class SQLiteConfig {
        @Comment("""
                
                SQLite database path.""")
        public String sqlitePath = "EasyAuth/easyauth.db";

        @Comment("""
                
                SQLite table name.""")
        public String sqliteTable = "easyauth";
    }

    @ConfigSerializable
    public static class PostgreSQLConfig {
        @Comment("""
                
                PostgreSQL host and port (e.g. localhost:5432).""")
        public String pgHost = "localhost:5432";

        @Comment("""
                
                PostgreSQL user.""")
        public String pgUser = "postgres";

        @Comment("""
                
                PostgreSQL password.""")
        public String pgPassword = "password";

        @Comment("""
                
                PostgreSQL database name.""")
        public String pgDatabase = "easyauth";

        @Comment("""
                
                PostgreSQL table name.""")
        public String pgTable = "easyauth";
    }

    public static @NotNull DbApi getDbApi() {
        DbApi db;
        if (EasyAuth.storageConfig.databaseType.equalsIgnoreCase("mysql")) {
            db = new MySQL(EasyAuth.storageConfig);
        } else if (EasyAuth.storageConfig.databaseType.equalsIgnoreCase("mongodb")) {
            db = new MongoDB(EasyAuth.storageConfig);
        } else if (EasyAuth.storageConfig.databaseType.equalsIgnoreCase("postgresql")) {
            db = new PostgreSQL(EasyAuth.storageConfig);
        } else {
            db = new SQLite(EasyAuth.storageConfig);
        }
        return db;
    }
}
