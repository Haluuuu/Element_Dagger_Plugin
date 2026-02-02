package com.haluuu;


import com.haluuu.interactions.TeleportDagger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class Main extends JavaPlugin {
    private static Main instance;

    public static Main get() {
        return instance;
    }
    public Main(@Nonnull JavaPluginInit init) {
        super(init);
    }

    protected void setup() {
        instance = this;
        Interaction.CODEC.register("TeleportDagger", TeleportDagger.class, TeleportDagger.CODEC);
    }
}