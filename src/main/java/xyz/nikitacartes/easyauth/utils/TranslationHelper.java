package xyz.nikitacartes.easyauth.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.*;

public class TranslationHelper {

    public static void sendEnterPassword(ServerCommandSource commandOutput) {
        if (langConfig.enterPassword != null) {
            commandOutput.sendMessage(langConfig.enterPassword);
        }
    }

    public static void sendEnterNewPassword(ServerCommandSource commandOutput) {
        if (langConfig.enterNewPassword != null) {
            commandOutput.sendMessage(langConfig.enterNewPassword);
        }
    }

    public static void sendWrongPassword(ServerCommandSource commandOutput) {
        if (langConfig.wrongPassword != null) {
            commandOutput.sendMessage(langConfig.wrongPassword);
        }
    }

    public static Text getWrongPassword() {
        return langConfig.wrongPassword != null ? langConfig.wrongPassword : Text.of("");
    }

    public static void sendMatchPassword(ServerCommandSource commandOutput) {
        if (langConfig.matchPassword != null) {
            commandOutput.sendMessage(langConfig.matchPassword);
        }
    }

    public static void sendPasswordUpdated(ServerCommandSource commandOutput) {
        if (langConfig.passwordUpdated != null) {
            commandOutput.sendMessage(langConfig.passwordUpdated);
        }
    }

    public static void sendLoginRequired(ServerCommandSource commandOutput) {
        if (langConfig.loginRequired != null) {
            commandOutput.sendMessage(langConfig.loginRequired);
        }
    }

    public static <T extends CommandOutput> void sendLoginRequired(T commandOutput) {
        if (langConfig.loginRequired != null) {
            commandOutput.sendMessage(langConfig.loginRequired);
        }
    }

    public static Text getLoginTriesExceeded() {
        return langConfig.loginTriesExceeded != null ? langConfig.loginTriesExceeded : Text.of("");
    }

    public static void sendGlobalPasswordSet(ServerCommandSource commandOutput) {
        if (langConfig.globalPasswordSet != null) {
            commandOutput.sendMessage(langConfig.globalPasswordSet);
        }
    }

    // Not used
    public static void sendCannotChangePassword(ServerCommandSource commandOutput) {
        if (langConfig.cannotChangePassword != null) {
            commandOutput.sendMessage(langConfig.cannotChangePassword);
        }
    }

    public static void sendCannotUnregister(ServerCommandSource commandOutput) {
        if (langConfig.cannotUnregister != null) {
            commandOutput.sendMessage(langConfig.cannotUnregister);
        }
    }

    // Not used
    public static void sendNotAuthenticated(ServerCommandSource commandOutput) {
        if (langConfig.notAuthenticated != null) {
            commandOutput.sendMessage(langConfig.notAuthenticated);
        }
    }

    public static void sendAlreadyAuthenticated(ServerCommandSource commandOutput) {
        if (langConfig.alreadyAuthenticated != null) {
            commandOutput.sendMessage(langConfig.alreadyAuthenticated);
        }
    }

    public static void sendSuccessfullyAuthenticated(ServerCommandSource commandOutput) {
        if (langConfig.successfullyAuthenticated != null) {
            commandOutput.sendMessage(langConfig.successfullyAuthenticated);
        }
    }

    public static void sendSuccessfulLogout(ServerCommandSource commandOutput) {
        if (langConfig.successfulLogout != null) {
            commandOutput.sendMessage(langConfig.successfulLogout);
        }
    }

    public static Text getTimeExpired() {
        return langConfig.timeExpired != null ? langConfig.timeExpired : Text.of("");
    }


    public static void sendRegisterRequired(ServerCommandSource commandOutput) {
        if (langConfig.registerRequired != null) {
            commandOutput.sendMessage(langConfig.registerRequired);
        }
    }

    public static <T extends CommandOutput> void sendRegisterRequired(T commandOutput) {
        if (langConfig.registerRequired != null) {
            commandOutput.sendMessage(langConfig.registerRequired);
        }
    }

    public static void sendAlreadyRegistered(ServerCommandSource commandOutput) {
        if (langConfig.alreadyRegistered != null) {
            commandOutput.sendMessage(langConfig.alreadyRegistered);
        }
    }

    public static void sendRegisterSuccess(ServerCommandSource commandOutput) {
        if (langConfig.registerSuccess != null) {
            commandOutput.sendMessage(langConfig.registerSuccess);
        }
    }

    public static void sendUserdataDeleted(ServerCommandSource commandOutput) {
        if (langConfig.userdataDeleted != null) {
            commandOutput.sendMessage(langConfig.userdataDeleted);
        }
    }

    public static void sendUserdataUpdated(ServerCommandSource commandOutput) {
        if (langConfig.userdataUpdated != null) {
            commandOutput.sendMessage(langConfig.userdataUpdated);
        }
    }

    public static void sendAccountDeleted(ServerCommandSource commandOutput) {
        if (langConfig.accountDeleted != null) {
            commandOutput.sendMessage(langConfig.accountDeleted);
        }
    }

    public static Text getAccountDeleted() {
        return langConfig.accountDeleted != null ? langConfig.accountDeleted : Text.of("");
    }

    public static void sendConfigurationReloaded(ServerCommandSource commandOutput) {
        if (langConfig.configurationReloaded != null) {
            commandOutput.sendMessage(langConfig.configurationReloaded);
        }
    }

    public static <T extends CommandOutput> void sendConfigurationReloaded(T commandOutput) {
        if (langConfig.configurationReloaded != null) {
            commandOutput.sendMessage(langConfig.configurationReloaded);
        }
    }

    public static void sendMaxPasswordChars(ServerCommandSource commandOutput) {
        if (langConfig.maxPasswordChars != null) {
            commandOutput.sendMessage(langConfig.maxPasswordChars);
        }
    }

    public static void sendMinPasswordChars(ServerCommandSource commandOutput) {
        if (langConfig.minPasswordChars != null) {
            commandOutput.sendMessage(langConfig.minPasswordChars);
        }
    }

    public static Text getDisallowedUsername() {
        if (langConfig.disallowedUsername != null) {
            if (langConfig.enableServerSideTranslation) {
                TranslatableTextContent content = (TranslatableTextContent) langConfig.disallowedUsername.getContent();
                return Text.translatableWithFallback(content.getKey(), content.getFallback(), extendedConfig.usernameRegexp);
            } else {
                return Text.translatable(langConfig.disallowedUsername.getString(), extendedConfig.usernameRegexp);
            }
        }
        return Text.of("");
    }

    public static Text getPlayerAlreadyOnline(PlayerEntity player) {
        if (langConfig.playerAlreadyOnline != null) {
            if (langConfig.enableServerSideTranslation) {
                TranslatableTextContent content = (TranslatableTextContent) langConfig.playerAlreadyOnline.getContent();
                return Text.translatableWithFallback(content.getKey(), content.getFallback(), player.getName());
            } else {
                return Text.translatable(langConfig.playerAlreadyOnline.getString(), player.getName());
            }
        }
        return Text.of("");
    }

    public static void sendWorldSpawnSet(ServerCommandSource commandOutput) {
        if (langConfig.worldSpawnSet != null) {
            commandOutput.sendMessage(langConfig.worldSpawnSet);
        }
    }

    // Not used
    public static Text getCorruptedPlayerData() {
        return langConfig.corruptedPlayerData != null ? langConfig.corruptedPlayerData : Text.of("");
    }


    public static void sendUserNotRegistered(ServerCommandSource commandOutput) {
        if (langConfig.userNotRegistered != null) {
            commandOutput.sendMessage(langConfig.userNotRegistered);
        }
    }

    public static void sendCannotLogout(ServerCommandSource commandOutput) {
        if (langConfig.cannotLogout != null) {
            commandOutput.sendMessage(langConfig.cannotLogout);
        }
    }

    public static void sendOfflineUuid(ServerCommandSource commandOutput, String player, UUID uuid) {
        if (langConfig.offlineUuid != null) {
            if (langConfig.enableServerSideTranslation) {
                TranslatableTextContent content = (TranslatableTextContent) langConfig.offlineUuid.getContent();
                commandOutput.sendMessage(Text.translatableWithFallback(content.getKey(),
                        content.getFallback(), player, Text.literal("[" + uuid + "]").setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))
                                .withColor(Formatting.YELLOW))));
            } else {
                commandOutput.sendMessage(Text.translatable(langConfig.offlineUuid.getString(), player, Text.literal(" [" + uuid + "]").setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))
                        .withColor(Formatting.YELLOW))));
            }
        }
    }

    public static void sendRegisteredPlayers(ServerCommandSource commandOutput) {
        if (langConfig.registeredPlayers != null) {
            int i = 0;
            MutableText message = langConfig.registeredPlayers;
            for (var entry : playerCacheMap.entrySet()) {
                if (!entry.getValue().password.isEmpty()) {
                    i++;
                    message.append(Text.translatable("\n" + i + ": [" + entry.getKey() + "]")
                            .setStyle(Style.EMPTY.withClickEvent(
                                    new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey())))
                            .formatted(Formatting.YELLOW));
                }
            }
            commandOutput.sendMessage(message);
        }
    }

    public static void sendAddToForcedOffline(ServerCommandSource commandOutput) {
        if (langConfig.addToForcedOffline != null) {
            commandOutput.sendMessage(langConfig.addToForcedOffline);
        }
    }


}
