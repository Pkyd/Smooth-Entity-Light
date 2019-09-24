package lakmoore.sel.client;

import java.util.HashMap;

import org.lwjgl.input.Keyboard;

import lakmoore.sel.capabilities.DefaultLightSourceCapability;
import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.capabilities.Storage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    public static Minecraft mcinstance;
    public static Profiler mcProfiler;

    /**
     * The Keybinding instance to monitor
     */
    static KeyBinding toggleButton;
    static KeyBinding toggleCullingTypeButton;    
    static long nextKeyTriggerTime;

    public void preInit(FMLPreInitializationEvent evt) {
        ClientProxy.mcinstance = FMLClientHandler.instance().getClient();

        Config.doConfig(evt.getSuggestedConfigurationFile());

        SEL.disabled = false;
        SEL.lightValueMap = new HashMap<Class<? extends Entity>, Boolean>();
        SEL.glowValueMap = new HashMap<Class<? extends Entity>, Integer>();

        MinecraftForge.EVENT_BUS.register(new EventHandler());
        CapabilityManager.INSTANCE.register(ILightSourceCapability.class, new Storage(), DefaultLightSourceCapability::new);

        ClientProxy.nextKeyTriggerTime = System.currentTimeMillis();
        SEL.lastLightUpdateTime = System.currentTimeMillis();

    }

    public void init() {
        mcProfiler = Minecraft.getMinecraft().profiler;

        ClientProxy.toggleButton = new KeyBinding("Toggle Smooth Entity Lights", Keyboard.KEY_L, "key.categories.gameplay");        
        ClientProxy.toggleCullingTypeButton = new KeyBinding("Toggle Culling Method", Keyboard.KEY_C, "key.categories.gameplay");        
        
        ClientRegistry.registerKeyBinding(ClientProxy.toggleButton);
        ClientRegistry.registerKeyBinding(ClientProxy.toggleCullingTypeButton);
        SEL.coloredLights = Loader.isModLoaded("easycoloredlights");
    }

}
