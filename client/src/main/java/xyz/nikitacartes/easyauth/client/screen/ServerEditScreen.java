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
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;

/** Per-server credentials editor; commits into the shared {@link Credentials.Store} on close. */
public class ServerEditScreen extends Screen {
    private static final int ROW_WIDTH = 310;

    private final ConfigScreen parent;
    private final Credentials.Store store;
    private String editAddress; // null = adding a new server; set on first commit

    private EditBox addressBox;
    private EditBox passwordBox;
    private EditBox totpBox;
    private Checkbox autoLoginBox;
    private Checkbox autoRegisterBox;

    public ServerEditScreen(ConfigScreen parent, Credentials.Store store, String editAddress) {
        super(editAddress != null
                ? Component.literal(editAddress)
                : Component.translatable("easyauthclient.config.newServer"));
        this.parent = parent;
        this.store = store;
        this.editAddress = editAddress;
    }

    @Override
    protected void init() {
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(title, font);
        LinearLayout column = layout.addToContents(LinearLayout.vertical().spacing(4));
        Credentials entry = editAddress != null ? store.servers.get(editAddress) : null;

        addressBox = column.addChild(new EditBox(font, ROW_WIDTH, 20,
                Component.translatable("easyauthclient.config.address")));
        addressBox.setMaxLength(128);
        addressBox.setHint(Component.translatable("easyauthclient.config.address"));
        if (editAddress != null) {
            addressBox.setValue(editAddress);
            addressBox.active = false;
        }

        passwordBox = column.addChild(new EditBox(font, ROW_WIDTH, 20,
                Component.translatable("easyauthclient.config.password")));
        passwordBox.setMaxLength(512);
        passwordBox.setHint(Component.translatable("easyauthclient.config.password"));
        passwordBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.password.tooltip")));
        if (entry != null && entry.password != null) {
            passwordBox.setValue(entry.password);
        }

        totpBox = column.addChild(new EditBox(font, ROW_WIDTH, 20,
                Component.translatable("easyauthclient.config.totp")));
        totpBox.setMaxLength(128);
        totpBox.setHint(Component.translatable("easyauthclient.config.totp"));
        totpBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.totp.tooltip")));
        if (entry != null && entry.totpSecret != null) {
            totpBox.setValue(entry.totpSecret);
        }

        LinearLayout flagRow = column.addChild(LinearLayout.horizontal().spacing(8));
        autoLoginBox = flagRow.addChild(Checkbox.builder(Component.translatable("easyauthclient.config.autoLogin"), font)
                .selected(entry == null || entry.autoLogin)
                .build());
        autoRegisterBox = flagRow.addChild(Checkbox.builder(Component.translatable("easyauthclient.config.autoRegister"), font)
                .selected(entry != null && entry.autoRegister)
                .build());

        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        layout.arrangeElements();
    }

    /** Widget values → {@link #store}; keeps them across re-init (window resize). */
    private void commit() {
        String address = editAddress != null ? editAddress : RuleEngine.normalizeAddress(addressBox.getValue());
        if (address.isEmpty()) {
            return;
        }
        Credentials entry = store.servers.computeIfAbsent(address, a -> new Credentials());
        entry.password = passwordBox.getValue().isEmpty() ? null : passwordBox.getValue();
        entry.totpSecret = totpBox.getValue().isEmpty() ? null : totpBox.getValue();
        entry.autoLogin = autoLoginBox.selected();
        entry.autoRegister = autoRegisterBox.selected();
        editAddress = address;
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
