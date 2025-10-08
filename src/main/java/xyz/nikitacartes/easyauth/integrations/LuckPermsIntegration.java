package xyz.nikitacartes.easyauth.integrations;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

public class LuckPermsIntegration implements ContextCalculator<ServerPlayerEntity> {

    @Override
    public void calculate(@NonNull ServerPlayerEntity playerEntity, @NonNull ContextConsumer contextConsumer) {
        contextConsumer.accept("easyauth:authenticated", Boolean.toString(((PlayerAuth)playerEntity).easyAuth$isAuthenticated()));
        contextConsumer.accept("easyauth:online_account", Boolean.toString(((PlayerAuth)playerEntity).easyAuth$isUsingMojangAccount()));
    }

    @Override
    public @NotNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        builder.add("easyauth:authenticated", Boolean.toString(true));
        builder.add("easyauth:online_account", Boolean.toString(true));

        builder.add("easyauth:authenticated", Boolean.toString(false));
        builder.add("easyauth:online_account", Boolean.toString(false));

        return builder.build();
    }

    public static void register() {
        LuckPerms luckPerms = LuckPermsProvider.get();
        luckPerms.getContextManager().registerCalculator(new LuckPermsIntegration());
    }

}
