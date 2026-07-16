package com.shahir.pearlcatch.client;

import com.shahir.pearlcatch.AutoCatchController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class PearlCatchClient implements ClientModInitializer {

    private static final AutoCatchController CONTROLLER = new AutoCatchController();

    // Default: G. Change in-game under Options > Controls > Pearl Catch if it conflicts with anything.
    private static final KeyBinding ARM_KEY = new KeyBinding(
            "key.pearlcatch.arm",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.pearlcatch"
    );

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(ARM_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ARM_KEY.wasPressed()) {
                CONTROLLER.arm();
                if (client.player != null) {
                    client.player.sendMessage(
                            net.minecraft.text.Text.literal("§b[PearlCatch] Armed — next pearl throw will be tracked."),
                            true
                    );
                }
            }
            CONTROLLER.onClientTick(client);
        });
    }
}
