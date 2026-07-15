package xyz.nikitacartes.easyauth.client.screen;

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
import xyz.nikitacartes.easyauth.client.rules.AutoInputRule;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;
import xyz.nikitacartes.easyauth.client.rules.Rules;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Single auto-input rule editor; commits into the shared {@link Rules.Store} on close, which
 * {@link RuleListScreen} saves. Every field is shown for every trigger — the tooltips say
 * which trigger uses what, and {@link RuleEngine} ignores the rest.
 */
public class RuleEditScreen extends Screen {
    private static final int ROW_WIDTH = 310;
    private static final List<String> TRIGGERS = List.of("join", "chat", "timer", "leave");
    private static final List<String> SOURCES = List.of("any", "system", "player");
    /** Separates the lines of a multi-line rule inside the single-line send box. */
    private static final String SEND_SEPARATOR = " | ";

    private final RuleListScreen parent;
    private final Rules.Store store;
    private final AutoInputRule rule;
    private final String pendingAddress; // what the address box starts with
    private String currentAddress; // null = not in the store yet (new rule); set on first commit

    private EditBox addressBox;
    private String trigger;
    private String source;
    private EditBox matchBox;
    private EditBox sendBox;
    private EditBox delayBox;
    private EditBox cooldownBox;
    private EditBox maxRunsBox;

    /**
     * @param address the rule's server, or the address a new rule should default to
     * @param rule    the rule to edit, or null to add one
     */
    public RuleEditScreen(RuleListScreen parent, Rules.Store store, String address, AutoInputRule rule) {
        super(Component.translatable(rule != null ? "easyauthclient.rules.editRule" : "easyauthclient.rules.newRule"));
        this.parent = parent;
        this.store = store;
        this.rule = rule != null ? rule : new AutoInputRule();
        this.currentAddress = rule != null ? address : null;
        this.pendingAddress = address;
    }

    /** The trigger as one of {@link #TRIGGERS}; hand-edited files can hold anything. */
    public static String knownTrigger(String trigger) {
        String value = trigger == null ? "" : trigger.toLowerCase(Locale.ROOT);
        return TRIGGERS.contains(value) ? value : "join";
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

        addressBox = editBox(Component.translatable("easyauthclient.config.address"), ROW_WIDTH);
        addressBox.setMaxLength(128);
        addressBox.setHint(Component.translatable("easyauthclient.config.address"));
        addressBox.setValue(currentAddress != null ? currentAddress : pendingAddress);
        column.addChild(addressBox, 0, 0);

        GridLayout modeRow = new GridLayout().spacing(4);
        trigger = knownTrigger(rule.trigger);
        source = SOURCES.contains(rule.source) ? rule.source : "any";
        modeRow.addChild(cycleButton("easyauthclient.rules.trigger", TRIGGERS,
                () -> trigger, value -> trigger = value), 0, 0);
        modeRow.addChild(cycleButton("easyauthclient.rules.source", SOURCES,
                () -> source, value -> source = value), 0, 1);
        column.addChild(modeRow, 1, 0);

        matchBox = editBox(Component.translatable("easyauthclient.rules.match"), ROW_WIDTH);
        matchBox.setMaxLength(256);
        matchBox.setHint(Component.translatable("easyauthclient.rules.match"));
        matchBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.rules.match.tooltip")));
        matchBox.setValue(rule.match == null ? "" : rule.match);
        column.addChild(matchBox, 2, 0);

        sendBox = editBox(Component.translatable("easyauthclient.rules.send"), ROW_WIDTH);
        sendBox.setMaxLength(512);
        sendBox.setHint(Component.translatable("easyauthclient.rules.send"));
        sendBox.setTooltip(Tooltip.create(Component.translatable("easyauthclient.rules.send.tooltip")));
        sendBox.setValue(rule.send == null ? "" : String.join(SEND_SEPARATOR, rule.send));
        column.addChild(sendBox, 3, 0);

        GridLayout numberRow = new GridLayout().spacing(4);
        delayBox = numberBox("easyauthclient.rules.delay", String.valueOf(rule.delayMs));
        cooldownBox = numberBox("easyauthclient.rules.cooldown", String.valueOf(rule.cooldownMs));
        maxRunsBox = numberBox("easyauthclient.rules.maxRuns", String.valueOf(rule.maxRuns));
        numberRow.addChild(delayBox, 0, 0);
        numberRow.addChild(cooldownBox, 0, 1);
        numberRow.addChild(maxRunsBox, 0, 2);
        column.addChild(numberRow, 4, 0);

        layout.addToContents(column);
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        layout.arrangeElements();
    }

    private EditBox numberBox(String key, String value) {
        EditBox box = editBox(Component.translatable(key), (ROW_WIDTH - 8) / 3);
        box.setMaxLength(16);
        box.setHint(Component.translatable(key));
        box.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
        box.setValue(value);
        // ponytail: no input filter (its API moved around across our targets) — anything
        // unparseable falls back to the field default in commit().
        return box;
    }

    /**
     * A button that cycles through {@code values} and shows the current one. Vanilla's
     * CycleButton would do this, but its builder differs across our targets.
     */
    private Button cycleButton(String key, List<String> values, Supplier<String> get, Consumer<String> set) {
        Button button = Button.builder(cycleLabel(key, get.get()), b -> {
            set.accept(values.get((values.indexOf(get.get()) + 1) % values.size()));
            b.setMessage(cycleLabel(key, get.get()));
        }).width((ROW_WIDTH - 4) / 2).build();
        button.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
        return button;
    }

    private static Component cycleLabel(String key, String value) {
        return Component.translatable(key, Component.translatable(key + "." + value));
    }

    private EditBox editBox(Component message, int width) {
        //? if >=1.20.2 {
        return new EditBox(font, width, 20, message);
        //?} else {
        /*return new EditBox(font, 0, 0, width, 20, message);*/
        //?}
    }

    /** Empty or just "-" (the filter lets both through) means "default". */
    private static long parseLong(String text, long fallback) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Widget values → {@link #store}; keeps them across re-init (window resize). */
    private void commit() {
        String address = RuleEngine.normalizeAddress(addressBox.getValue());
        if (address.isEmpty()) {
            return;
        }
        rule.trigger = trigger;
        rule.source = source;
        rule.match = matchBox.getValue().isEmpty() ? null : matchBox.getValue();
        rule.send = Arrays.stream(sendBox.getValue().split("\\|"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        rule.delayMs = parseLong(delayBox.getValue(), 0);
        rule.cooldownMs = parseLong(cooldownBox.getValue(), 0);
        rule.maxRuns = (int) parseLong(maxRunsBox.getValue(), -1);
        if (!address.equals(currentAddress)) {
            if (currentAddress != null) {
                store.servers.get(currentAddress).rules.remove(rule);
            }
            store.servers.computeIfAbsent(address, a -> new Rules.ServerRules()).rules.add(rule);
            currentAddress = address;
        }
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
