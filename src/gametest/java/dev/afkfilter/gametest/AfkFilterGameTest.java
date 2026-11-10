package dev.afkfilter.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.afkfilter.AfkFilterMod;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Gives the local player an AFK tab-list name and checks the tab list hides it, and that
 * /afkfilter and F6 toggle the filter. Prints AFK-GAMETEST ok/FAIL per check.
 */
public class AfkFilterGameTest implements FabricClientGameTest {

    private final List<String> failures = new ArrayList<>();
    private ClientGameTestContext ctx;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        this.ctx = ctx;
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getConnection().waitForChunksDownload();
            ctx.waitTicks(5);

            check("join message shown", ctx.computeOnClient(AfkFilterGameTest::chat).stream()
                    .anyMatch(s -> s.contains("[AFK Filter]") && s.contains("Loaded!")), true);
            check("filter on by default", ctx.computeOnClient(mc -> AfkFilterMod.isFilterEnabled()), true);
            check("not AFK: listed", tabNames(), List.of("Tester"));

            setNick("[afk] Tester");
            check("AFK nickname (any case): hidden", tabNames(), List.of());

            ctx.setScreen(() -> new ChatScreen("", false));
            ctx.waitForScreen(ChatScreen.class);
            ctx.getInput().typeChars("/afkfilter");
            ctx.getInput().pressKey(InputConstants.KEY_RETURN);
            ctx.waitTicks(2);
            check("/afkfilter turns it off", ctx.computeOnClient(mc -> AfkFilterMod.isFilterEnabled()), false);
            check("filter off: AFK player listed", tabNames(), List.of("[afk] Tester"));

            ctx.getInput().pressKey(InputConstants.KEY_F6);
            ctx.waitTicks(2);
            check("F6 turns it back on", ctx.computeOnClient(mc -> AfkFilterMod.isFilterEnabled()), true);
            check("filter on again: hidden", tabNames(), List.of());

            setNick("Tester");
            check("AFK gone from name: listed again", tabNames(), List.of("Tester"));
        }
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.size() + " check(s) failed:\n  " + String.join("\n  ", failures));
        }
    }

    private void setNick(String nick) {
        ctx.runOnClient(mc -> mc.getConnection().getPlayerInfo(mc.player.getUUID())
                .setTabListDisplayName(Component.literal(nick)));
    }

    @SuppressWarnings("unchecked")
    private List<String> tabNames() {
        return ctx.computeOnClient(mc -> tabNames(mc));
    }

    @SuppressWarnings("unchecked")
    private static List<String> tabNames(Minecraft mc) {
        try {
            PlayerTabOverlay tab = mc.gui.hud.getTabList();
            Method m = PlayerTabOverlay.class.getDeclaredMethod("getPlayerInfos");
            m.setAccessible(true);
            List<String> out = new ArrayList<>();
            for (PlayerInfo info : (List<PlayerInfo>) m.invoke(tab)) {
                out.add(tab.getNameForDisplay(info).getString());
            }
            return out;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> chat(Minecraft mc) {
        try {
            Field f = ChatComponent.class.getDeclaredField("allMessages");
            f.setAccessible(true);
            return ((List<GuiMessage>) f.get(mc.gui.hud.getChat())).stream().map(g -> g.content().getString()).toList();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void check(String name, Object actual, Object expected) {
        if (actual.equals(expected)) {
            System.out.println("AFK-GAMETEST ok " + name);
        } else {
            System.out.println("AFK-GAMETEST FAIL " + name + ": got " + actual + ", expected " + expected);
            failures.add(name + ": got " + actual + ", expected " + expected);
        }
    }
}
