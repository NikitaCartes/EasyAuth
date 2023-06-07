package xyz.nikitacartes.easyauth.utils;

import eu.pb4.placeholders.api.TextParserUtils;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.playerCacheMap;

public class TranslationHelper {

    public static Text getEnterPassword() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.enterPassword") :
                Text.of(config.lang.enterPassword);
    }

    public static Text getEnterNewPassword() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.enterNewPassword") :
                Text.of(config.lang.enterNewPassword);
    }

    public static Text getWrongPassword() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.wrongPassword") :
                Text.of(config.lang.wrongPassword);
    }

    public static Text getMatchPassword() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.matchPassword") :
                Text.of(config.lang.matchPassword);
    }

    public static Text getPasswordUpdated() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.passwordUpdated") :
                Text.of(config.lang.passwordUpdated);
    }

    public static Text getLoginRequired() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.loginRequired") :
                Text.of(config.lang.loginRequired);
    }

    public static Text getLoginTriesExceeded() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.loginTriesExceeded") :
                Text.of(config.lang.loginTriesExceeded);
    }

    public static Text getGlobalPasswordSet() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.globalPasswordSet") :
                Text.of(config.lang.globalPasswordSet);
    }

    public static Text getCannotChangePassword() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.cannotChangePassword") :
                Text.of(config.lang.cannotChangePassword);
    }

    public static Text getCannotUnregister() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.cannotUnregister") :
                Text.of(config.lang.cannotUnregister);
    }

    public static Text getNotAuthenticated() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.notAuthenticated") :
                Text.of(config.lang.notAuthenticated);
    }

    public static Text getAlreadyAuthenticated() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.alreadyAuthenticated") :
                Text.of(config.lang.alreadyAuthenticated);
    }

    public static Text getSuccessfullyAuthenticated() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.successfullyAuthenticated") :
                Text.of(config.lang.successfullyAuthenticated);
    }

    public static Text getSuccessfulLogout() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.successfulLogout") :
                Text.of(config.lang.successfulLogout);
    }

    public static Text getTimeExpired() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.timeExpired") :
                Text.of(config.lang.timeExpired);
    }

    public static Text getRegisterRequired() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.registerRequired") :
                Text.of(config.lang.registerRequired);
    }

    public static Text getAlreadyRegistered() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.alreadyAuthenticated") :
                Text.of(config.lang.alreadyAuthenticated);
    }

    public static Text getRegisterSuccess() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.registerSuccess") :
                Text.of(config.lang.registerSuccess);
    }

    public static Text getUserdataDeleted() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.userdataDeleted") :
                Text.of(config.lang.userdataDeleted);
    }

    public static Text getUserdataUpdated() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.userdataUpdated") :
                Text.of(config.lang.userdataUpdated);
    }

    public static Text getAccountDeleted() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.accountDeleted") :
                Text.of(config.lang.accountDeleted);
    }

    public static Text getConfigurationReloaded() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.configurationReloaded") :
                Text.of(config.lang.configurationReloaded);
    }

    public static Text getMaxPasswordChars() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.maxPasswordChars", config.main.maxPasswordChars) :
                Text.of(String.format(config.lang.maxPasswordChars, config.main.maxPasswordChars));
    }

    public static Text getMinPasswordChars() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.minPasswordChars", config.main.minPasswordChars) :
                Text.of(String.format(config.lang.minPasswordChars, config.main.minPasswordChars));
    }

    public static Text getDisallowedUsername() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.disallowedUsername") :
                Text.of(config.lang.disallowedUsername);
    }

    public static Text getPlayerAlreadyOnline() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.playerAlreadyOnline") :
                Text.of(config.lang.playerAlreadyOnline);
    }

    public static Text getWorldSpawnSet() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.worldSpawnSet") :
                Text.of(config.lang.worldSpawnSet);
    }

    public static Text getCorruptedPlayerData() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.corruptedPlayerData") :
                Text.of(config.lang.corruptedPlayerData);
    }

    public static Text getUserNotRegistered() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.userNotRegistered") :
                Text.of(config.lang.userNotRegistered);
    }

    public static Text getCannotLogout() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.cannotLogout") :
                Text.of(config.lang.cannotLogout);
    }

    public static Text getOfflineUuid(String player, UUID uuid) {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.offlineUuid", player).
                        append(Text.translatable(" [" + uuid + "]").
                                setStyle(Style.EMPTY.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))).
                                formatted(Formatting.YELLOW)) :
                TextParserUtils.formatText(
                        String.format(config.lang.offlineUuid, player) + " <yellow><copy:" + uuid.toString() + ">[" + uuid + "]"
                );
    }

    public static Text getRegisteredPlayers(boolean plainString) {
        int i = 0;
        if (config.experimental.enableServerSideTranslation && !plainString) {
            MutableText message = Text.translatable("text.easyauth.registeredPlayers");
            for (var entry : playerCacheMap.entrySet()) {
                if (!entry.getValue().password.isEmpty()) {
                    i++;
                    message.append(Text.translatable("\n" + i + ": [" + entry.getKey() + "]").
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
            return TextParserUtils.formatText(String.valueOf(message));
        }
    }

    public static Text getAddToForcedOffline() {
        return config.experimental.enableServerSideTranslation ?
                Text.translatable("text.easyauth.addToForcedOffline") :
                Text.of(config.lang.addToForcedOffline);
    }


}
