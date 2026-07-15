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
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import xyz.nikitacartes.easyauth.client.rules.Credentials;
import xyz.nikitacartes.easyauth.client.rules.Vault;

/** Global auto-auth settings editor; commits into the shared {@link Credentials.Store} on close. */
public class GlobalSettingsScreen extends Screen {
    private static final int ROW_WIDTH = 310;
    private static final int REVEAL_BUTTON_WIDTH = 60;

    private final ConfigScreen parent;
    private final Credentials.Store store;
    private boolean showPassword;

    private Checkbox autoLoginBox;
    private Checkbox autoRegisterBox;
    private Checkbox useSessionTokenBox;
    private Checkbox usePasskeyBox;
    private EditBox defaultPasswordBox;
    private EditBox loginCommandBox;
    private EditBox registerCommandBox;
    private EditBox totpCommandBox;
    // Still-encrypted (locked) original: shown as an empty box with a "(locked)" hint and
    // written back on commit unless the player types a replacement.
    private String lockedDefaultPassword;

    public GlobalSettingsScreen(ConfigScreen parent, Credentials.Store store) {
        super(Component.translatable("easyauthclient.config.globalSettings"));
        this.parent = parent;
        this.store = store;
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

        GridLayout flagRow = new GridLayout().spacing(8);
        autoLoginBox = checkbox(Component.translatable("easyauthclient.config.globalAutoLogin"), store.autoLogin);
        autoLoginBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.globalAutoLogin.tooltip")));
        autoRegisterBox = checkbox(Component.translatable("easyauthclient.config.globalAutoRegister"), store.autoRegister);
        autoRegisterBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.globalAutoRegister.tooltip")));
        flagRow.addChild(autoLoginBox, 0, 0);
        flagRow.addChild(autoRegisterBox, 0, 1);
        column.addChild(flagRow, 0, 0);

        GridLayout keyRow = new GridLayout().spacing(8);
        useSessionTokenBox = checkbox(Component.translatable("easyauthclient.config.useSessionToken"), store.useSessionToken);
        useSessionTokenBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.useSessionToken.tooltip")));
        usePasskeyBox = checkbox(Component.translatable("easyauthclient.config.usePasskey"), store.usePasskey);
        usePasskeyBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.usePasskey.tooltip")));
        keyRow.addChild(useSessionTokenBox, 0, 0);
        keyRow.addChild(usePasskeyBox, 0, 1);
        column.addChild(keyRow, 1, 0);

        lockedDefaultPassword = Vault.isEncrypted(store.defaultPassword) ? store.defaultPassword : null;
        defaultPasswordBox = editBox(Component.translatable("easyauthclient.config.defaultPassword"),
                ROW_WIDTH - REVEAL_BUTTON_WIDTH - 4);
        defaultPasswordBox.setMaxLength(512);
        defaultPasswordBox.setHint(Component.translatable(lockedDefaultPassword != null
                ? "easyauthclient.config.locked"
                : "easyauthclient.config.defaultPassword"));
        defaultPasswordBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.defaultPassword.tooltip")));
        if (store.defaultPassword != null && lockedDefaultPassword == null) {
            defaultPasswordBox.setValue(store.defaultPassword);
        }
        //? if >=1.21.9 {
        defaultPasswordBox.addFormatter(this::maskPassword);
        //?} else {
        /*defaultPasswordBox.setFormatter(this::maskPassword);*/
        //?}
        GridLayout passwordRow = new GridLayout().spacing(4);
        passwordRow.addChild(defaultPasswordBox, 0, 0);
        passwordRow.addChild(Button.builder(revealLabel(), button -> {
            showPassword = !showPassword;
            button.setMessage(revealLabel());
        }).width(REVEAL_BUTTON_WIDTH).build(), 0, 1);
        column.addChild(passwordRow, 2, 0);

        loginCommandBox = editBox(Component.translatable("easyauthclient.config.loginCommand"), ROW_WIDTH);
        loginCommandBox.setMaxLength(256);
        loginCommandBox.setHint(Component.translatable("easyauthclient.config.loginCommand"));
        loginCommandBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.loginCommand.tooltip")));
        loginCommandBox.setValue(store.loginCommand == null ? "" : store.loginCommand);
        column.addChild(loginCommandBox, 3, 0);

        registerCommandBox = editBox(Component.translatable("easyauthclient.config.registerCommand"), ROW_WIDTH);
        registerCommandBox.setMaxLength(256);
        registerCommandBox.setHint(Component.translatable("easyauthclient.config.registerCommand"));
        registerCommandBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.registerCommand.tooltip")));
        registerCommandBox.setValue(store.registerCommand == null ? "" : store.registerCommand);
        column.addChild(registerCommandBox, 4, 0);

        totpCommandBox = editBox(Component.translatable("easyauthclient.config.totpCommand"), ROW_WIDTH);
        totpCommandBox.setMaxLength(256);
        totpCommandBox.setHint(Component.translatable("easyauthclient.config.totpCommand"));
        totpCommandBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.config.totpCommand.tooltip")));
        totpCommandBox.setValue(store.totpCommand == null ? "" : store.totpCommand);
        column.addChild(totpCommandBox, 5, 0);

        column.addChild(Button.builder(Component.translatable("easyauthclient.storage.title"), button -> {
            commit();
            ConfigScreen.open(new StorageScreen(this, parent, store));
        }).width(ROW_WIDTH).build(), 6, 0);

        layout.addToContents(column);
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        layout.arrangeElements();
    }

    private EditBox editBox(Component message, int width) {
        //? if >=1.20.2 {
        return new EditBox(font, width, 20, message);
        //?} else {
        /*return new EditBox(font, 0, 0, width, 20, message);*/
        //?}
    }

    private Component revealLabel() {
        return Component.translatable(showPassword ? "easyauthclient.config.hide" : "easyauthclient.config.show");
    }

    /** Display formatter for the default-password box: asterisks unless Show is toggled on. */
    private FormattedCharSequence maskPassword(String text, int offset) {
        return FormattedCharSequence.forward(showPassword ? text : "*".repeat(text.length()), Style.EMPTY);
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
        store.autoLogin = autoLoginBox.selected();
        store.autoRegister = autoRegisterBox.selected();
        store.useSessionToken = useSessionTokenBox.selected();
        store.usePasskey = usePasskeyBox.selected();
        // An empty box means "keep" for a locked value (there is nothing to redisplay).
        store.defaultPassword = defaultPasswordBox.getValue().isEmpty() && lockedDefaultPassword != null
                ? lockedDefaultPassword
                : defaultPasswordBox.getValue();
        store.loginCommand = loginCommandBox.getValue();
        store.registerCommand = registerCommandBox.getValue();
        store.totpCommand = totpCommandBox.getValue();
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
