package xyz.nikitacartes.easyauth.utils;

import eu.pb4.placeholders.TextParser;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.playerCacheMap;

public class TranslationHelper {

    public static Text getEnterPassword() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.enterPassword") :
                TextParser.parse(config.lang.enterPassword);
    }

    public static Text getEnterNewPassword() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.enterNewPassword") :
                TextParser.parse(config.lang.enterNewPassword);
    }

    public static Text getWrongPassword() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.wrongPassword") :
                TextParser.parse(config.lang.wrongPassword);
    }

    public static Text getMatchPassword() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.matchPassword") :
                TextParser.parse(config.lang.matchPassword);
    }

    public static Text getPasswordUpdated() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.passwordUpdated") :
                TextParser.parse(config.lang.passwordUpdated);
    }

    public static Text getLoginRequired() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.loginRequired") :
                TextParser.parse(config.lang.loginRequired);
    }

    public static Text getLoginTriesExceeded() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.loginTriesExceeded") :
                TextParser.parse(config.lang.loginTriesExceeded);
    }

    public static Text getGlobalPasswordSet() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.globalPasswordSet") :
                TextParser.parse(config.lang.globalPasswordSet);
    }

    public static Text getCannotChangePassword() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.cannotChangePassword") :
                TextParser.parse(config.lang.cannotChangePassword);
    }

    public static Text getCannotUnregister() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.cannotUnregister") :
                TextParser.parse(config.lang.cannotUnregister);
    }

    public static Text getNotAuthenticated() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.notAuthenticated") :
                TextParser.parse(config.lang.notAuthenticated);
    }

    public static Text getAlreadyAuthenticated() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.alreadyAuthenticated") :
                TextParser.parse(config.lang.alreadyAuthenticated);
    }

    public static Text getSuccessfullyAuthenticated() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.successfullyAuthenticated") :
                TextParser.parse(config.lang.successfullyAuthenticated);
    }

    public static Text getSuccessfulLogout() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.successfulLogout") :
                TextParser.parse(config.lang.successfulLogout);
    }

    public static Text getTimeExpired() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.timeExpired") :
                TextParser.parse(config.lang.timeExpired);
    }

    public static Text getRegisterRequired() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.registerRequired") :
                TextParser.parse(config.lang.registerRequired);
    }

    public static Text getAlreadyRegistered() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.alreadyAuthenticated") :
                TextParser.parse(config.lang.alreadyAuthenticated);
    }

    public static Text getRegisterSuccess() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.registerSuccess") :
                TextParser.parse(config.lang.registerSuccess);
    }

    public static Text getUserdataDeleted() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.userdataDeleted") :
                TextParser.parse(config.lang.userdataDeleted);
    }

    public static Text getUserdataUpdated() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.userdataUpdated") :
                TextParser.parse(config.lang.userdataUpdated);
    }

    public static Text getAccountDeleted() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.accountDeleted") :
                TextParser.parse(config.lang.accountDeleted);
    }

    public static Text getConfigurationReloaded() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.configurationReloaded") :
                TextParser.parse(config.lang.configurationReloaded);
    }

    public static Text getMaxPasswordChars() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.maxPasswordChars", config.main.maxPasswordChars) :
                TextParser.parse(String.format(config.lang.maxPasswordChars, config.main.maxPasswordChars));
    }

    public static Text getMinPasswordChars() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.minPasswordChars", config.main.minPasswordChars) :
                TextParser.parse(String.format(config.lang.minPasswordChars, config.main.minPasswordChars));
    }

    public static Text getDisallowedUsername() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.disallowedUsername") :
                TextParser.parse(config.lang.disallowedUsername);
    }

    public static Text getPlayerAlreadyOnline() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.playerAlreadyOnline") :
                TextParser.parse(config.lang.playerAlreadyOnline);
    }

    public static Text getWorldSpawnSet() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.worldSpawnSet") :
                TextParser.parse(config.lang.worldSpawnSet);
    }

    public static Text getCorruptedPlayerData() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.corruptedPlayerData") :
                TextParser.parse(config.lang.corruptedPlayerData);
    }

    public static Text getUserNotRegistered() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.userNotRegistered") :
                TextParser.parse(config.lang.userNotRegistered);
    }

    public static Text getCannotLogout() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.cannotLogout") :
                TextParser.parse(config.lang.cannotLogout);
    }

    public static Text getOfflineUuid(String player, UUID uuid) {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.offlineUuid", player).
                        append(new TranslatableText(" [" + uuid + "]").
                                setStyle(Style.EMPTY.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))).
                                formatted(Formatting.YELLOW)) :
                TextParser.parse(
                        String.format(config.lang.offlineUuid, player) + " <yellow><copy:" + uuid.toString() + ">[" + uuid + "]"
                );
    }

    public static Text getRegisteredPlayers(boolean plainString) {
        int i = 0;
        if (config.experimental.enableServerSideTranslation && !plainString) {
            TranslatableText message = new TranslatableText("text.easyauth.registeredPlayers");
            for (var entry : playerCacheMap.entrySet()) {
                if (!entry.getValue().password.isEmpty()) {
                    i++;
                    message.append(new TranslatableText("\n" + i + ": [" + entry.getKey() + "]").
                            setStyle(Style.EMPTY.withClickEvent(
                                    new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey()))).
                            formatted(Formatting.YELLOW));
                }
            }
            return message;
        } else {
            StringBuilder message = new StringBuilder(config.lang.registeredPlayers);
            for (var entry : playerCacheMap.entrySet()) {
                if (!entry.getValue().password.isEmpty()) {
                    i++;
                    message.append("<yellow>\n").append(i).append(": <copy:").append(entry.getKey()).append(">[").append(entry.getKey()).append("]");
                }
            }
            return TextParser.parse(String.valueOf(message));
        }
    }

    public static Text getAddToForcedOffline() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.addToForcedOffline") :
                TextParser.parse(config.lang.addToForcedOffline);
    }


}
