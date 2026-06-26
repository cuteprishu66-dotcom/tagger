package net.portaltiertagger.keybind;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_437;
import net.portaltiertagger.PortalTierTagger;
import net.portaltiertagger.config.ModConfig;
import org.lwjgl.glfw.GLFW;

public class ModKeybinds {

    private static class_304 toggleKey;
    private static class_304 refreshKey;
    private static class_304 openGuiKey; // NEW

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new class_304(
            "key.portaltiertagger.toggle",
            class_3675.class_307.field_1668,
            GLFW.GLFW_KEY_P,
            "category.portaltiertagger"
        ));

        refreshKey = KeyBindingHelper.registerKeyBinding(new class_304(
            "key.portaltiertagger.refresh",
            class_3675.class_307.field_1668,
            GLFW.GLFW_KEY_R,
            "category.portaltiertagger"
        ));

        // NEW: Right Shift opens config
        openGuiKey = KeyBindingHelper.registerKeyBinding(new class_304(
            "key.portaltiertagger.opengui",
            class_3675.class_307.field_1668,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.portaltiertagger"
        ));
    }

    public static void tick() {
        while (toggleKey.method_1436()) {
            ModConfig config = PortalTierTagger.getConfig();
            if (config != null) {
                config.enabled = !config.enabled;
                if (PortalTierTagger.getConfigManager() != null) PortalTierTagger.getConfigManager().save();
                if (PortalTierTagger.getClient() != null && PortalTierTagger.getClient().field_1724 != null) {
                    PortalTierTagger.getClient().field_1724.method_7353(
                        class_2561.method_43470("Tier Tagger: ").method_10852(
                            class_2561.method_43470(config.enabled ? "ON" : "OFF").method_27692(config.enabled ? class_124.field_1060 : class_124.field_1061)
                        ), true
                    );
                }
            }
        }

        while (refreshKey.method_1436()) {
            PortalTierTagger.triggerRefresh();
            if (PortalTierTagger.getClient() != null && PortalTierTagger.getClient().field_1724 != null) {
                PortalTierTagger.getClient().field_1724.method_7353(
                    class_2561.method_43470("Refreshing rankings...").method_27692(class_124.field_1054), true
                );
            }
        }

        // NEW: Open Config Menu
        while (openGuiKey.method_1436()) {
            class_310 client = PortalTierTagger.getClient();
            if (client != null && client.field_1755 == null) { // Only open if no other menu is open
                class_437 screen = AutoConfig.getConfigScreen(ModConfig.class, client.field_1755).get();
                client.method_1507(screen);
            }
        }
    }
}
