package com.miniclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SettingsScreen extends Screen {
    private static final int[] FPS_STEPS = {30, 60, 90, 120, 144, 165, 240, 260};
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Text.literal("MiniClient Ayarları"));
        this.parent = parent;
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "AÇIK" : "KAPALI"));
    }

    private static Text fpsLabel() {
        return Text.literal("FPS Limiti: " + (Config.fpsLimit >= 260 ? "Sınırsız" : String.valueOf(Config.fpsLimit)));
    }

    private void toggle(int x, int y, String name, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(label(name, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(label(name, get.getAsBoolean()));
            Config.save();
        }).dimensions(x, y, 150, 20).build());
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 152;
        int right = this.width / 2 + 2;
        int y = 36;
        int step = 24;

        toggle(left, y, "Fullbright", () -> Config.fullbright, v -> Config.fullbright = v);
        toggle(right, y, "FPS Göster", () -> Config.showFps, v -> Config.showFps = v);
        y += step;
        toggle(left, y, "Koordinat", () -> Config.showCoords, v -> Config.showCoords = v);
        toggle(right, y, "Ping", () -> Config.showPing, v -> Config.showPing = v);
        y += step;
        toggle(left, y, "CPS", () -> Config.showCps, v -> Config.showCps = v);
        toggle(right, y, "Tuş Göstergesi", () -> Config.showKeys, v -> Config.showKeys = v);
        y += step;
        toggle(left, y, "FPS Kilidi", () -> Config.fpsLock, v -> {
            Config.fpsLock = v;
            if (v) Optimizer.applyFps();
        });
        addDrawableChild(ButtonWidget.builder(fpsLabel(), b -> {
            int idx = 0;
            for (int i = 0; i < FPS_STEPS.length; i++) {
                if (FPS_STEPS[i] == Config.fpsLimit) idx = i;
            }
            Config.fpsLimit = FPS_STEPS[(idx + 1) % FPS_STEPS.length];
            b.setMessage(fpsLabel());
            Optimizer.applyFps();
            Config.save();
        }).dimensions(right, y, 150, 20).build());
        y += step;
        toggle(left, y, "Optimizasyon", () -> Config.optimize, Optimizer::setOptimize);
        toggle(right, y, "Menü Bulanıklığı Kapalı", () -> Config.noBlur, Optimizer::setNoBlur);
        y += step + 6;

        addDrawableChild(ButtonWidget.builder(Text.literal("Bitti"), b -> this.close())
                .dimensions(this.width / 2 - 100, y, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
