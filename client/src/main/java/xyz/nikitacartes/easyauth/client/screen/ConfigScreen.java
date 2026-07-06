package xyz.nikitacartes.easyauth.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import xyz.nikitacartes.easyauth.client.rules.Credentials;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Saved-credentials overview shared by ModMenu (Fabric) and the NeoForge mods-list Config
 * button: one row per server with its password and per-server flags. Editing happens in
 * {@link ServerEditScreen}, global auto-auth settings in {@link GlobalSettingsScreen}.
 * Auto-input rules are edited in rules.json directly.
 * ponytail: no in-game rule editor until someone asks for one.
 */
public class ConfigScreen extends Screen {
    private static final int FOOTER_HEIGHT = 64;

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, FOOTER_HEIGHT);
    private final Credentials.Store store;
    private ServerList list;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("easyauthclient.config.title"));
        this.parent = parent;
        this.store = Credentials.load(RuleEngine.getCredentialsFile());
    }

    /** The 26.1/26.2 setScreen split in one place; also used by the sub-screens. */
    static void open(Screen screen) {
        //? if >=26.2 {
        Minecraft.getInstance().gui.setScreen(screen);
        //?} else {
        /*Minecraft.getInstance().setScreen(screen);*/
        //?}
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);
        list = layout.addToContents(new ServerList(minecraft));

        LinearLayout footer = layout.addToFooter(LinearLayout.vertical()).spacing(4);
        footer.defaultCellSetting().alignHorizontallyCenter();
        footer.addChild(new StringWidget(Component.translatable("easyauthclient.config.rulesHint"), font));
        LinearLayout editRow = footer.addChild(LinearLayout.horizontal().spacing(8));
        editRow.addChild(Button.builder(Component.translatable("easyauthclient.config.newServer"),
                button -> open(new ServerEditScreen(this, store, null))).width(100).build());
        editRow.addChild(Button.builder(Component.translatable("easyauthclient.config.edit"),
                button -> editSelected()).width(100).build());
        editRow.addChild(Button.builder(Component.translatable("easyauthclient.config.remove"),
                button -> removeSelected()).width(100).build());
        LinearLayout doneRow = footer.addChild(LinearLayout.horizontal().spacing(8));
        doneRow.addChild(Button.builder(Component.translatable("easyauthclient.config.globalSettings"),
                button -> open(new GlobalSettingsScreen(this, store))).width(154).build());
        doneRow.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(154).build());

        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (list != null) {
            list.updateSize(width, layout);
        }
    }

    private void editSelected() {
        ServerList.Entry selected = list.getSelected();
        if (selected != null) {
            open(new ServerEditScreen(this, store, selected.address));
        }
    }

    private void removeSelected() {
        ServerList.Entry selected = list.getSelected();
        if (selected != null) {
            store.servers.remove(selected.address);
            rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        store.servers.values().removeIf(entry -> entry.password == null && entry.totpSecret == null);
        Credentials.save(RuleEngine.getCredentialsFile(), store);
        open(parent);
    }

    private class ServerList extends ObjectSelectionList<ServerList.Entry> {
        ServerList(Minecraft minecraft) {
            super(minecraft, ConfigScreen.this.width, ConfigScreen.this.height - 33 - FOOTER_HEIGHT, 33, 18);
            ServerData current = minecraft.getCurrentServer();
            String currentAddress = current != null && current.ip != null
                    ? RuleEngine.normalizeAddress(current.ip)
                    : null;
            for (String address : new TreeSet<>(store.servers.keySet())) {
                Entry entry = new Entry(address, store.servers.get(address));
                addEntry(entry);
                if (address.equals(currentAddress)) {
                    setSelected(entry);
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 340;
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            final String address;
            private final String summary;

            Entry(String address, Credentials credentials) {
                this.address = address;
                List<String> parts = new ArrayList<>();
                parts.add(credentials.password != null
                        ? credentials.password
                        : I18n.get("easyauthclient.config.noPassword"));
                if (credentials.totpSecret != null) {
                    parts.add("TOTP");
                }
                if (!credentials.autoLogin) {
                    parts.add(I18n.get("easyauthclient.config.tag.noAutoLogin"));
                }
                if (credentials.autoRegister) {
                    parts.add(I18n.get("easyauthclient.config.tag.autoRegister"));
                }
                this.summary = String.join(" • ", parts);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int y = this.getContentYMiddle() - 9 / 2;
                graphics.text(ConfigScreen.this.font, address, this.getContentX(), y, -1);
                graphics.text(ConfigScreen.this.font, summary,
                        this.getContentRight() - ConfigScreen.this.font.width(summary), y, 0xFFAAAAAA);
            }

            @Override
            public boolean keyPressed(KeyEvent event) {
                if (event.isSelection()) {
                    ServerList.this.setSelected(this);
                    editSelected();
                    return true;
                }
                return super.keyPressed(event);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                ServerList.this.setSelected(this);
                if (doubleClick) {
                    editSelected();
                }
                return super.mouseClicked(event, doubleClick);
            }

            @Override
            public Component getNarration() {
                return Component.literal(address);
            }
        }
    }
}
