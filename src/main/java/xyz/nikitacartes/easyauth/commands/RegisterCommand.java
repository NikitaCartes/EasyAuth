package xyz.nikitacartes.easyauth.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.storage.database.*;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.HashMap;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.AuthHelper.checkGlobalPassword;
import static xyz.nikitacartes.easyauth.utils.AuthHelper.hashPassword;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;


public class RegisterCommand {

    // Registering the "/reg" alias
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> node = registerRegister(dispatcher);
        if (extendedConfig.aliases.register) {
            dispatcher.register(literal("reg")
                    .requires(Permissions.require("easyauth.commands.register", true))
                    .redirect(node));
        }
        
        // Регистрация команды для миграции данных
        dispatcher.register(literal("easyauth-migrate")
                .requires(Permissions.require("easyauth.commands.migrate", 4))
                .executes(ctx -> migrateData(ctx.getSource())));
    }

    // Метод для миграции данных
    private static int migrateData(ServerCommandSource source) {
        LogInfo("Принудительная миграция данных...");
        long now = System.currentTimeMillis();

        DbApi db;
        if (storageConfig.databaseType.equalsIgnoreCase("mysql")) {
            db = new MySQL(storageConfig);
        } else if (storageConfig.databaseType.equalsIgnoreCase("mongodb")) {
            db = new MongoDB(storageConfig);
        } else {
            storageConfig.databaseType = "sqlite";
            db = new SQLite(storageConfig);
        }
        
        try {
            db.connect();
        } catch (DBApiException e) {
            LogDebug("Ошибка подключения к базе данных: " + e.getMessage());
            source.sendMessage(net.minecraft.text.Text.literal("Ошибка подключения к базе данных"));
            return 0;
        }

        UserCache userCache = new UserCache(null, new File(gameDirectory + "/usercache.json"));
        HashMap<String, String> uuids = new HashMap<>();
        for (UserCache.Entry entry : userCache.load()) {
            GameProfile profile = entry.getProfile();
            uuids.put(profile.getName(), profile.getId().toString());
        }
        
        db.migrateFromV1(uuids);
        db.close();
        
        LogInfo("Миграция завершена за " + (System.currentTimeMillis() - now) + "мс");
        source.sendMessage(net.minecraft.text.Text.literal("Миграция данных завершена"));
        return 1;
    }

    // Registering the "/register" command
    public static LiteralCommandNode<ServerCommandSource> registerRegister(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (config.enableGlobalPassword && config.singleUseGlobalPassword) {
            return dispatcher.register(literal("register")
                    .requires(Permissions.require("easyauth.commands.register", true))
                    .then(argument("globalPassword", string())
                            .then(argument("password", string())
                                    .then(argument("passwordAgain", string())
                                            .executes(ctx -> register(ctx.getSource(),
                                                    getString(ctx, "globalPassword"),
                                                    getString(ctx, "password"),
                                                    getString(ctx, "passwordAgain")))
                                    )
                            )
                    )
                    .executes(ctx -> {
                        langConfig.enterPassword.send(ctx.getSource());
                        return 0;
                    }));
        } else {
            return dispatcher.register(literal("register")
                    .requires(Permissions.require("easyauth.commands.register", true))
                    .then(argument("password", string())
                            .then(argument("passwordAgain", string())
                                    .executes(ctx -> register(ctx.getSource(),
                                            getString(ctx, "password"),
                                            getString(ctx, "passwordAgain")))
                            )
                    )
                    .executes(ctx -> {
                        langConfig.enterPassword.send(ctx.getSource());
                        return 0;
                    }));
        }
    }

    private static int register(ServerCommandSource source, String globalPassword, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (config.enableGlobalPassword && config.singleUseGlobalPassword) {
            if (checkGlobalPassword(globalPassword.toCharArray())) {
                return register(source, pass1, pass2);
            } else {
                PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();

                playerData.loginTries++;
                if (playerData.loginTries >= config.maxLoginTries && config.maxLoginTries != -1) { // Player exceeded maxLoginTries
                    LogDebug("Player " + player.getNameForScoreboard() + " exceeded global password tries limit.");
                    playerData.lastKickedDate = ZonedDateTime.now();
                    playerData.loginTries = 0;
                    playerData.update();
                    player.networkHandler.disconnect(langConfig.wrongGlobalPassword.get());
                    return 0;
                }
                langConfig.wrongGlobalPassword.send(source);
                return 0;
            }
        }
        return 0;
    }

    // Method called for hashing the password & writing to DB
    private static int register(ServerCommandSource source, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.loginRequired.send(source);
            return 0;
        } else if (playerAuth.easyAuth$isAuthenticated()) {
            langConfig.alreadyAuthenticated.send(source);
            return 0;
        } else if (!pass1.equals(pass2)) {
            langConfig.matchPassword.send(source);
            return 0;
        }

        if (pass1.length() < extendedConfig.minPasswordLength) {
            langConfig.minPasswordChars.send(source);
            return 0;
        } else if (pass1.length() > extendedConfig.maxPasswordLength && extendedConfig.maxPasswordLength != -1) {
            langConfig.maxPasswordChars.send(source);
            return 0;
        }

        PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();
        if (!playerData.password.isEmpty()) {
            langConfig.alreadyRegistered.send(source);
            return 0;
        }
        playerAuth.easyAuth$setAuthenticated(true);
        playerAuth.easyAuth$restoreTrueLocation();
        langConfig.registerSuccess.send(source);
        // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));

        THREADPOOL.submit(() -> {
            playerData.password = hashPassword(pass1.toCharArray());
            playerData.registrationDate = ZonedDateTime.now();
            playerData.lastIp = playerAuth.easyAuth$getIpAddress();
            playerData.lastAuthenticatedDate = ZonedDateTime.now();
            playerData.uuid = player.getUuid();
            playerAuth.easyAuth$setPlayerEntryV1(playerData);
            playerData.update();

            LogDebug("Player " + player.getNameForScoreboard() + "{" + player.getUuidAsString() + "} successfully registered with password: " + playerData.password);
        });
        return 0;
    }
}
