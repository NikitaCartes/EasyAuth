package xyz.nikitacartes.easyauth.client;

//? if fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import xyz.nikitacartes.easyauth.client.screen.ConfigScreen;

/** Loaded by ModMenu via the "modmenu" entrypoint; never touched when ModMenu is absent. */
public class EasyAuthClientModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
//?}
