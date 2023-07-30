package xyz.nikitacartes.easyauth.utils;

import eu.pb4.placeholders.api.TextParserUtils;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.*;

public class TranslationHelper {

    public static Text getEnterPassword() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.enterPassword") :
                Text.of(langConfig.enterPassword);
    }

    public static Text getEnterNewPassword() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.enterNewPassword") :
                Text.of(langConfig.enterNewPassword);
    }

    public static Text getWrongPassword() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.wrongPassword") :
                Text.of(langConfig.wrongPassword);
    }

    public static Text getMatchPassword() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.matchPassword") :
                Text.of(langConfig.matchPassword);
    }

    public static Text getPasswordUpdated() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.passwordUpdated") :
                Text.of(langConfig.passwordUpdated);
    }

    public static Text getLoginRequired() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.loginRequired") :
                Text.of(langConfig.loginRequired);
    }

    public static Text getLoginTriesExceeded() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.loginTriesExceeded") :
                Text.of(langConfig.loginTriesExceeded);
    }

    public static Text getGlobalPasswordSet() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.globalPasswordSet") :
                Text.of(langConfig.globalPasswordSet);
    }

    public static Text getCannotChangePassword() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.cannotChangePassword") :
                Text.of(langConfig.cannotChangePassword);
    }

    public static Text getCannotUnregister() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.cannotUnregister") :
                Text.of(langConfig.cannotUnregister);
    }

    public static Text getNotAuthenticated() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.notAuthenticated") :
                Text.of(langConfig.notAuthenticated);
    }

    public static Text getAlreadyAuthenticated() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.alreadyAuthenticated") :
                Text.of(langConfig.alreadyAuthenticated);
    }

    public static Text getSuccessfullyAuthenticated() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.successfullyAuthenticated") :
                Text.of(langConfig.successfullyAuthenticated);
    }

    public static Text getSuccessfulLogout() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.successfulLogout") :
                Text.of(langConfig.successfulLogout);
    }

    public static Text getTimeExpired() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.timeExpired") :
                Text.of(langConfig.timeExpired);
    }

    public static Text getRegisterRequired() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.registerRequired") :
                Text.of(langConfig.registerRequired);
    }

    public static Text getAlreadyRegistered() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.alreadyAuthenticated") :
                Text.of(langConfig.alreadyAuthenticated);
    }

    public static Text getRegisterSuccess() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.registerSuccess") :
                Text.of(langConfig.registerSuccess);
    }

    public static Text getUserdataDeleted() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.userdataDeleted") :
                Text.of(langConfig.userdataDeleted);
    }

    public static Text getUserdataUpdated() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.userdataUpdated") :
                Text.of(langConfig.userdataUpdated);
    }

    public static Text getAccountDeleted() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.accountDeleted") :
                Text.of(langConfig.accountDeleted);
    }

    public static Text getConfigurationReloaded() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.configurationReloaded") :
                Text.of(langConfig.configurationReloaded);
    }

    public static Text getMaxPasswordChars() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.maxPasswordChars", extendedConfig.maxPasswordLength) :
                Text.of(String.format(langConfig.maxPasswordChars, extendedConfig.maxPasswordLength));
    }

    public static Text getMinPasswordChars() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.minPasswordChars", extendedConfig.minPasswordLength) :
                Text.of(String.format(langConfig.minPasswordChars, extendedConfig.minPasswordLength));
    }

    public static Text getDisallowedUsername() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.disallowedUsername") :
                Text.of(langConfig.disallowedUsername);
    }

    public static Text getPlayerAlreadyOnline() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.playerAlreadyOnline") :
                Text.of(langConfig.playerAlreadyOnline);
    }

    public static Text getWorldSpawnSet() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.worldSpawnSet") :
                Text.of(langConfig.worldSpawnSet);
    }

    public static Text getCorruptedPlayerData() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.corruptedPlayerData") :
                Text.of(langConfig.corruptedPlayerData);
    }

    public static Text getUserNotRegistered() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.userNotRegistered") :
                Text.of(langConfig.userNotRegistered);
    }

    public static Text getCannotLogout() {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.cannotLogout") :
                Text.of(langConfig.cannotLogout);
    }

    public static Text getOfflineUuid(String player, UUID uuid) {
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.offlineUuid", player).
                        append(Text.translatable(" [" + uuid + "]").
                                setStyle(Style.EMPTY.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))).
                                formatted(Formatting.YELLOW)) :
                TextParserUtils.formatText(
                        String.format(langConfig.offlineUuid, player) + " <yellow><copy:" + uuid.toString() + ">[" + uuid + "]"
                );
    }

    public static Text getRegisteredPlayers(boolean plainString) {
        int i = 0;
        if (langConfig.enableServerSideTranslation && !plainString) {
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
            StringBuilder message = new StringBuilder(langConfig.registeredPlayers);
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
        return langConfig.enableServerSideTranslation ?
                Text.translatable("text.easyauth.addToForcedOffline") :
                Text.of(langConfig.addToForcedOffline);
    }


}
