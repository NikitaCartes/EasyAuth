package xyz.nikitacartes.easyauth.client.screen;

import net.minecraft.ChatFormatting;
//? if <1.21.11 {
/*import net.minecraft.client.Minecraft;
*///?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import xyz.nikitacartes.easyauth.client.rules.Credentials;
import xyz.nikitacartes.easyauth.client.rules.Vault;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Storage &amp; security: the data-dir/key-file locations (point several instances at
 * the same paths to share servers, rules and passwords) and the optional master password on
 * top of the automatic key protection (DPAPI on Windows, plain 0600 key file elsewhere).
 */
public class StorageScreen extends Screen {
    private static final int ROW_WIDTH = 310;
    private static final int HOME_BUTTON_WIDTH = 70;
    private static final int PATH_BOX_WIDTH = ROW_WIDTH - HOME_BUTTON_WIDTH - 4;

    private final GlobalSettingsScreen parent;
    private final ConfigScreen config;
    private final Credentials.Store store;

    private EditBox dataDirBox;
    private EditBox keyFileBox;
    private EditBox newPasswordBox;
    private EditBox repeatPasswordBox;
    private StringWidget statusText;
    // Values survive rebuildWidgets()/resize; status survives one rebuild (set by the buttons).
    private String pendingDataDir;
    private String pendingKeyFile;
    private Component pendingStatus = Component.empty();

    public StorageScreen(GlobalSettingsScreen parent, ConfigScreen config, Credentials.Store store) {
        super(Component.translatable("easyauthclient.storage.title"));
        this.parent = parent;
        this.config = config;
        this.store = store;
        this.pendingDataDir = Vault.dataDirOverride();
        this.pendingKeyFile = Vault.keyFileOverride();
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
        int row = 0;

        dataDirBox = editBox(Component.translatable("easyauthclient.storage.dataDir"), PATH_BOX_WIDTH);
        dataDirBox.setHint(Component.literal(Vault.defaultDataDir().toString()));
        dataDirBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.storage.dataDir.tooltip")));
        dataDirBox.setValue(pendingDataDir);
        column.addChild(pathRow(dataDirBox, Vault.homeDataDir().toString()), row++, 0);

        keyFileBox = editBox(Component.translatable("easyauthclient.storage.keyFile"), PATH_BOX_WIDTH);
        keyFileBox.setHint(Component.literal(Vault.defaultKeyFile().toString()));
        keyFileBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.storage.keyFile.tooltip")));
        keyFileBox.setValue(pendingKeyFile);
        column.addChild(pathRow(keyFileBox, Vault.homeKeyFile().toString()), row++, 0);

        if (!Vault.encryptionEnabled()) {
            column.addChild(new StringWidget(ROW_WIDTH, 9,
                    Component.translatable("easyauthclient.storage.plaintextWarning").withStyle(ChatFormatting.RED),
                    font), row++, 0);
            column.addChild(Button.builder(Component.translatable("easyauthclient.storage.enableEncryption"),
                    button -> setEncryption(true)).width(ROW_WIDTH).build(), row++, 0);
        } else if (Vault.passwordProtected() && Vault.locked()) {
            column.addChild(Button.builder(Component.translatable("easyauthclient.unlock.unlock"), button -> {
                commit(); // path edits survive the round trip through the prompt
                ConfigScreen.open(new UnlockScreen(this));
            }).width(ROW_WIDTH).build(), row++, 0);
        } else if (!Vault.locked()) {
            newPasswordBox = editBox(Component.translatable("easyauthclient.storage.newPassword"), ROW_WIDTH);
            newPasswordBox.setHint(Component.translatable("easyauthclient.storage.newPassword"));
            column.addChild(newPasswordBox, row++, 0);
            repeatPasswordBox = editBox(Component.translatable("easyauthclient.storage.repeatPassword"), ROW_WIDTH);
            repeatPasswordBox.setHint(Component.translatable("easyauthclient.storage.repeatPassword"));
            column.addChild(repeatPasswordBox, row++, 0);
            column.addChild(Button.builder(Component.translatable(Vault.passwordProtected()
                            ? "easyauthclient.storage.changePassword"
                            : "easyauthclient.storage.setPassword"),
                    button -> applyPassword()).width(ROW_WIDTH).build(), row++, 0);
            if (Vault.passwordProtected()) {
                column.addChild(Button.builder(Component.translatable("easyauthclient.storage.removePassword"),
                        button -> removePassword()).width(ROW_WIDTH).build(), row++, 0);
            }
            Button disableButton = Button.builder(Component.translatable("easyauthclient.storage.disableEncryption"),
                    button -> setEncryption(false)).width(ROW_WIDTH).build();
            disableButton.setTooltip(Tooltip.create(Component.translatable("easyauthclient.storage.disableEncryption.tooltip")));
            column.addChild(disableButton, row++, 0);
        } else {
            // Locked without a master password: a DPAPI blob from another user/computer or a
            // corrupt key file. Nothing to type — only replacing the key file helps.
            column.addChild(new StringWidget(ROW_WIDTH, 9,
                    Component.translatable("easyauthclient.storage.keyUnavailable").withStyle(ChatFormatting.RED),
                    font), row++, 0);
        }

        statusText = new StringWidget(ROW_WIDTH, 9, pendingStatus, font);
        column.addChild(statusText, row, 0);
        pendingStatus = Component.empty();

        layout.addToContents(column);
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        layout.arrangeElements();
    }

    private EditBox editBox(Component message, int width) {
        //? if >=1.20.2 {
        EditBox box = new EditBox(font, width, 20, message);
        //?} else {
        /*EditBox box = new EditBox(font, 0, 0, width, 20, message);*/
        //?}
        box.setMaxLength(1024);
        return box;
    }

    /** A path EditBox with a button that fills in the shared home-folder location. */
    private GridLayout pathRow(EditBox box, String homePath) {
        GridLayout pathRow = new GridLayout().spacing(4);
        pathRow.addChild(box, 0, 0);
        pathRow.addChild(Button.builder(Component.translatable("easyauthclient.storage.useHome"),
                button -> box.setValue(homePath)).width(HOME_BUTTON_WIDTH).build(), 0, 1);
        return pathRow;
    }

    /**
     * Toggles encryption at rest and immediately re-saves the credentials in the new form.
     * Only reachable while the vault is unlocked (or already in plaintext mode), so the live
     * store holds usable plaintext to convert.
     */
    private void setEncryption(boolean enabled) {
        commit();
        Vault.setEncryptionEnabled(enabled);
        Credentials.save(Vault.credentialsFile(), store);
        pendingStatus = Component.translatable(enabled
                ? "easyauthclient.storage.encryptionEnabled"
                : "easyauthclient.storage.encryptionDisabled").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
        rebuildWidgets();
    }

    private void applyPassword() {
        String password = newPasswordBox.getValue();
        if (password.isEmpty()) {
            statusText.setMessage(Component.translatable("easyauthclient.storage.passwordEmpty").withStyle(ChatFormatting.RED));
            return;
        }
        if (!password.equals(repeatPasswordBox.getValue())) {
            statusText.setMessage(Component.translatable("easyauthclient.storage.mismatch").withStyle(ChatFormatting.RED));
            return;
        }
        if (Vault.setMasterPassword(password)) {
            commit();
            pendingStatus = Component.translatable("easyauthclient.storage.passwordSet").withStyle(ChatFormatting.GREEN);
            rebuildWidgets(); // the button row changes to change/remove
        }
    }

    private void removePassword() {
        if (Vault.clearMasterPassword()) {
            commit();
            pendingStatus = Component.translatable("easyauthclient.storage.passwordRemoved").withStyle(ChatFormatting.GREEN);
            rebuildWidgets();
        }
    }

    /** Widget values → fields; keeps them across re-init (resize, state rebuilds). */
    private void commit() {
        pendingDataDir = dataDirBox.getValue().trim();
        pendingKeyFile = keyFileBox.getValue().trim();
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
        boolean dataMoved = !pendingDataDir.equals(Vault.dataDirOverride());
        boolean keyMoved = !pendingKeyFile.equals(Vault.keyFileOverride());
        if (!dataMoved && !keyMoved) {
            ConfigScreen.open(parent);
            return;
        }
        boolean adoptExisting;
        try {
            Path targetData = pendingDataDir.isEmpty() ? Vault.defaultDataDir() : Path.of(pendingDataDir);
            // A store that already exists at the target wins (that is the sharing use case);
            // otherwise our current store is carried over.
            adoptExisting = dataMoved && Files.exists(targetData.resolve("credentials.json"));
        } catch (InvalidPathException e) {
            pendingStatus = Component.literal(e.getMessage()).withStyle(ChatFormatting.RED);
            rebuildWidgets();
            return;
        }
        Credentials.saveNow(Vault.credentialsFile(), store); // flush edits before any file copy
        if (!Vault.setLocations(pendingDataDir, pendingKeyFile)) {
            pendingStatus = Component.translatable("easyauthclient.storage.moveFailed").withStyle(ChatFormatting.RED);
            rebuildWidgets();
            return;
        }
        if (adoptExisting) {
            // Reload the whole config UI from the adopted store.
            ConfigScreen.open(new ConfigScreen(config.parent));
        } else {
            // Re-save under the (possibly new) key so the file at the new location is
            // readable on the next launch even if the key file changed.
            Credentials.saveNow(Vault.credentialsFile(), store);
            ConfigScreen.open(dataMoved ? new ConfigScreen(config.parent) : parent);
        }
    }
}
