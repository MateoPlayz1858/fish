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
    public static int delayMs = 0; // 0-200, wait after your click before the swap and recast

    private static Configuration cfg;

    private boolean pending = false;
    private boolean reelIn = false;
    private long runAt;
    private int returnSlot;

    public static void loadConfig(File file) {
        cfg = new Configuration(file);
        enabled  = cfg.getBoolean("enabled", "rodmacro", true, "Enable rod macro");
        swapSlot = cfg.getInt("swapSlot", "rodmacro", 4, 1, 9, "Slot to switch to");
        delayMs  = cfg.getInt("delayMs", "rodmacro", 0, 0, 200, "Wait after your click before the swap and recast, in ms");
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

        // the click itself is NOT cancelled, it casts or reels in like normal
        reelIn = mc.thePlayer.fishEntity != null; // a bobber is out, so this click reels it in
        returnSlot = current;
        pending = true;
        runAt = System.currentTimeMillis() + delayMs;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !pending) return;
        if (mc.thePlayer == null || mc.theWorld == null) { pending = false; return; }
        if (System.currentTimeMillis() < runAt) return;

        pending = false;
        if (mc.thePlayer.inventory.currentItem != returnSlot) return; // you changed slot yourself, stop

        // both slot packets go out together, so the server never sees you without the rod
        sendSlot(swapSlot - 1);
        sendSlot(returnSlot);

        // if your click reeled the bobber in, cast again right away
        if (reelIn && holdingRod()) {
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
            mc.thePlayer.swingItem();
        }
    }
}
