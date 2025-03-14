package xyz.nikitacartes.easyauth.telegram;

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
            
            // Получаем имя бота
            String botUsername = EasyAuth.telegramManager.getBotUsername();
            
            // Создаем кликабельное сообщение для кода
            String codeText = String.format("§a🔗 Your link code: §e%s§a\n§7§n[Click to copy]§r §7Send this code to the Telegram bot to link your account.", code);
            MutableText codeMessage = Text.literal(codeText);
            
            // Находим позицию [Click to copy] в тексте
            int startIndex = codeText.indexOf("[Click to copy]");
            int endIndex = startIndex + "[Click to copy]".length();
            
            // Добавляем действие копирования при клике на [Click to copy]
            codeMessage = codeMessage.styled(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§aНажмите, чтобы скопировать код"))));
            
            // Создаем кликабельную ссылку на бота
            String botLink = "https://t.me/" + botUsername;
            String botLinkText = String.format("§a🤖 Telegram bot: §e%s§a\n§7§n[Click to open]§r §7Open the link to connect with the bot.", botUsername);
            MutableText botLinkMessage = Text.literal(botLinkText);
            
            // Находим позицию [Click to open] в тексте
            startIndex = botLinkText.indexOf("[Click to open]");
            endIndex = startIndex + "[Click to open]".length();
            
            // Добавляем действие открытия ссылки при клике на [Click to open]
            botLinkMessage = botLinkMessage.styled(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, botLink))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§aНажмите, чтобы открыть чат с ботом"))));
            
            // Отправляем сообщения игроку
            player.sendMessage(codeMessage);
            player.sendMessage(botLinkMessage);
            
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