package net.unfamily.another_quarries.integration.anotherdynamics;

import net.neoforged.fml.ModList;

public final class AnotherDynamicsIntegration {
    private static final String MOD_ID = "another_dynamics";

    private AnotherDynamicsIntegration() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }
}
