package com.miniclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Properties;

public class MiniClient implements ClientModInitializer {
    static boolean menuKeyDown = false;
    static boolean openRequested = false;
    static boolean weAddedNightVision = false;

    @Override
    public void onInitializeClient() {
        Config.load();
        HudRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(MiniClient::tick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("miniclient").executes(ctx -> {
                    openRequested = true;
                    return 1;
                })));
    }

    private static void tick(MinecraftClient mc) {
        if (mc.getWindow() != null) {
            boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            if (down && !menuKeyDown && mc.currentScreen == null && mc.player != null) {
                openRequested = true;
            }
            menuKeyDown = down;
        }
        if (openRequested && mc.currentScreen == null && mc.player != null) {
            openRequested = false;
            mc.setScreen(new SettingsScreen(null));
        }

        if (Config.fpsLock) {
            Optimizer.applyFps();
        }

        if (mc.player != null) {
            StatusEffectInstance eff = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (Config.fullbright) {
                if (eff == null) {
                    mc.player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false, false));
                    weAddedNightVision = true;
                }
            } else if (weAddedNightVision) {
                if (eff != null && eff.isInfinite()) {
                    mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
                weAddedNightVision = false;
            }
        }
    }
}

class Config {
    static boolean fullbright = false, showFps = true, showCoords = false, showPing = true,
            showCps = true, showKeys = true, fpsLock = false, optimize = false, noBlur = false;
    static int fpsLimit = 144;
    static int prevView = 12, prevSim = 12, prevParticles = 0, prevBiome = 2, prevBlur = 5;
    static boolean prevShadows = true;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("miniclient.properties");
    }

    private static boolean b(Properties p, String k, boolean d) {
        return Boolean.parseBoolean(p.getProperty(k, String.valueOf(d)));
    }

    private static int i(Properties p, String k, int d) {
        try {
            return Integer.parseInt(p.getProperty(k, String.valueOf(d)));
        } catch (NumberFormatException e) {
            return d;
        }
    }

    static void load() {
        Path file = path();
        if (!Files.exists(file)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        fullbright = b(p, "fullbright", fullbright);
        showFps = b(p, "showFps", showFps);
        showCoords = b(p, "showCoords", showCoords);
        showPing = b(p, "showPing", showPing);
        showCps = b(p, "showCps", showCps);
        showKeys = b(p, "showKeys", showKeys);
        fpsLock = b(p, "fpsLock", fpsLock);
        optimize = b(p, "optimize", optimize);
        noBlur = b(p, "noBlur", noBlur);
        fpsLimit = i(p, "fpsLimit", fpsLimit);
        prevView = i(p, "prevView", prevView);
        prevSim = i(p, "prevSim", prevSim);
        prevParticles = i(p, "prevParticles", prevParticles);
        prevBiome = i(p, "prevBiome", prevBiome);
        prevBlur = i(p, "prevBlur", prevBlur);
        prevShadows = b(p, "prevShadows", prevShadows);
    }

    static void save() {
        Properties p = new Properties();
        p.setProperty("fullbright", String.valueOf(fullbright));
        p.setProperty("showFps", String.valueOf(showFps));
        p.setProperty("showCoords", String.valueOf(showCoords));
        p.setProperty("showPing", String.valueOf(showPing));
        p.setProperty("showCps", String.valueOf(showCps));
        p.setProperty("showKeys", String.valueOf(showKeys));
        p.setProperty("fpsLock", String.valueOf(fpsLock));
        p.setProperty("optimize", String.valueOf(optimize));
        p.setProperty("noBlur", String.valueOf(noBlur));
        p.setProperty("fpsLimit", String.valueOf(fpsLimit));
        p.setProperty("prevView", String.valueOf(prevView));
        p.setProperty("prevSim", String.valueOf(prevSim));
        p.setProperty("prevParticles", String.valueOf(prevParticles));
        p.setProperty("prevBiome", String.valueOf(prevBiome));
        p.setProperty("prevBlur", String.valueOf(prevBlur));
        p.setProperty("prevShadows", String.valueOf(prevShadows));
        try (OutputStream out = Files.newOutputStream(path())) {
            p.store(out, "MiniClient ayarlari");
        } catch (IOException ignored) {
        }
    }
}

class Optimizer {
    static void applyFps() {
        GameOptions o = MinecraftClient.getInstance().options;
        int cur = o.getMaxFps().getValue();
        if (cur != Config.fpsLimit) {
            o.getMaxFps().setValue(Config.fpsLimit);
        }
    }

    static void setOptimize(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        if (on && !Config.optimize) {
            Config.prevView = o.getViewDistance().getValue();
            Config.prevSim = o.getSimulationDistance().getValue();
            Config.prevParticles = o.getParticles().getValue().ordinal();
            Config.prevBiome = o.getBiomeBlendRadius().getValue();
            Config.prevShadows = o.getEntityShadows().getValue();
            o.getViewDistance().setValue(8);
            o.getSimulationDistance().setValue(6);
            var pAll = o.getParticles().getValue().getDeclaringClass().getEnumConstants();
            o.getParticles().setValue(pAll[pAll.length - 1]);
            o.getBiomeBlendRadius().setValue(0);
            o.getEntityShadows().setValue(false);
        } else if (!on && Config.optimize) {
            o.getViewDistance().setValue(Config.prevView);
            o.getSimulationDistance().setValue(Config.prevSim);
            var pAll = o.getParticles().getValue().getDeclaringClass().getEnumConstants();
            o.getParticles().setValue(pAll[Math.min(Config.prevParticles, pAll.length - 1)]);
            o.getBiomeBlendRadius().setValue(Config.prevBiome);
            o.getEntityShadows().setValue(Config.prevShadows);
        }
        Config.optimize = on;
        o.write();
        Config.save();
    }

    static void setNoBlur(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        if (on && !Config.noBlur) {
            Config.prevBlur = o.getMenuBackgroundBlurriness().getValue();
            o.getMenuBackgroundBlurriness().setValue(0);
        } else if (!on && Config.noBlur) {
            o.getMenuBackgroundBlurriness().setValue(Config.prevBlur);
        }
        Config.noBlur = on;
        o.write();
        Config.save();
    }
}

class HudRenderer {
    private static final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private static final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
    private static boolean lastLeft = false, lastRight = false;

    static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("miniclient", "hud"),
                HudRenderer::render);
    }

    private static void trim(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
    }

    private static void text(DrawContext ctx, TextRenderer tr, String s, int x, int y) {
        ctx.drawText(tr, s, x, y, 0xFFFFFFFF, true);
    }

    private static void box(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, boolean pressed) {
        ctx.fill(x, y, x + w, y + h, pressed ? 0xB0FFFFFF : 0x80000000);
        int c = pressed ? 0xFF000000 : 0xFFFFFFFF;
        ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2, c, false);
    }

    static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        long now = System.currentTimeMillis();

        boolean l = mc.options.attackKey.isPressed();
        boolean r = mc.options.useKey.isPressed();
        if (l && !lastLeft) leftClicks.addLast(now);
        if (r && !lastRight) rightClicks.addLast(now);
        lastLeft = l;
        lastRight = r;
        trim(leftClicks, now);
        trim(rightClicks, now);

        int x = 4, y = 4;
        if (Config.showFps) {
            text(ctx, tr, "FPS: " + mc.getCurrentFps(), x, y);
            y += 11;
        }
        if (Config.showPing) {
            int ping = 0;
            if (mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            text(ctx, tr, "Ping: " + ping + " ms", x, y);
            y += 11;
        }
        if (Config.showCps) {
            text(ctx, tr, "CPS: " + leftClicks.size() + " | " + rightClicks.size(), x, y);
            y += 11;
        }
        if (Config.showCoords) {
            text(ctx, tr, "XYZ: " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ(), x, y);
            y += 11;
        }
        if (Config.showKeys) {
            y += 4;
            box(ctx, tr, x + 24, y, 22, 22, "W", mc.options.forwardKey.isPressed());
            box(ctx, tr, x, y + 24, 22, 22, "A", mc.options.leftKey.isPressed());
            box(ctx, tr, x + 24, y + 24, 22, 22, "S", mc.options.backKey.isPressed());
            box(ctx, tr, x + 48, y + 24, 22, 22, "D", mc.options.rightKey.isPressed());
            box(ctx, tr, x, y + 48, 35, 22, "LMB", l);
            box(ctx, tr, x + 35 + 2, y + 48, 33, 22, "RMB", r);
            box(ctx, tr, x, y + 72, 70, 14, "SPACE", mc.options.jumpKey.isPressed());
        }
    }
          }
