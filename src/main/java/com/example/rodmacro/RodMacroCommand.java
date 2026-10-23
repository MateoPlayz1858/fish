package com.example.rodmacro;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = RodMacroMod.MODID, name = "RodMacro", version = "1.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class RodMacroMod {
    public static final String MODID = "rodmacro";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        RodMacro.loadConfig(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new RodMacro());
        ClientCommandHandler.instance.registerCommand(new RodMacroCommand());
    }
}
