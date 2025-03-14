package xyz.nikitacartes.easyauth.telegram;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.nikitacartes.easyauth.EasyAuth;

import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.langConfig;

/**
 * Commands for Telegram integration
 */
public class TelegramCommands {

    /**
     * Register Telegram commands
     *
     * @param dispatcher Command dispatcher
     */
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("telegram")
                .requires(Permissions.require("easyauth.commands.telegram.root", true))
                .then(literal("link")
                        .requires(Permissions.require("easyauth.commands.telegram.link", true))
                        .executes(TelegramCommands::linkTelegram))
                .then(literal("unlink")
                        .requires(Permissions.require("easyauth.commands.telegram.unlink", true))
                        .executes(TelegramCommands::unlinkTelegram))
                .then(literal("status")
                        .requires(Permissions.require("easyauth.commands.telegram.status", true))
                        .executes(TelegramCommands::telegramStatus))
        );
    }

    /**
     * Generate a link code for Telegram
     *
     * @param context Command context
     * @return Command result
     */
    private static int linkTelegram(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            
            // Check if Telegram integration is enabled
            if (!EasyAuth.telegramConfig.enabled) {
                langConfig.telegramDisabled.send(player);
                return 0;
            }
            
            // Check if player is already linked
            if (EasyAuth.telegramManager.isLinked(player.getName().getString())) {
                langConfig.telegramAlreadyLinked.send(player);
                return 0;
            }
            
            // Generate link code
            String code = EasyAuth.telegramManager.generateLinkCode(player.getName().getString());
            
            if (code == null) {
                langConfig.telegramTooManyAttempts.send(player);
                return 0;
            }
            
            // Send link code to player using sendMessage with get() method
            player.sendMessage(langConfig.telegramLinkCodeGenerated.get(code));
            
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Unlink Telegram from player's account
     *
     * @param context Command context
     * @return Command result
     */
    private static int unlinkTelegram(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            
            // Check if Telegram integration is enabled
            if (!EasyAuth.telegramConfig.enabled) {
                langConfig.telegramDisabled.send(player);
                return 0;
            }
            
            // Check if player is linked
            if (!EasyAuth.telegramManager.isLinked(player.getName().getString())) {
                langConfig.telegramNotLinked.send(player);
                return 0;
            }
            
            // Unlink account
            if (EasyAuth.telegramManager.unlinkAccount(player.getName().getString())) {
                langConfig.telegramUnlinkSuccess.send(player);
                return 1;
            }
            
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Check Telegram link status
     *
     * @param context Command context
     * @return Command result
     */
    private static int telegramStatus(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            
            // Check if Telegram integration is enabled
            if (!EasyAuth.telegramConfig.enabled) {
                langConfig.telegramDisabled.send(player);
                return 0;
            }
            
            // Check if player is linked
            if (EasyAuth.telegramManager.isLinked(player.getName().getString())) {
                langConfig.telegramStatusLinked.send(player);
            } else {
                langConfig.telegramStatusNotLinked.send(player);
            }
            
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
} 