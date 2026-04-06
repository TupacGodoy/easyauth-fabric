package com.cobblemania.economia.main;

import com.cobblemania.economia.ModeconomiaBootstrap;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobbleMania implements ModInitializer {
    public static final String MOD_ID = "cobblemania-economia";
    public static final Logger LOGGER = LoggerFactory.getLogger("CobbleMania-Economia");

    @Override
    public void onInitialize() {
        ModeconomiaBootstrap.initialize();
    }
}
