package dev.afkfilter.mixin;

import dev.afkfilter.AfkFilterConfig;
import dev.afkfilter.AfkFilterMod;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.ChatFormatting;
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
        if (config.sortMode()) {
            cir.setReturnValue(AfkFilterMod.afkLast(original, entry -> isAfkPlayer(entry, config)));
            return;
        }
        List<PlayerInfo> filtered = original.stream()
                .filter(entry -> !isAfkPlayer(entry, config))
                .collect(Collectors.toList());

        cir.setReturnValue(filtered);
    }

    // Sort mode: AFK names in plain grey, dropping server colours so they read as inactive.
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void greyAfkNames(PlayerInfo entry, CallbackInfoReturnable<Component> cir) {
        AfkFilterConfig config = AfkFilterMod.getConfig();
        if (!AfkFilterMod.isFilterEnabled() || config == null || !config.sortMode() || !isAfkPlayer(entry, config)) {
            return;
        }
        cir.setReturnValue(Component.literal(ChatFormatting.stripFormatting(cir.getReturnValue().getString())).withStyle(ChatFormatting.GRAY));
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
