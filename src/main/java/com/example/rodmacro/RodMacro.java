package com.example.rodmacro;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemFishingRod;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;

public class RodMacro {
    private final Minecraft mc = Minecraft.getMinecraft();

    // config (slots are 1-9 as shown on your hotbar)
    public static boolean enabled = true;
    public static int rodSlot = 2;
    public static int swapSlot = 4;
    public static int delayMs = 50; // 0-100

    private enum Stage { IDLE, WAIT_SWAP, WAIT_BACK }
    private Stage stage = Stage.IDLE;
    private long nextTime;

    public static void loadConfig(File file) {
        Configuration cfg = new Configuration(file);
        enabled  = cfg.getBoolean("enabled", "rodmacro", true, "Enable rod macro");
        rodSlot  = cfg.getInt("rodSlot", "rodmacro", 2, 1, 9, "Hotbar slot of your rod");
        swapSlot = cfg.getInt("swapSlot", "rodmacro", 4, 1, 9, "Slot to switch to after casting");
        delayMs  = cfg.getInt("delayMs", "rodmacro", 50, 0, 100, "Delay between steps in ms");
        if (cfg.hasChanged()) cfg.save();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!enabled || e.button != 1 || !e.buttonstate) return;
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        if (stage != Stage.IDLE) return;

        if (mc.thePlayer.inventory.currentItem != rodSlot - 1) return;
        if (!(mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemFishingRod)) return;

        stage = Stage.WAIT_SWAP;
        nextTime = System.currentTimeMillis() + delayMs;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || stage == Stage.IDLE) return;
        if (mc.thePlayer == null) { stage = Stage.IDLE; return; }
        if (System.currentTimeMillis() < nextTime) return;

        if (stage == Stage.WAIT_SWAP) {
            mc.thePlayer.inventory.currentItem = swapSlot - 1;
            stage = Stage.WAIT_BACK;
            nextTime = System.currentTimeMillis() + delayMs;
        } else {
            mc.thePlayer.inventory.currentItem = rodSlot - 1;
            stage = Stage.IDLE;
        }
    }
}
