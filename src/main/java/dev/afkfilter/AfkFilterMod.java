package dev.afkfilter;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfkFilterMod implements ClientModInitializer {
    public static final String MOD_ID = "afk-filter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean filterEnabled = true;
    private static AfkFilterConfig config;
    private static KeyBinding toggleKeyBinding;

    @Override
    public void onInitializeClient() {
        config = AfkFilterConfig.load();
        filterEnabled = config.enabledByDefault;

        // Keybind F6 (konfigurierbar in Einstellungen → Steuerung → AFK Filter)
        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.afkfilter.toggle",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_F6,
                        "key.categories.afkfilter"
                )
        );

        // Keybind check
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKeyBinding.wasPressed()) {
                toggleFilter(client);
            }
        });

        // Client command /afkfilter
        ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);

        // Show message when joining server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                if (client.player != null) {
                    String keyName = toggleKeyBinding.getBoundKeyLocalizedText().getString();
                    client.player.sendMessage(
                            Text.literal("\u00a77[AFK Filter] \u00a7fLoaded! Press " + keyName + " or /afkfilter to toggle"),
                            false
                    );
                }
            });
        });

        LOGGER.info("AFK Filter Mod initialized. Press F6 to toggle. Pattern: '{}'", config.afkPattern);
    }

    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("afkfilter")
                .executes(context -> {
                    toggleFilter(MinecraftClient.getInstance());
                    return 1;
                })
        );
    }

    private void toggleFilter(MinecraftClient client) {
        filterEnabled = !filterEnabled;
        if (client.player != null) {
            String status = filterEnabled ? "\u00a7aON" : "\u00a7cOFF";
            client.player.sendMessage(
                    Text.literal("\u00a77[AFK Filter] " + status),
                    true
            );
        }
        LOGGER.info("AFK Filter toggled: {}", filterEnabled ? "enabled" : "disabled");
    }

    public static boolean isFilterEnabled() {
        return filterEnabled;
    }

    public static AfkFilterConfig getConfig() {
        return config;
    }
}
