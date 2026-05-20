package dev.afkfilter.mixin;

import dev.afkfilter.AfkFilterConfig;
import dev.afkfilter.AfkFilterMod;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(PlayerTabOverlay.class)
public class PlayerListHudMixin {

    @Inject(method = "getPlayerInfos", at = @At("RETURN"), cancellable = true)
    private void filterAfkPlayers(CallbackInfoReturnable<List<PlayerInfo>> cir) {
        if (!AfkFilterMod.isFilterEnabled()) {
            return;
        }

        AfkFilterConfig config = AfkFilterMod.getConfig();
        if (config == null) {
            return;
        }

        List<PlayerInfo> original = cir.getReturnValue();
        List<PlayerInfo> filtered = original.stream()
                .filter(entry -> !isAfkPlayer(entry, config))
                .collect(Collectors.toList());

        cir.setReturnValue(filtered);
    }

    private boolean isAfkPlayer(PlayerInfo entry, AfkFilterConfig config) {
        // Check tab list display name first (often contains prefix/suffix from plugins)
        Component displayName = entry.getTabListDisplayName();
        if (displayName != null) {
            String displayStr = displayName.getString();
            if (config.matchesAfkPattern(displayStr)) {
                return true;
            }
        }

        // Also check profile name (GameProfile is a record in modern authlib)
        String profileName = entry.getProfile().name();
        return config.matchesAfkPattern(profileName);
    }
}
