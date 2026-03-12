package dev.afkfilter.mixin;

import dev.afkfilter.AfkFilterConfig;
import dev.afkfilter.AfkFilterMod;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "collectPlayerEntries", at = @At("RETURN"), cancellable = true)
    private void filterAfkPlayers(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
        if (!AfkFilterMod.isFilterEnabled()) {
            return;
        }

        AfkFilterConfig config = AfkFilterMod.getConfig();
        if (config == null) {
            return;
        }

        List<PlayerListEntry> original = cir.getReturnValue();
        List<PlayerListEntry> filtered = original.stream()
                .filter(entry -> !isAfkPlayer(entry, config))
                .collect(Collectors.toList());

        cir.setReturnValue(filtered);
    }

    private boolean isAfkPlayer(PlayerListEntry entry, AfkFilterConfig config) {
        // Check DisplayName first (often contains prefix/suffix from plugins)
        Text displayName = entry.getDisplayName();
        if (displayName != null) {
            String displayStr = displayName.getString();
            if (config.matchesAfkPattern(displayStr)) {
                return true;
            }
        }

        // Also check profile name
        String profileName = entry.getProfile().name();
        return config.matchesAfkPattern(profileName);
    }
}
