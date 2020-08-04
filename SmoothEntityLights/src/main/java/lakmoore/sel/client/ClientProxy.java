package lakmoore.sel.client;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.HashMap;

import lakmoore.sel.capabilities.DefaultLightSourceCapability;
import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.capabilities.LitChunkCacheCapability;
import lakmoore.sel.capabilities.NoStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ClientProxy extends CommonProxy {
    public static Minecraft mcinstance;
    public static Profiler mcProfiler;

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

        // ==================
    	
        CapabilityManager.INSTANCE.register(ILightSourceCapability.class, new NoStorage<ILightSourceCapability>(), DefaultLightSourceCapability::new);
        CapabilityManager.INSTANCE.register(ILitChunkCache.class, new NoStorage<ILitChunkCache>(), LitChunkCacheCapability::new);                
    }

    @Override
    public void setupClient(FMLClientSetupEvent evt) {
    	// Second event (sided) - not in the main thread
    	// Client only - Do Keybindings???

        ClientProxy.mcinstance = evt.getMinecraftSupplier().get();

    	ModLoadingContext.get().registerConfig(Type.CLIENT, Config.CLIENT_SPEC);

        SEL.disabled = false;

        ClientProxy.nextKeyTriggerTime = System.currentTimeMillis();
        
        // The entire EventHandler is only registered Client Side
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        
        mcProfiler = ClientProxy.mcinstance.profiler;
        
        // ==================

        // 76 = L key
        ClientProxy.toggleButton = new KeyBinding("Toggle Smooth Entity Lights", KeyEvent.VK_L, "key.categories.gameplay");                
        ClientRegistry.registerKeyBinding(ClientProxy.toggleButton);

        // I don't think EasyColoredLights still exists, but no harm in leaving this here
        SEL.coloredLights = ModList.get().isLoaded("easycoloredlights");
        
        // Set up our main worker thread
		SEL.lightWorker = new LightWorker();	
		SEL.lightWorker.start();
    }

}
