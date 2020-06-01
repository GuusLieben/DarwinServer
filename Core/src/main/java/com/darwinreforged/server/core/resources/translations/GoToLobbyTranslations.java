package com.darwinreforged.server.core.resources.translations;

import com.darwinreforged.server.core.resources.ConfigSetting;

@ConfigSetting("gotolobby")
public class GoToLobbyTranslations {

    // TODO : Move to module
    public static final Translation GTL_WARPED = Translation.create("warped", "$1You have been teleported to the lobby as the world you were previously in is disabled");

    public GoToLobbyTranslations() {
    }

}
