package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.utils.AuthHelper;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.simpleauth.utils.UuidConverter.convertUuid;

public class UnregisterCommand {
    private static Text enterPassword = new LiteralText(SimpleAuth.config.lang.enterPassword);
    private static Text wrongPassword = new LiteralText(SimpleAuth.config.lang.wrongPassword);
    private static Text accountDeleted = new LiteralText(SimpleAuth.config.lang.accountDeleted);
    private static Text cannotUnregister = new LiteralText(SimpleAuth.config.lang.cannotUnregister);

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/unregister" command
        dispatcher.register(literal("unregister")
                .executes(ctx -> {
                    ctx.getSource().getPlayer().sendMessage(enterPassword, false);
                    return 1;
                })
                .then(argument("password", word())
                        .executes( ctx -> unregister(
                                ctx.getSource(),
                                getString(ctx, "password")
                                )
                        )
                )
        );
    }

    // Method called for checking the password and then removing user's account from db
    private static int unregister(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayer();
        if (SimpleAuth.config.main.enableGlobalPassword) {
            player.sendMessage(cannotUnregister, false);
            return 0;
        }
        else if (AuthHelper.checkPass(convertUuid(player), pass.toCharArray()) == 1) {
            SimpleAuth.deauthenticatePlayer(player);
            SimpleAuth.db.deleteUserData(convertUuid(player));
            player.sendMessage(accountDeleted, false);
            return 1;
        }
        player.sendMessage(wrongPassword, false);
        return 0;
    }
}
