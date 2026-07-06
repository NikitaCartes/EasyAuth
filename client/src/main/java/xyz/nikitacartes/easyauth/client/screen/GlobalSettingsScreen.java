package xyz.nikitacartes.easyauth.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import xyz.nikitacartes.easyauth.client.rules.Credentials;

/** Global auto-auth settings editor; commits into the shared {@link Credentials.Store} on close. */
public class GlobalSettingsScreen extends Screen {
    private static final int ROW_WIDTH = 310;

    private final ConfigScreen parent;
    private final Credentials.Store store;

    private Checkbox autoLoginBox;
    private Checkbox autoRegisterBox;
    private EditBox defaultPasswordBox;
    private EditBox loginCommandBox;
    private EditBox registerCommandBox;

    public GlobalSettingsScreen(ConfigScreen parent, Credentials.Store store) {
        super(Component.translatable("easyauthclient.config.globalSettings"));
        this.parent = parent;
        this.store = store;
    }

    @Override
    protected void init() {
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(title, font);
        LinearLayout column = layout.addToContents(LinearLayout.vertical().spacing(4));

        LinearLayout flagRow = column.addChild(LinearLayout.horizontal().spacing(8));
        autoLoginBox = flagRow.addChild(Checkbox.builder(
                        Component.translatable("easyauthclient.config.globalAutoLogin"), font)
                .selected(store.autoLogin)
                .build());
        autoLoginBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.globalAutoLogin.tooltip")));
        autoRegisterBox = flagRow.addChild(Checkbox.builder(
                        Component.translatable("easyauthclient.config.globalAutoRegister"), font)
                .selected(store.autoRegister)
                .build());
        autoRegisterBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.globalAutoRegister.tooltip")));

        defaultPasswordBox = column.addChild(new EditBox(font, ROW_WIDTH, 20,
                Component.translatable("easyauthclient.config.defaultPassword")));
        defaultPasswordBox.setMaxLength(512);
        defaultPasswordBox.setHint(Component.translatable("easyauthclient.config.defaultPassword"));
        defaultPasswordBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.defaultPassword.tooltip")));
        if (store.defaultPassword != null) {
            defaultPasswordBox.setValue(store.defaultPassword);
        }

        loginCommandBox = column.addChild(new EditBox(font, ROW_WIDTH, 20,
                Component.translatable("easyauthclient.config.loginCommand")));
        loginCommandBox.setMaxLength(256);
        loginCommandBox.setHint(Component.translatable("easyauthclient.config.loginCommand"));
        loginCommandBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.loginCommand.tooltip")));
        loginCommandBox.setValue(store.loginCommand == null ? "" : store.loginCommand);

        registerCommandBox = column.addChild(new EditBox(font, ROW_WIDTH, 20,
                Component.translatable("easyauthclient.config.registerCommand")));
        registerCommandBox.setMaxLength(256);
        registerCommandBox.setHint(Component.translatable("easyauthclient.config.registerCommand"));
        registerCommandBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.registerCommand.tooltip")));
        registerCommandBox.setValue(store.registerCommand == null ? "" : store.registerCommand);

        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        layout.arrangeElements();
    }

    /** Widget values → {@link #store}; keeps them across re-init (window resize). */
    private void commit() {
        store.autoLogin = autoLoginBox.selected();
        store.autoRegister = autoRegisterBox.selected();
        store.defaultPassword = defaultPasswordBox.getValue();
        store.loginCommand = loginCommandBox.getValue();
        store.registerCommand = registerCommandBox.getValue();
    }

    @Override
    public void resize(int width, int height) {
        commit();
        super.resize(width, height);
    }

    @Override
    public void onClose() {
        commit();
        ConfigScreen.open(parent);
    }
}
