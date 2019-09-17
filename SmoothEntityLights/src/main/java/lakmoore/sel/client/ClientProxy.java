package lakmoore.sel.client;

import java.util.HashMap;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
    static Minecraft mcinstance;

    /**
     * The Keybinding instance to monitor
     */
    static KeyBinding toggleButton;
    static long nextKeyTriggerTime;

    public void preInit(FMLPreInitializationEvent evt) {
        Config.doConfig(evt.getSuggestedConfigurationFile());

        SEL.disabled = false;
        ClientProxy.mcinstance = FMLClientHandler.instance().getClient();
        SEL.lightValueMap = new HashMap<Class<? extends Entity>, Boolean>();
        SEL.glowValueMap = new HashMap<Class<? extends Entity>, Integer>();

        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
        FMLCommonHandler.instance().bus().register(new FMLEventHandler());

        ClientProxy.nextKeyTriggerTime = System.currentTimeMillis();
        SEL.lastLightUpdateTime = System.currentTimeMillis();

    }

    public void init() {
        ClientProxy.toggleButton = new KeyBinding("Toggle Smooth Entity Lights", Keyboard.KEY_L, "key.categories.gameplay");
        ClientRegistry.registerKeyBinding(ClientProxy.toggleButton);
        SEL.coloredLights = Loader.isModLoaded("easycoloredlights");
    }

}
