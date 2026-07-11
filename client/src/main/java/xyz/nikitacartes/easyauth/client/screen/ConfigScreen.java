package xyz.nikitacartes.easyauth.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
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
        //? if >=1.20.5 {
        layout.addTitleHeader(title, font);
        //?} else {
        /*layout.addToHeader(new StringWidget(title, font));*/
        //?}

        ServerList serverList = new ServerList(minecraft);
        //? if >=1.20.3 {
        list = layout.addToContents(serverList);
        //?} else {
        /*list = serverList;
        addRenderableWidget(serverList);*/
        //?}

        // GridLayout (identical API 1.19.4→26.x) instead of LinearLayout, which lacks the
        // vertical()/horizontal() factories before 1.20.2.
        GridLayout footer = new GridLayout().spacing(8);
        footer.addChild(new StringWidget(Component.translatable("easyauthclient.config.rulesHint"), font), 0, 0, 1, 3);
        footer.addChild(Button.builder(Component.translatable("easyauthclient.config.newServer"),
                button -> open(new ServerEditScreen(this, store, null))).width(100).build(), 1, 0);
        footer.addChild(Button.builder(Component.translatable("easyauthclient.config.edit"),
                button -> editSelected()).width(100).build(), 1, 1);
        footer.addChild(Button.builder(Component.translatable("easyauthclient.config.remove"),
                button -> removeSelected()).width(100).build(), 1, 2);
        footer.addChild(Button.builder(Component.translatable("easyauthclient.config.globalSettings"),
                button -> open(new GlobalSettingsScreen(this, store))).width(208).build(), 2, 0, 1, 2);
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(100).build(), 2, 2);
        layout.addToFooter(footer);

        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (list != null) {
            //? if >=1.20.5 {
            list.updateSize(width, layout);
            //?} else if <1.20.3 {
            /*list.updateSize(width, height, 33, height - FOOTER_HEIGHT);*/
            //?}
            // 1.20.3-1.20.4: the list is a layout child (addToContents), sized by arrangeElements above.
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
        store.servers.values().removeIf(entry -> entry.password == null && entry.totpSecret == null
                && entry.sessionToken == null && entry.passkeyPrivate == null);
        Credentials.save(RuleEngine.getCredentialsFile(), store);
        open(parent);
    }

    private class ServerList extends ObjectSelectionList<ServerList.Entry> {
        ServerList(Minecraft minecraft) {
            //? if >=1.20.3 {
            super(minecraft, ConfigScreen.this.width, ConfigScreen.this.height - 33 - FOOTER_HEIGHT, 33, 18);
            //?} else {
            /*super(minecraft, ConfigScreen.this.width, ConfigScreen.this.height, 33, ConfigScreen.this.height - FOOTER_HEIGHT, 18);*/
            //?}
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

            //? if >=26.1 {
            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int y = this.getContentYMiddle() - 9 / 2;
                graphics.text(ConfigScreen.this.font, address, this.getContentX(), y, -1);
                graphics.text(ConfigScreen.this.font, summary,
                        this.getContentRight() - ConfigScreen.this.font.width(summary), y, 0xFFAAAAAA);
            }
            //?} else if >=1.21.9 {
            /*@Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int y = this.getContentYMiddle() - 9 / 2;
                graphics.drawString(ConfigScreen.this.font, address, this.getContentX(), y, -1);
                graphics.drawString(ConfigScreen.this.font, summary,
                        this.getContentRight() - ConfigScreen.this.font.width(summary), y, 0xFFAAAAAA);
            }
            *///?} else if >=1.20 {
            /*@Override
            public void render(GuiGraphics graphics, int index, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float a) {
                int y = top + (entryHeight - 9) / 2;
                graphics.drawString(ConfigScreen.this.font, address, left, y, -1);
                graphics.drawString(ConfigScreen.this.font, summary,
                        left + entryWidth - ConfigScreen.this.font.width(summary), y, 0xFFAAAAAA);
            }
            *///?} else {
            /*@Override
            public void render(PoseStack poseStack, int index, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float a) {
                int y = top + (entryHeight - 9) / 2;
                ConfigScreen.this.font.draw(poseStack, address, left, y, -1);
                ConfigScreen.this.font.draw(poseStack, summary,
                        left + entryWidth - ConfigScreen.this.font.width(summary), y, 0xFFAAAAAA);
            }
            *///?}

            //? if >=1.21.9 {
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
            //?} else {
            /*@Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 257 || keyCode == 335) { // ENTER, KP_ENTER
                    ServerList.this.setSelected(this);
                    editSelected();
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            *///?}
            // ponytail: pre-1.21.9 keeps single-click select (list default) + Enter/Edit button;
            // double-click-to-edit only where MouseButtonEvent hands it to us for free.

            @Override
            public Component getNarration() {
                return Component.literal(address);
            }
        }
    }
}
