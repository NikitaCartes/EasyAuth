package xyz.nikitacartes.easyauth.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xyz.nikitacartes.easyauth.client.rules.Credentials;
import xyz.nikitacartes.easyauth.client.rules.Vault;

/**
 * Master-password prompt. Shown once per launch when the multiplayer screen opens
 * with a password-protected vault, and on demand from the config screens. Skipping keeps the
 * session locked: stored secrets stay ciphertext and auto-login stays silent.
 */
public class UnlockScreen extends Screen {
    private static final int ROW_WIDTH = 310;

    private final Screen parent;
    private EditBox passwordBox;
    private StringWidget errorText;

    public UnlockScreen(Screen parent) {
        super(Component.translatable("easyauthclient.unlock.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        //? if >=1.20.5 {
        layout.addTitleHeader(title, font);
        //?} else {
        /*layout.addToHeader(new StringWidget(title, font));*/
        //?}
        GridLayout column = new GridLayout().spacing(4);
        //? if >=1.20.2 {
        passwordBox = new EditBox(font, ROW_WIDTH, 20, Component.translatable("easyauthclient.unlock.password"));
        //?} else {
        /*passwordBox = new EditBox(font, 0, 0, ROW_WIDTH, 20, Component.translatable("easyauthclient.unlock.password"));*/
        //?}
        passwordBox.setMaxLength(512);
        passwordBox.setHint(Component.translatable("easyauthclient.unlock.password"));
        column.addChild(passwordBox, 0, 0);
        errorText = new StringWidget(ROW_WIDTH, 9, Component.empty(), font);
        column.addChild(errorText, 1, 0);
        layout.addToContents(column);

        GridLayout footer = new GridLayout().spacing(8);
        footer.addChild(Button.builder(Component.translatable("easyauthclient.unlock.unlock"), button -> tryUnlock())
                .width(150).build(), 0, 0);
        footer.addChild(Button.builder(Component.translatable("easyauthclient.unlock.skip"), button -> onClose())
                .width(150).build(), 0, 1);
        layout.addToFooter(footer);
        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        layout.arrangeElements();
    }

    private void tryUnlock() {
        // ponytail: PBKDF2 (~0.3s) runs on the render thread — one hitch per launch, no worker.
        if (Vault.unlock(passwordBox.getValue())) {
            Credentials.onUnlocked();
            onClose();
        } else {
            errorText.setMessage(Component.translatable("easyauthclient.unlock.wrong").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public void onClose() {
        ConfigScreen.open(parent);
    }
}
