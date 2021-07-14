package org.samo_lego.simpleauth.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleLogger {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void logError(String error) {
        LOGGER.error("[EasyAuth] " + error);
    }

    public static void logInfo(String info) {
        LOGGER.info("[EasyAuth] " + info);
    }

}
