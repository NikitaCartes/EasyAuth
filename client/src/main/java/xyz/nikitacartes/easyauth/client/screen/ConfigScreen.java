package xyz.nikitacartes.easyauth.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import xyz.nikitacartes.easyauth.client.rules.Credentials;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Credentials editor shared by ModMenu (Fabric) and the NeoForge mods-list Config button.
 * Forgetting a server = clearing its password and TOTP fields (empty entries are pruned
 * on save). Auto-input rules are edited in rules.json directly.
 * ponytail: no in-game rule editor until someone asks for one.
 */
public class ConfigScreen extends Screen {
    private static final String NEW_SERVER = "+";
    private static final int ROW_WIDTH = 310;

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final Map<String, Credentials> servers;
    private String selected;

    private EditBox addressBox;
    private EditBox passwordBox;
    private EditBox totpBox;
    private Checkbox autoLoginBox;
    private Checkbox autoRegisterBox;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("easyauthclient.config.title"));
        this.parent = parent;
        this.servers = Credentials.loadAll(RuleEngine.getCredentialsFile());
        ServerData current = Minecraft.getInstance().getCurrentServer();
        String address = current != null && current.ip != null ? RuleEngine.normalizeAddress(current.ip) : null;
        if (address != null && !address.isEmpty()) {
            servers.computeIfAbsent(address, a -> new Credentials());
            this.selected = address;
        } else {
            this.selected = servers.isEmpty() ? NEW_SERVER : new TreeSet<>(servers.keySet()).first();
        }
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);
        LinearLayout column = layout.addToContents(LinearLayout.vertical().spacing(4));
        Credentials entry = servers.get(selected);

        List<String> values = new ArrayList<>(new TreeSet<>(servers.keySet()));
        values.add(NEW_SERVER);
        column.addChild(CycleButton.<String>builder(value -> NEW_SERVER.equals(value)
                        ? Component.translatable("easyauthclient.config.newServer")
                        : Component.literal(value), selected)
                .withValues(values)
                .create(0, 0, ROW_WIDTH, 20, Component.translatable("easyauthclient.config.server"),
                        (button, value) -> selectServer(value)));

        addressBox = column.addChild(new EditBox(font, ROW_WIDTH, 20,
                Component.translatable("easyauthclient.config.address")));
        addressBox.setMaxLength(128);
        addressBox.setHint(Component.translatable("easyauthclient.config.address"));
        if (!NEW_SERVER.equals(selected)) {
            addressBox.setValue(selected);
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

        autoLoginBox = column.addChild(Checkbox.builder(Component.translatable("easyauthclient.config.autoLogin"), font)
                .selected(entry == null || entry.autoLogin)
                .build());
        autoRegisterBox = column.addChild(Checkbox.builder(Component.translatable("easyauthclient.config.autoRegister"), font)
                .selected(entry != null && entry.autoRegister)
                .build());

        column.addChild(new StringWidget(ROW_WIDTH, 9,
                Component.translatable("easyauthclient.config.rulesHint"), font));

        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }

    private void selectServer(String value) {
        commitFields();
        selected = value;
        rebuildWidgets();
    }

    /** Writes the widget values into {@link #servers}; keeps {@link #selected} pointing at them. */
    private void commitFields() {
        String address = NEW_SERVER.equals(selected)
                ? RuleEngine.normalizeAddress(addressBox.getValue())
                : selected;
        if (address.isEmpty()) {
            return;
        }
        Credentials entry = servers.computeIfAbsent(address, a -> new Credentials());
        entry.password = passwordBox.getValue().isEmpty() ? null : passwordBox.getValue();
        entry.totpSecret = totpBox.getValue().isEmpty() ? null : totpBox.getValue();
        entry.autoLogin = autoLoginBox.selected();
        entry.autoRegister = autoRegisterBox.selected();
        selected = address;
    }

    @Override
    public void resize(int width, int height) {
        commitFields();
        super.resize(width, height);
    }

    @Override
    public void onClose() {
        commitFields();
        servers.values().removeIf(entry -> entry.password == null && entry.totpSecret == null);
        Credentials.saveAll(RuleEngine.getCredentialsFile(), servers);
        //? if >=26.2 {
        minecraft.gui.setScreen(parent);
        //?} else {
        /*minecraft.setScreen(parent);*/
        //?}
    }
}
