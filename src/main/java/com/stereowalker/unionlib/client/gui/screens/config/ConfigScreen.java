package com.stereowalker.unionlib.client.gui.screens.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen previousScreen;

    public ConfigScreen(Screen previousScreen, Object ignoredConfig) {
        super(Component.translatable("gui.navigate.config"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> minecraft.setScreen(previousScreen))
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.navigate.configUnavailable"), width / 2, 50, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
