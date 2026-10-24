package com.example.rodmacro;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
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
    public static int delayMs = 0;       // 0-200, extra hold time on top of the ticks
    public static int minSwapTicks = 1;  // 1-5, hold = ticks * 50ms + delay

    private static Configuration cfg;

    private boolean active = false;
    private long holdUntil;

    public static void loadConfig(File file) {
        cfg = new Configuration(file);
        enabled      = cfg.getBoolean("enabled", "rodmacro", true, "Enable rod macro");
        rodSlot      = cfg.getInt("rodSlot", "rodmacro", 2, 1, 9, "Hotbar slot of your rod");
        swapSlot     = cfg.getInt("swapSlot", "rodmacro", 4, 1, 9, "Slot to switch to");
        delayMs      = cfg.getInt("delayMs", "rodmacro", 0, 0, 200, "Extra hold time on the other slot in ms");
        minSwapTicks = cfg.getInt("minSwapTicks", "rodmacro", 1, 1, 5, "Hold = ticks * 50ms + delay. Raise if it reels in instead of casting");
        if (cfg.hasChanged()) cfg.save();
    }

    public static void setDelay(int ms) {
        delayMs = ms;
        if (cfg != null) {
            cfg.get("rodmacro", "delayMs", 0).set(ms);
            cfg.save();
        }
    }

    public static void setMinSwapTicks(int ticks) {
        minSwapTicks = ticks;
        if (cfg != null) {
            cfg.get("rodmacro", "minSwapTicks", 1).set(ticks);
            cfg.save();
        }
    }

    private boolean holdingRod() {
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && held.getItem() instanceof ItemFishingRod;
    }

    private void sendSlot(int slot) {
        if (mc.getNetHandler() != null) {
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(slot));
        }
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
        sendSlot(swapSlot - 1); // send right now instead of waiting for the next game tick
        active = true;
        holdUntil = System.currentTimeMillis() + minSwapTicks * 50L + delayMs;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !active) return;
        if (mc.thePlayer == null || mc.theWorld == null) { active = false; return; }
        if (System.currentTimeMillis() < holdUntil) return;

        // back to the rod and cast once, both packets go out together
        mc.thePlayer.inventory.currentItem = rodSlot - 1;
        sendSlot(rodSlot - 1);
        if (holdingRod()) {
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
            mc.thePlayer.swingItem();
        }
        active = false;
    }
}
