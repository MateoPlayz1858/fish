package com.example.rodmacro;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
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
    public static int delayMs = 50;      // 0-200, extra time spent on the other slot
    public static int minSwapTicks = 2;  // 1-5, game ticks (50ms each) spent on the other slot

    private static final long TIMEOUT_MS = 1500; // safety: never get stuck on the other slot

    private static Configuration cfg;

    private boolean active = false;
    private long startTime;
    private long holdUntil;
    private int ticksWaited;

    public static void loadConfig(File file) {
        cfg = new Configuration(file);
        enabled      = cfg.getBoolean("enabled", "rodmacro", true, "Enable rod macro");
        rodSlot      = cfg.getInt("rodSlot", "rodmacro", 2, 1, 9, "Hotbar slot of your rod");
        swapSlot     = cfg.getInt("swapSlot", "rodmacro", 4, 1, 9, "Slot to switch to");
        delayMs      = cfg.getInt("delayMs", "rodmacro", 50, 0, 200, "Extra time on the other slot in ms");
        minSwapTicks = cfg.getInt("minSwapTicks", "rodmacro", 2, 1, 5, "Min game ticks on the other slot");
        if (cfg.hasChanged()) cfg.save();
    }

    public static void setDelay(int ms) {
        delayMs = ms;
        if (cfg != null) {
            cfg.get("rodmacro", "delayMs", 50).set(ms);
            cfg.save();
        }
    }

    public static void setMinSwapTicks(int ticks) {
        minSwapTicks = ticks;
        if (cfg != null) {
            cfg.get("rodmacro", "minSwapTicks", 2).set(ticks);
            cfg.save();
        }
    }

    private boolean holdingRod() {
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && held.getItem() instanceof ItemFishingRod;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!enabled || e.button != 1 || !e.buttonstate) return;
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        if (active) { e.setCanceled(true); return; } // ignore clicks mid-cycle
        if (mc.thePlayer.inventory.currentItem != rodSlot - 1) return;
        if (!holdingRod()) return;

        // cancel the normal click, the swap itself removes any bobber that's out
        e.setCanceled(true);
        mc.thePlayer.inventory.currentItem = swapSlot - 1;
        active = true;
        ticksWaited = 0;
        startTime = System.currentTimeMillis();
        holdUntil = startTime + delayMs;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && active) ticksWaited++;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !active) return;
        if (mc.thePlayer == null || mc.theWorld == null) { active = false; return; }

        long now = System.currentTimeMillis();
        boolean timedOut = now - startTime > TIMEOUT_MS;

        if (!timedOut) {
            if (ticksWaited < minSwapTicks || now < holdUntil) return;
            // wait until the old bobber is really gone, otherwise the next use would reel it in
            if (mc.thePlayer.fishEntity != null) return;
        }

        // back to the rod and cast once
        mc.thePlayer.inventory.currentItem = rodSlot - 1;
        if (holdingRod() && mc.thePlayer.fishEntity == null) {
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
            mc.thePlayer.swingItem();
        }
        active = false;
    }
}
