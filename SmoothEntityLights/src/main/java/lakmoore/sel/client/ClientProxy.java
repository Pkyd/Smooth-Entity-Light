package lakmoore.sel.client;

import org.lwjgl.glfw.GLFW;

import lakmoore.sel.capabilities.DefaultLightSourceCapability;
import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.capabilities.LitChunkCacheCapability;
import lakmoore.sel.capabilities.NoStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.profiler.IProfiler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ClientProxy extends CommonProxy {
    public static Minecraft mcinstance;
    public static IProfiler mcProfiler;

    /**
     * The Keybinding instance to monitor
     */
    static KeyBinding toggleButton;
    static long nextKeyTriggerTime;
    
    @Override
    public void setupCommon(FMLCommonSetupEvent evt) {    
    	// First event - not in the main thread
    	// Registry is now valid
    	// Things to do:  
    	//      Creating and reading the config files
    	//      Registering Capabilities

    	super.setupCommon(evt);
    	
        CapabilityManager.INSTANCE.register(ILightSourceCapability.class, new NoStorage<ILightSourceCapability>(), DefaultLightSourceCapability::new);
        CapabilityManager.INSTANCE.register(ILitChunkCache.class, new NoStorage<ILitChunkCache>(), LitChunkCacheCapability::new);                

        // Only put Client Side Events in EventHandler
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());

    }
    
    @Override
    public void setupClient(FMLClientSetupEvent evt) {
    	// Second event (sided) - not in the main thread
    	// Client only - Do Keybindings???
    	
    	super.setupClient(evt);

        ClientProxy.mcinstance = evt.getMinecraftSupplier().get();

        SEL.disabled = false;

        ClientProxy.nextKeyTriggerTime = System.currentTimeMillis();
                
        mcProfiler = ClientProxy.mcinstance.getProfiler();
        
        // ==================

        // 76 = L key
        ClientProxy.toggleButton = new KeyBinding("Toggle Smooth Entity Lights", GLFW.GLFW_KEY_L, "key.categories.gameplay");                
        ClientRegistry.registerKeyBinding(ClientProxy.toggleButton);

        // I don't think EasyColoredLights still exists, but no harm in leaving this here
        SEL.coloredLights = ModList.get().isLoaded("easycoloredlights");
        
    }

}
