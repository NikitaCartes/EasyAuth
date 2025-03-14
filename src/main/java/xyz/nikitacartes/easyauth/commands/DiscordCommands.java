package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.discord.DiscordManager;

import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.langConfig;

/**
 * Commands for Discord integration
 */
public class DiscordCommands {

    /**
     * Register Discord commands
     *
     * @param dispatcher Command dispatcher
     */
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("discord")
                .requires(Permissions.require("easyauth.commands.discord.root", true))
                .then(literal("link")
                        .requires(Permissions.require("easyauth.commands.discord.link", true))
                        .executes(DiscordCommands::linkDiscord))
                .then(literal("unlink")
                        .requires(Permissions.require("easyauth.commands.discord.unlink", true))
                        .executes(DiscordCommands::unlinkDiscord))
                .then(literal("status")
                        .requires(Permissions.require("easyauth.commands.discord.status", true))
                        .executes(DiscordCommands::discordStatus))
        );
    }

    /**
     * Generate a link code for Discord
     *
     * @param context Command context
     * @return Command result
     */
    private static int linkDiscord(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            
            // Check if Discord integration is enabled
            if (!EasyAuth.discordConfig.enabled) {
                player.sendMessage(Text.literal("§c⚠ Discord integration is disabled."));
                return 0;
            }
            
            // Check if player is already linked
            if (EasyAuth.discordManager.isAccountLinked(player.getName().getString())) {
                player.sendMessage(Text.literal("§c⚠ Your account is already linked to Discord."));
                return 0;
            }
            
            // Generate link code
            String code = EasyAuth.discordManager.generateLinkCode(player);
            
            if (code == null) {
                player.sendMessage(Text.literal("§c⚠ Too many link attempts today. Please try again tomorrow."));
                return 0;
            }
            
            // Получаем имя бота
            String botUsername = EasyAuth.discordManager.getBotUsername();
            
            // Создаем кликабельное сообщение для кода
            String codeText = String.format("§a🔗 Your Discord link code: §e%s§a\n§7§n[Click to copy]§r §7Send this code to the Discord channel to link your account.", code);
            MutableText codeMessage = Text.literal(codeText);
            
            // Добавляем действие копирования при клике на [Click to copy]
            codeMessage = codeMessage.styled(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§aClick to copy code"))));
            
            // Отправляем сообщение игроку
            player.sendMessage(codeMessage);
            
            // Если есть ссылка на канал Discord
            if (EasyAuth.discordConfig.inviteLink != null && !EasyAuth.discordConfig.inviteLink.isEmpty()) {
                String botLinkText = String.format("§a🤖 Discord server: §e%s§a\n§7§n[Click to open]§r §7Join the Discord server.", EasyAuth.discordConfig.inviteLink);
                MutableText botLinkMessage = Text.literal(botLinkText);
                
                // Добавляем действие открытия ссылки при клике на [Click to open]
                botLinkMessage = botLinkMessage.styled(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, EasyAuth.discordConfig.inviteLink))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§aClick to open Discord server"))));
                
                player.sendMessage(botLinkMessage);
            }
            
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Unlink Discord from player's account
     *
     * @param context Command context
     * @return Command result
     */
    private static int unlinkDiscord(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            
            // Check if Discord integration is enabled
            if (!EasyAuth.discordConfig.enabled) {
                player.sendMessage(Text.literal("§c⚠ Discord integration is disabled."));
                return 0;
            }
            
            // Check if player is linked
            if (!EasyAuth.discordManager.isAccountLinked(player.getName().getString())) {
                player.sendMessage(Text.literal("§c⚠ Your account is not linked to Discord."));
                return 0;
            }
            
            // Unlink account
            EasyAuth.discordManager.unlinkAccount(player.getName().getString());
            player.sendMessage(Text.literal("§a✓ Your account has been unlinked from Discord."));
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Check Discord link status
     *
     * @param context Command context
     * @return Command result
     */
    private static int discordStatus(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            
            // Check if Discord integration is enabled
            if (!EasyAuth.discordConfig.enabled) {
                player.sendMessage(Text.literal("§c⚠ Discord integration is disabled."));
                return 0;
            }
            
            // Check if player is linked
            if (EasyAuth.discordManager.isAccountLinked(player.getName().getString())) {
                player.sendMessage(Text.literal("§a✓ Your account is linked to Discord."));
            } else {
                player.sendMessage(Text.literal("§c⚠ Your account is not linked to Discord."));
                player.sendMessage(Text.literal("§7Use /discord link to link your account."));
            }
            
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
} 