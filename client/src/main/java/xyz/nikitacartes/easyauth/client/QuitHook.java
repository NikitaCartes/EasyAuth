package xyz.nikitacartes.easyauth.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.contents.TranslatableContents;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;

/**
 * Wraps the pause-menu disconnect button so "leave" rules run while the connection is still
 * open — the disconnect events of both loaders fire only after the channel closed, so anything
 * sent from there goes nowhere. The loader entrypoints do the screen-event wiring; this class
 * holds the shared detection + wrapping.
 * ponytail: menu-quit only — kicks, crashes and Alt+F4 close the connection with no send window.
 */
public final class QuitHook {

    private QuitHook() {
    }

    /** The multiplayer pause-menu disconnect button, identified language-independently. */
    public static boolean isDisconnect(AbstractWidget widget) {
        return widget.getMessage().getContents() instanceof TranslatableContents contents
                && "menu.disconnect".equals(contents.getKey());
    }

    /** A visually identical button that fires the "leave" rules before the original action. */
    public static Button wrap(Button original) {
        return Button.builder(original.getMessage(), button -> {
                    RuleEngine.onQuit();
                    //? if >=1.21.9 {
                    // Synthetic Enter press (257 = GLFW_KEY_ENTER); the disconnect action ignores it.
                    original.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                    //?} else {
                    /*original.onPress();*/
                    //?}
                })
                .bounds(original.getX(), original.getY(), original.getWidth(), original.getHeight())
                .build();
    }
}
