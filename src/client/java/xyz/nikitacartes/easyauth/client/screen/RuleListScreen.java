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
import xyz.nikitacartes.easyauth.client.rules.AutoInputRule;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;
import xyz.nikitacartes.easyauth.client.rules.Rules;
import xyz.nikitacartes.easyauth.client.rules.Vault;

import java.util.TreeSet;

/**
 * Auto-input rules overview: one row per rule, across all servers. Editing happens in
 * {@link RuleEditScreen}; this screen owns the shared {@link Rules.Store} and saves it on close.
 */
public class RuleListScreen extends Screen {
    private static final int FOOTER_HEIGHT = 72; // hint row + two button rows

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, FOOTER_HEIGHT);
    private final Rules.Store store;
    private RuleList list;

    public RuleListScreen(Screen parent) {
        super(Component.translatable("easyauthclient.rules.title"));
        this.parent = parent;
        this.store = Rules.load(Vault.rulesFile());
    }

    @Override
    protected void init() {
        //? if >=1.20.5 {
        layout.addTitleHeader(title, font);
        //?} else {
        /*layout.addToHeader(new StringWidget(title, font));*/
        //?}

        RuleList ruleList = new RuleList(minecraft);
        //? if >=1.20.3 {
        list = layout.addToContents(ruleList);
        //?} else {
        /*list = ruleList;
        addRenderableWidget(ruleList);*/
        //?}

        // GridLayout, not LinearLayout: same API on every target (LinearLayout lacks vertical() before 1.20.2).
        GridLayout buttons = new GridLayout().spacing(8);
        buttons.addChild(Button.builder(Component.translatable("easyauthclient.rules.newRule"),
                button -> ConfigScreen.open(new RuleEditScreen(this, store, currentServerAddress(), null)))
                .width(100).build(), 0, 0);
        buttons.addChild(Button.builder(Component.translatable("easyauthclient.config.edit"),
                button -> editSelected()).width(100).build(), 0, 1);
        buttons.addChild(Button.builder(Component.translatable("easyauthclient.config.remove"),
                button -> removeSelected()).width(100).build(), 0, 2);
        buttons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(100).build(), 1, 1);

        GridLayout footer = new GridLayout().spacing(8);
        footer.addChild(new StringWidget(Component.translatable("easyauthclient.rules.joinHint"), font),
                0, 0, footer.newCellSettings().alignHorizontallyCenter());
        footer.addChild(buttons, 1, 0);
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

    /** The server the player is on, so a new rule starts out aimed at it; empty when offline. */
    private static String currentServerAddress() {
        ServerData current = Minecraft.getInstance().getCurrentServer();
        return current != null && current.ip != null ? RuleEngine.normalizeAddress(current.ip) : "";
    }

    private void editSelected() {
        RuleList.Entry selected = list.getSelected();
        if (selected != null) {
            ConfigScreen.open(new RuleEditScreen(this, store, selected.address, selected.rule));
        }
    }

    private void removeSelected() {
        RuleList.Entry selected = list.getSelected();
        if (selected != null) {
            store.servers.get(selected.address).rules.remove(selected.rule);
            rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        Rules.pruneEmpty(store);
        Rules.save(Vault.rulesFile(), store);
        ConfigScreen.open(parent);
    }

    private class RuleList extends ObjectSelectionList<RuleList.Entry> {
        RuleList(Minecraft minecraft) {
            //? if >=1.20.3 {
            super(minecraft, RuleListScreen.this.width, RuleListScreen.this.height - 33 - FOOTER_HEIGHT, 33, 18);
            //?} else {
            /*super(minecraft, RuleListScreen.this.width, RuleListScreen.this.height, 33, RuleListScreen.this.height - FOOTER_HEIGHT, 18);*/
            //?}
            for (String address : new TreeSet<>(store.servers.keySet())) {
                for (AutoInputRule rule : store.servers.get(address).rules) {
                    if (rule != null) {
                        addEntry(new Entry(address, rule));
                    }
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 340;
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            final String address;
            final AutoInputRule rule;
            private final String summary;

            Entry(String address, AutoInputRule rule) {
                this.address = address;
                this.rule = rule;
                String trigger = I18n.get("easyauthclient.rules.trigger." + RuleEditScreen.knownTrigger(rule.trigger));
                String first = rule.send == null || rule.send.isEmpty() ? "" : rule.send.get(0);
                this.summary = first.isEmpty() ? trigger : trigger + " • " + first;
            }

            //? if >=26.1 {
            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int y = this.getContentYMiddle() - 9 / 2;
                graphics.text(RuleListScreen.this.font, address, this.getContentX(), y, -1);
                graphics.text(RuleListScreen.this.font, summary,
                        this.getContentRight() - RuleListScreen.this.font.width(summary), y, 0xFFAAAAAA);
            }
            //?} else if >=1.21.9 {
            /*@Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int y = this.getContentYMiddle() - 9 / 2;
                graphics.drawString(RuleListScreen.this.font, address, this.getContentX(), y, -1);
                graphics.drawString(RuleListScreen.this.font, summary,
                        this.getContentRight() - RuleListScreen.this.font.width(summary), y, 0xFFAAAAAA);
            }
            *///?} else if >=1.20 {
            /*@Override
            public void render(GuiGraphics graphics, int index, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float a) {
                int y = top + (entryHeight - 9) / 2;
                graphics.drawString(RuleListScreen.this.font, address, left, y, -1);
                graphics.drawString(RuleListScreen.this.font, summary,
                        left + entryWidth - RuleListScreen.this.font.width(summary), y, 0xFFAAAAAA);
            }
            *///?} else {
            /*@Override
            public void render(PoseStack poseStack, int index, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float a) {
                int y = top + (entryHeight - 9) / 2;
                RuleListScreen.this.font.draw(poseStack, address, left, y, -1);
                RuleListScreen.this.font.draw(poseStack, summary,
                        left + entryWidth - RuleListScreen.this.font.width(summary), y, 0xFFAAAAAA);
            }
            *///?}

            //? if >=1.21.9 {
            @Override
            public boolean keyPressed(KeyEvent event) {
                if (event.isSelection()) {
                    RuleList.this.setSelected(this);
                    editSelected();
                    return true;
                }
                return super.keyPressed(event);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                RuleList.this.setSelected(this);
                if (doubleClick) {
                    editSelected();
                }
                return super.mouseClicked(event, doubleClick);
            }
            //?} else {
            /*@Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 257 || keyCode == 335) { // ENTER, KP_ENTER
                    RuleList.this.setSelected(this);
                    editSelected();
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            *///?}

            @Override
            public Component getNarration() {
                return Component.literal(address + " " + summary);
            }
        }
    }
}
