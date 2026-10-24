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
    public static int delayMs = 50; // 0-200, how long the hotbar stays on the swap slot

    private static Configuration cfg;

    private enum Stage { IDLE, SWAP_AWAY, SWAP_BACK }
    private Stage stage = Stage.IDLE;
    private long runAt;
    private int rodSlotIndex;

    public static void loadConfig(File file) {
        cfg = new Configuration(file);
        enabled  = cfg.getBoolean("enabled", "rodmacro", true, "Enable rod macro");
        swapSlot = cfg.getInt("swapSlot", "rodmacro", 4, 1, 9, "Slot to scroll to");
        delayMs  = cfg.getInt("delayMs", "rodmacro", 50, 0, 200, "How long the hotbar stays on the swap slot, in ms");
        if (cfg.hasChanged()) cfg.save();
    }

    public static void setDelay(int ms) {
        delayMs = ms;
        if (cfg != null) {
            cfg.get("rodmacro", "delayMs", 50).set(ms);
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

    private void scrollTo(int slot) {
        mc.thePlayer.inventory.currentItem = slot;
        if (mc.getNetHandler() != null) {
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(slot));
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!enabled || e.button != 1 || !e.buttonstate) return;
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        if (stage != Stage.IDLE) { e.setCanceled(true); return; } // ignore clicks mid-cycle
        if (!holdingRod()) return;

        int current = mc.thePlayer.inventory.currentItem;
        if (current == swapSlot - 1) return; // rod is in the swap slot, nothing to scroll to

        rodSlotIndex = current;

        // bobber out: let the click go through, it reels in.
        // nothing out: cancel the click, the cast happens at the end of the cycle.
        if (mc.thePlayer.fishEntity == null) {
            e.setCanceled(true);
        }

        stage = Stage.SWAP_AWAY;
        runAt = 0; // scroll away on the next frame, after the click has been handled
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || stage == Stage.IDLE) return;
        if (mc.thePlayer == null || mc.theWorld == null) { stage = Stage.IDLE; return; }
        if (System.currentTimeMillis() < runAt) return;

        if (stage == Stage.SWAP_AWAY) {
            scrollTo(swapSlot - 1);
            stage = Stage.SWAP_BACK;
            runAt = System.currentTimeMillis() + delayMs;
        } else {
            scrollTo(rodSlotIndex);
            if (holdingRod() && mc.thePlayer.fishEntity == null) {
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                mc.thePlayer.swingItem();
            }
            stage = Stage.IDLE;
        }
    }
}
