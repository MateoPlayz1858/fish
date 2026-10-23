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
        return "/rodmacro <delay <0-200> | ticks <1-5>>";
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
        } else if (sub.equals("ticks")) {
            if (args.length == 1) {
                msg(sender, EnumChatFormatting.YELLOW + "Min swap ticks is " + RodMacro.minSwapTicks + ". Set it with /rodmacro ticks <1-5>");
                return;
            }
            Integer t = parse(sender, args[1], 1, 5);
            if (t == null) return;
            RodMacro.setMinSwapTicks(t);
            msg(sender, EnumChatFormatting.GREEN + "Rod macro min swap ticks set to " + t);
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
                + "ms), /rodmacro ticks <1-5> (now " + RodMacro.minSwapTicks + ")");
    }

    private void msg(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }
}
