package xyz.nikitacartes.easyauth.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

@ConfigSerializable
public class DialogConfigV1 extends ConfigTemplate {

    @Comment("""
            Master switch for server-side Dialog windows (Minecraft 1.21.6+ only).
            On older clients/versions players always use the chat commands instead.""")
    public boolean enabled = true;

    @Comment("""

            Show the login window to registered players.""")
    public boolean login = true;

    @Comment("""

            Show the registration window to new players.""")
    public boolean register = true;

    @Comment("""

            Show the account menu (/account) window: change password, delete account, mark online, logout.""")
    public boolean account = true;

    @Comment("""

            Show the admin panel (/auth) window.""")
    public boolean admin = true;

    @Comment("""

            Allow closing the login/registration window with ESC.""")
    public boolean canCloseWithEscape = false;

    @Comment("""

            Let datapacks override the built-in windows.
            If a datapack provides a dialog with the matching id (e.g. easyauth:login),
            EasyAuth shows that custom layout instead of the default one.
            The input keys EasyAuth reads are: password, password_confirm, global_password,
            old_password, new_password, username, uuid, single_use.
            See https://github.com/NikitaCartes/EasyAuth/wiki for the full contract.""")
    public boolean allowDatapackOverride = true;

    public DialogConfigV1() {
        super("dialogs.conf", """
                ##                        ##
                ##        EasyAuth        ##
                ##  Dialog Configuration  ##
                ##                        ##""");
    }

    public static DialogConfigV1 create() {
        DialogConfigV1 config = loadConfig(DialogConfigV1.class, "dialogs.conf");
        if (config == null) {
            config = new DialogConfigV1();
            config.save();
        }
        return config;
    }

    public static DialogConfigV1 load() {
        DialogConfigV1 config = loadConfig(DialogConfigV1.class, "dialogs.conf");
        if (config == null) {
            LogError("dialogs.conf was not found, creating new one with default values");
            config = new DialogConfigV1();
            config.save();
        }
        return config;
    }

    @Override
    public void save() {
        save(DialogConfigV1.class, this);
    }
}
