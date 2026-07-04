package xyz.nikitacartes.easyauth.client;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyAuthClientFabric implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");

    @Override
    public void onInitializeClient() {
        LOGGER.info("EasyAuth Client loaded");
    }
}
//?}
