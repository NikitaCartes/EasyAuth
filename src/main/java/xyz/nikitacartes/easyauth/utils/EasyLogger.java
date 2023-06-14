package xyz.nikitacartes.easyauth.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import static xyz.nikitacartes.easyauth.EasyAuth.config;

public class EasyLogger {
    private static final Logger logger = LogManager.getLogger("EasyAuth");

    static void log(Level level, String message) {
        logger.atLevel(level).log(Strings.concat("[EasyAuth]: ", message));
    }

    static void log(Level level, String message, Throwable e) {
        logger.atLevel(level).log("[EasyAuth]: " + message + "\n" + ExceptionUtils.getStackTrace(e));
    }

    public static void LogInfo(String message) {
        log(Level.INFO, message);
    }

    public static void LogInfo(String message, Throwable e) {
        log(Level.INFO, message, e);
    }

    public static void LogWarn(String message) {
        log(Level.WARN, message);
    }

    public static void LogWarn(String message, Throwable e) {
        log(Level.WARN, message, e);
    }

    public static void LogDebug(String message) {
        if (config != null && config.experimental.debugMode) {
            log(Level.INFO, "[DEBUG]: " + message);
        }
    }

    public static void LogDebug(String message, Throwable e) {
        if (config != null && config.experimental.debugMode) {
            log(Level.INFO, "[DEBUG]: " + message, e);
        }
    }

    public static void LogError(String message) {
        log(Level.ERROR, message);
    }

    public static void LogError(String message, Throwable e) {
        log(Level.ERROR, message, e);
    }
}