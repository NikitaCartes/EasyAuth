package xyz.nikitacartes.easyauth.client.screen;

//? if <1.21.11 {
/*import net.minecraft.client.Minecraft;
*///?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import xyz.nikitacartes.easyauth.client.rules.Credentials;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;
import xyz.nikitacartes.easyauth.client.rules.Vault;

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
    // Still-encrypted (locked) originals: shown as an empty box with a "(locked)" hint and
    // written back on commit unless the player types a replacement.
    private String lockedPassword;
    private String lockedTotp;

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
        //? if >=1.20.5 {
        layout.addTitleHeader(title, font);
        //?} else {
        /*layout.addToHeader(new StringWidget(title, font));*/
        //?}
        // GridLayout, not LinearLayout: same API on every target (LinearLayout lacks vertical() before 1.20.2).
        GridLayout column = new GridLayout().spacing(4);
        Credentials entry = editAddress != null ? store.servers.get(editAddress) : null;

        addressBox = editBox(Component.translatable("easyauthclient.config.address"));
        addressBox.setMaxLength(128);
        addressBox.setHint(Component.translatable("easyauthclient.config.address"));
        if (editAddress != null) {
            addressBox.setValue(editAddress);
            addressBox.active = false;
        }
        column.addChild(addressBox, 0, 0);

        lockedPassword = entry != null && Vault.isEncrypted(entry.password) ? entry.password : null;
        lockedTotp = entry != null && Vault.isEncrypted(entry.totpSecret) ? entry.totpSecret : null;

        passwordBox = editBox(Component.translatable("easyauthclient.config.password"));
        passwordBox.setMaxLength(512);
        passwordBox.setHint(Component.translatable(lockedPassword != null
                ? "easyauthclient.config.locked"
                : "easyauthclient.config.password"));
        passwordBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.password.tooltip")));
        if (entry != null && entry.password != null && lockedPassword == null) {
            passwordBox.setValue(entry.password);
        }
        column.addChild(passwordBox, 1, 0);

        totpBox = editBox(Component.translatable("easyauthclient.config.totp"));
        totpBox.setMaxLength(128);
        totpBox.setHint(Component.translatable(lockedTotp != null
                ? "easyauthclient.config.locked"
                : "easyauthclient.config.totp"));
        totpBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.totp.tooltip")));
        if (entry != null && entry.totpSecret != null && lockedTotp == null) {
            totpBox.setValue(entry.totpSecret);
        }
        column.addChild(totpBox, 2, 0);

        GridLayout flagRow = new GridLayout().spacing(8);
        autoLoginBox = checkbox(Component.translatable("easyauthclient.config.autoLogin"), entry == null || entry.autoLogin);
        autoRegisterBox = checkbox(Component.translatable("easyauthclient.config.autoRegister"), entry != null && entry.autoRegister);
        flagRow.addChild(autoLoginBox, 0, 0);
        flagRow.addChild(autoRegisterBox, 0, 1);
        column.addChild(flagRow, 3, 0);

        // Forget the server-issued session token and the local passkey pair (e.g. after
        // /account passkey revoke on the server, or to force a fresh password login).
        if (entry != null && (entry.sessionToken != null || entry.passkeyPrivate != null)) {
            Credentials clearTarget = entry;
            Button clearButton = Button.builder(Component.translatable("easyauthclient.config.clearKeys"), button -> {
                clearTarget.sessionToken = null;
                clearTarget.passkeyPublic = null;
                clearTarget.passkeyPrivate = null;
                button.active = false;
            }).width(ROW_WIDTH).build();
            clearButton.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.clearKeys.tooltip")));
            column.addChild(clearButton, 4, 0);
        }

        layout.addToContents(column);
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        layout.arrangeElements();
    }

    private EditBox editBox(Component message) {
        //? if >=1.20.2 {
        return new EditBox(font, ROW_WIDTH, 20, message);
        //?} else {
        /*return new EditBox(font, 0, 0, ROW_WIDTH, 20, message);*/
        //?}
    }

    private Checkbox checkbox(Component message, boolean selected) {
        //? if >=1.20.3 {
        return Checkbox.builder(message, font).selected(selected).build();
        //?} else {
        /*return new Checkbox(0, 0, font.width(message) + 24, 20, message, selected);*/
        //?}
    }

    /** Widget values → {@link #store}; keeps them across re-init (window resize). */
    private void commit() {
        String address = editAddress != null ? editAddress : RuleEngine.normalizeAddress(addressBox.getValue());
        if (address.isEmpty()) {
            return;
        }
        Credentials entry = store.servers.computeIfAbsent(address, a -> new Credentials());
        // An empty box means "keep" for a locked value (there is nothing to redisplay) and
        // "forget" otherwise; a typed value always replaces.
        entry.password = passwordBox.getValue().isEmpty() ? lockedPassword : passwordBox.getValue();
        entry.totpSecret = totpBox.getValue().isEmpty() ? lockedTotp : totpBox.getValue();
        entry.autoLogin = autoLoginBox.selected();
        entry.autoRegister = autoRegisterBox.selected();
        editAddress = address;
    }

    //? if >=1.21.11 {
    @Override
    public void resize(int width, int height) {
        commit();
        super.resize(width, height);
    }
    //?} else {
    /*@Override
    public void resize(Minecraft minecraft, int width, int height) {
        commit();
        super.resize(minecraft, width, height);
    }
    *///?}

    @Override
    public void onClose() {
        commit();
        ConfigScreen.open(parent);
    }
}
