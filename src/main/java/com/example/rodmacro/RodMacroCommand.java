package com.example.rodmacro;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class RodMacroCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "rodmacro";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/rodmacro <delay <0-200> | swap <1-9>>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            usage(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("delay")) {
            if (args.length == 1) {
                msg(sender, EnumChatFormatting.YELLOW + "Delay is " + RodMacro.delayMs + "ms. Set it with /rodmacro delay <0-200>");
                return;
            }
            Integer ms = parse(sender, args[1], 0, 200);
            if (ms == null) return;
            RodMacro.setDelay(ms);
            msg(sender, EnumChatFormatting.GREEN + "Rod macro delay set to " + ms + "ms");
        } else if (sub.equals("swap")) {
            if (args.length == 1) {
                msg(sender, EnumChatFormatting.YELLOW + "Swap slot is " + RodMacro.swapSlot + ". Set it with /rodmacro swap <1-9>");
                return;
            }
            Integer s = parse(sender, args[1], 1, 9);
            if (s == null) return;
            RodMacro.setSwapSlot(s);
            msg(sender, EnumChatFormatting.GREEN + "Rod macro swap slot set to " + s);
        } else {
            usage(sender);
        }
    }

    private Integer parse(ICommandSender sender, String s, int min, int max) {
        int v;
        try {
            v = Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            msg(sender, EnumChatFormatting.RED + "That's not a number. Use " + min + "-" + max + ".");
            return null;
        }
        if (v < min || v > max) {
            msg(sender, EnumChatFormatting.RED + "Value must be between " + min + " and " + max + ".");
            return null;
        }
        return v;
    }

    private void usage(ICommandSender sender) {
        msg(sender, EnumChatFormatting.YELLOW + "Usage: /rodmacro delay <0-200> (now " + RodMacro.delayMs
                + "ms), swap <1-9> (now " + RodMacro.swapSlot + ")");
    }

    private void msg(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }
}
