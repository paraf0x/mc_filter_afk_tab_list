package dev.afkfilter.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.afkfilter.AfkFilterMod;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.TextColor;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

            check("afkLast keeps order, AFK at the end",
                    AfkFilterMod.afkLast(List.of("a", "AFK b", "c", "afk d", "e"), n -> n.toLowerCase().contains("afk")),
                    List.of("a", "c", "e", "AFK b", "afk d"));
            command("/afkfilter mode sort");
            check("sort mode saved", ctx.computeOnClient(mc -> AfkFilterMod.getConfig().sortMode()), true);
            check("sort mode: AFK player listed", tabNames(), List.of("[afk] Tester"));
            check("sort mode: AFK name grey", ctx.computeOnClient(mc -> mc.gui.hud.getTabList()
                    .getNameForDisplay(mc.getConnection().getPlayerInfo(mc.player.getUUID())).getStyle().getColor()),
                    TextColor.fromLegacyFormat(ChatFormatting.GRAY));

            addFakePlayers();
            check("sort mode: AFK players last", tabNames(), List.of("Alex", "Notch", "Zed", "[AFK] Steve", "Bob AFK", "[afk] Tester"));
            ctx.getInput().holdKey(options -> options.keyPlayerList);
            ctx.waitTicks(3);
            ctx.takeScreenshot("tab-sort");
            command("/afkfilter mode hide");
            ctx.getInput().holdKey(options -> options.keyPlayerList);
            ctx.waitTicks(3);
            ctx.takeScreenshot("tab-hide");
            ctx.getInput().releaseKey(options -> options.keyPlayerList);
            check("hide mode again: hidden", tabNames(), List.of("Alex", "Notch", "Zed"));
            removeFakePlayers();

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

    private static final String[] FAKE = {"Alex", "Bob AFK", "§c[AFK] §fSteve", "Notch", "Zed"};

    @SuppressWarnings("unchecked")
    private void addFakePlayers() {
        ctx.runOnClient(mc -> {
            try {
                Field map = ClientPacketListener.class.getDeclaredField("playerInfoMap");
                Field listed = ClientPacketListener.class.getDeclaredField("listedPlayers");
                map.setAccessible(true);
                listed.setAccessible(true);
                for (String nick : FAKE) {
                    String name = nick.replaceAll("§.", "").replaceAll("[^A-Za-z]", "");
                    GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name);
                    PlayerInfo info = new PlayerInfo(profile, false);
                    if (!nick.equals(name)) {
                        info.setTabListDisplayName(Component.literal(nick));
                    }
                    ((Map<UUID, PlayerInfo>) map.get(mc.getConnection())).put(profile.id(), info);
                    ((Set<PlayerInfo>) listed.get(mc.getConnection())).add(info);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void removeFakePlayers() {
        ctx.runOnClient(mc -> {
            try {
                Field map = ClientPacketListener.class.getDeclaredField("playerInfoMap");
                Field listed = ClientPacketListener.class.getDeclaredField("listedPlayers");
                map.setAccessible(true);
                listed.setAccessible(true);
                ((Map<UUID, PlayerInfo>) map.get(mc.getConnection())).values().removeIf(i -> !i.getProfile().id().equals(mc.player.getUUID()));
                ((Set<PlayerInfo>) listed.get(mc.getConnection())).removeIf(i -> !i.getProfile().id().equals(mc.player.getUUID()));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void command(String command) {
        ctx.setScreen(() -> new ChatScreen("", false));
        ctx.waitForScreen(ChatScreen.class);
        ctx.getInput().typeChars(command);
        ctx.getInput().pressKey(InputConstants.KEY_RETURN);
        ctx.waitTicks(2);
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
