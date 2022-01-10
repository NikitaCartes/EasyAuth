package xyz.nikitacartes.easyauth.utils;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.playerCacheMap;

public class TranslationHelper {

    public static Text getEnterPassword() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.enterPassword") :
                new LiteralText(config.lang.enterPassword);
    }

    public static Text getEnterNewPassword() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.enterNewPassword") :
                new LiteralText(config.lang.enterNewPassword);
    }

    public static Text getWrongPassword() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.wrongPassword") :
                new LiteralText(config.lang.wrongPassword);
    }

    public static Text getMatchPassword() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.matchPassword") :
                new LiteralText(config.lang.matchPassword);
    }

    public static Text getPasswordUpdated() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.passwordUpdated") :
                new LiteralText(config.lang.passwordUpdated);
    }

    public static Text getLoginRequired() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.loginRequired") :
                new LiteralText(config.lang.loginRequired);
    }

    public static Text getLoginTriesExceeded() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.loginTriesExceeded") :
                new LiteralText(config.lang.loginTriesExceeded);
    }

    public static Text getGlobalPasswordSet() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.globalPasswordSet") :
                new LiteralText(config.lang.globalPasswordSet);
    }

    public static Text getCannotChangePassword() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.cannotChangePassword") :
                new LiteralText(config.lang.cannotChangePassword);
    }

    public static Text getCannotUnregister() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.cannotUnregister") :
                new LiteralText(config.lang.cannotUnregister);
    }

    public static Text getNotAuthenticated() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.notAuthenticated") :
                new LiteralText(config.lang.notAuthenticated);
    }

    public static Text getAlreadyAuthenticated() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.alreadyAuthenticated") :
                new LiteralText(config.lang.alreadyAuthenticated);
    }

    public static Text getSuccessfullyAuthenticated() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.successfullyAuthenticated") :
                new LiteralText(config.lang.successfullyAuthenticated);
    }

    public static Text getSuccessfulLogout() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.successfulLogout") :
                new LiteralText(config.lang.successfulLogout);
    }

    public static Text getTimeExpired() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.timeExpired") :
                new LiteralText(config.lang.timeExpired);
    }

    public static Text getRegisterRequired() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.registerRequired") :
                new LiteralText(config.lang.registerRequired);
    }

    public static Text getAlreadyRegistered() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.alreadyAuthenticated") :
                new LiteralText(config.lang.alreadyAuthenticated);
    }

    public static Text getRegisterSuccess() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.registerSuccess") :
                new LiteralText(config.lang.registerSuccess);
    }

    public static Text getUserdataDeleted() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.userdataDeleted") :
                new LiteralText(config.lang.userdataDeleted);
    }

    public static Text getUserdataUpdated() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.userdataUpdated") :
                new LiteralText(config.lang.userdataUpdated);
    }

    public static Text getAccountDeleted() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.accountDeleted") :
                new LiteralText(config.lang.accountDeleted);
    }

    public static Text getConfigurationReloaded() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.configurationReloaded") :
                new LiteralText(config.lang.configurationReloaded);
    }

    public static Text getMaxPasswordChars() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.maxPasswordChars", config.main.maxPasswordChars) :
                new LiteralText(String.format(config.lang.maxPasswordChars, config.main.maxPasswordChars));
    }

    public static Text getMinPasswordChars() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.minPasswordChars", config.main.minPasswordChars) :
                new LiteralText(String.format(config.lang.minPasswordChars, config.main.minPasswordChars));
    }

    public static Text getDisallowedUsername() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.disallowedUsername") :
                new LiteralText(config.lang.disallowedUsername);
    }

    public static Text getPlayerAlreadyOnline() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.playerAlreadyOnline") :
                new LiteralText(config.lang.playerAlreadyOnline);
    }

    public static Text getWorldSpawnSet() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.worldSpawnSet") :
                new LiteralText(config.lang.worldSpawnSet);
    }

    public static Text getCorruptedPlayerData() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.corruptedPlayerData") :
                new LiteralText(config.lang.corruptedPlayerData);
    }

    public static Text getUserNotRegistered() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.userNotRegistered") :
                new LiteralText(config.lang.userNotRegistered);
    }

    public static Text getCannotLogout() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.cannotLogout") :
                new LiteralText(config.lang.cannotLogout);
    }

    public static Text getOfflineUuid(String player, UUID uuid) {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.offlineUuid", player).
                        append(new TranslatableText(" [" + uuid + "]").
                                setStyle(Style.EMPTY.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))).
                                formatted(Formatting.YELLOW)) :
                new LiteralText(String.format(config.lang.offlineUuid, player)).
                        append(new LiteralText(" [" + uuid + "]").
                                setStyle(Style.EMPTY.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))).
                                formatted(Formatting.YELLOW));
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
            LiteralText message = new LiteralText(config.lang.registeredPlayers);
            for (var entry : playerCacheMap.entrySet()) {
                if (!entry.getValue().password.isEmpty()) {
                    i++;
                    message.append(new LiteralText("\n" + i + ": [" + entry.getKey() + "]").
                            setStyle(Style.EMPTY.withClickEvent(
                                    new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey()))).
                            formatted(Formatting.YELLOW));
                }
            }
            return message;
        }
    }

    public static Text getAddToForcedOffline() {
        return config.experimental.enableServerSideTranslation ?
                new TranslatableText("text.easyauth.addToForcedOffline") :
                new LiteralText(config.lang.addToForcedOffline);
    }


}
