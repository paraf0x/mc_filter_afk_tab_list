package dev.afkfilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AfkFilterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "afk-filter.json";

    public String afkPattern = "AFK";
    public boolean caseSensitive = false;
    public boolean enabledByDefault = true;

    public static AfkFilterConfig load() {
        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(CONFIG_FILE);

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                AfkFilterConfig config = GSON.fromJson(json, AfkFilterConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                AfkFilterMod.LOGGER.error("Failed to load config", e);
            }
        }

        AfkFilterConfig config = new AfkFilterConfig();
        config.save();
        return config;
    }

    public void save() {
        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(CONFIG_FILE);

        try {
            Files.writeString(configPath, GSON.toJson(this));
        } catch (IOException e) {
            AfkFilterMod.LOGGER.error("Failed to save config", e);
        }
    }

    public boolean matchesAfkPattern(String name) {
        if (name == null || afkPattern == null) return false;

        if (caseSensitive) {
            return name.contains(afkPattern);
        } else {
            return name.toLowerCase().contains(afkPattern.toLowerCase());
        }
    }
}
