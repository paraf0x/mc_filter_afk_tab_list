package dev.afkfilter;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfkFilterMod implements ClientModInitializer {
    public static final String MOD_ID = "afk-filter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean filterEnabled = true;
    private static AfkFilterConfig config;
    private static KeyMapping toggleKeyBinding;

    @Override
    public void onInitializeClient() {
        config = AfkFilterConfig.load();
        filterEnabled = config.enabledByDefault;

        // Keybind F6 (konfigurierbar in Einstellungen → Steuerung → AFK Filter)
        // MC 26.x: keybind categories are KeyMapping.Category objects, no longer raw strings.
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        toggleKeyBinding = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.afkfilter.toggle",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_F6,
                        category
                )
        );

        // Keybind check
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKeyBinding.consumeClick()) {
                toggleFilter(client);
            }
        });

        // Client command /afkfilter
        ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);

        // Show message when joining server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                if (client.player != null) {
                    String keyName = toggleKeyBinding.getTranslatedKeyMessage().getString();
                    client.player.sendSystemMessage(
                            Component.literal("§7[AFK Filter] §fLoaded! Press " + keyName + " or /afkfilter to toggle")
                    );
                }
            });
        });

        LOGGER.info("AFK Filter Mod initialized. Press F6 to toggle. Pattern: '{}'", config.afkPattern);
    }

    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(ClientCommands.literal("afkfilter")
                .executes(context -> {
                    toggleFilter(Minecraft.getInstance());
                    return 1;
                })
        );
    }

    private void toggleFilter(Minecraft client) {
        filterEnabled = !filterEnabled;
        if (client.player != null) {
            String status = filterEnabled ? "§aON" : "§cOFF";
            client.player.sendOverlayMessage(
                    Component.literal("§7[AFK Filter] " + status)
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
