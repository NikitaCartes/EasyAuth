package org.samo_lego.simpleauth.utils;

import net.minecraft.text.Text;

public interface PlayerAuth {
    void hidePosition(boolean hide);

    String getFakeUuid();

    void setAuthenticated(boolean authenticated);
    boolean isAuthenticated();

    Text getAuthMessage();

    boolean canSkipAuth();
}
