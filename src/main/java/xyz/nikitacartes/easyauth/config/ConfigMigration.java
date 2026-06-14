package xyz.nikitacartes.easyauth.config;

import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.storage.database.*;

import static xyz.nikitacartes.easyauth.config.MainConfigV1.CURRENT_CONFIG_VERSION;
import static xyz.nikitacartes.easyauth.config.StorageConfigV1.getDbApi;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

public class ConfigMigration {

    public static void migrateFromV4() {
        LogInfo("Migrating DB from v4 to v5");
        long now = System.currentTimeMillis();

        DbApi db = getDbApi();
        try {
            db.connect();
        } catch (DBApiException e) {
            LogError("Migration connection error: ", e);
            return;
        }

        db.migrateFromV4();
        db.close();

        EasyAuth.langConfig.save();
        EasyAuth.extendedConfig.save();

        EasyAuth.config.configVersion = 5;
        EasyAuth.config.save();

        LogInfo("Migration completed in " + (System.currentTimeMillis() - now) + "ms");
    }

    public static void saveAndMigrateTo(int targetVersion) {
        LogInfo("Backing up config and migrating to v" + targetVersion);

        EasyAuth.storageConfig.save();
        EasyAuth.extendedConfig.save();
        EasyAuth.langConfig.save();

        EasyAuth.config.configVersion = targetVersion;
        EasyAuth.config.save();
    }

    public static void configMigration(int configVersion) {
        // Apply migrations sequentially
        if (configVersion < 2) {
            throw new RuntimeException("The config and database are too old to be migrated directly. The latest EasyAuth version that still supports migration is 3.4.3. Start your server with that version once to perform the migration, then update EasyAuth to the latest release.");
        }
        if (configVersion < 4) {
            saveAndMigrateTo(4);
        }
        if (configVersion < 5) {
            migrateFromV4();
        }
        if (configVersion < CURRENT_CONFIG_VERSION) {
            saveAndMigrateTo(CURRENT_CONFIG_VERSION);
        }
    }

}
