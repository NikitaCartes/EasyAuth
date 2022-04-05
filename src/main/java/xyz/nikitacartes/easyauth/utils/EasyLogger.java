package xyz.nikitacartes.easyauth.utils;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EasyLogger {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void logError(String error) {
        LOGGER.error("[EasyAuth] " + error);
    }

    public static void logInfo(String info) {
        LOGGER.info("[EasyAuth] " + info);
    }

    public static void logWarn(String info) {
        LOGGER.warn("[EasyAuth] " + info);
    }

}
