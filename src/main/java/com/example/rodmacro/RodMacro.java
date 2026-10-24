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
    public static int swapSlot = 4;
    public static int delayMs = 0; // 0-200, wait between reeling in and recasting

    private static Configuration cfg;

    private boolean pending = false;
    private long runAt;
    private int returnSlot;

    public static void loadConfig(File file) {
        cfg = new Configuration(file);
        enabled  = cfg.getBoolean("enabled", "rodmacro", true, "Enable rod macro");
        swapSlot = cfg.getInt("swapSlot", "rodmacro", 4, 1, 9, "Slot to switch to");
        delayMs  = cfg.getInt("delayMs", "rodmacro", 0, 0, 200, "Wait between reeling in and recasting, in ms");
        if (cfg.hasChanged()) cfg.save();
    }

    public static void setDelay(int ms) {
        delayMs = ms;
        if (cfg != null) {
            cfg.get("rodmacro", "delayMs", 0).set(ms);
            cfg.save();
        }
    }

    public static void setSwapSlot(int slot) {
        swapSlot = slot;
        if (cfg != null) {
            cfg.get("rodmacro", "swapSlot", 4).set(slot);
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
        if (pending) return;
        if (!holdingRod()) return;

        int current = mc.thePlayer.inventory.currentItem;
        if (current == swapSlot - 1) return; // rod is in the swap slot, nothing to swap to

        // swap away and back BEFORE the click, so the cast/reel-in is always the last thing the server sees
        sendSlot(swapSlot - 1);
        sendSlot(current);

        // the click itself goes through normally right after this.
        // if a bobber is out, this click reels it in, so recast afterwards
        if (mc.thePlayer.fishEntity != null) {
            returnSlot = current;
            pending = true;
            runAt = System.currentTimeMillis() + delayMs;
        }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !pending) return;
        if (mc.thePlayer == null || mc.theWorld == null) { pending = false; return; }
        if (System.currentTimeMillis() < runAt) return;

        pending = false;
        if (mc.thePlayer.inventory.currentItem != returnSlot || !holdingRod()) return;

        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
        mc.thePlayer.swingItem();
    }
}
