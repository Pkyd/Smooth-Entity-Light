package lakmoore.sel.client;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.lwjgl.input.Keyboard;

import lakmoore.sel.capabilities.DefaultLightSourceCapability;
import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.capabilities.LitChunkCacheCapability;
import lakmoore.sel.capabilities.NoStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
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
    public static Field viewFrustumField;
    public static Field renderDispatcher;
    public static Field renderWorker;
    public static Field regionRenderCacheBuilder;

    /**
     * The Keybinding instance to monitor
     */
    static KeyBinding toggleButton;
    static long nextKeyTriggerTime;

    public void preInit(FMLPreInitializationEvent evt) {
        ClientProxy.mcinstance = FMLClientHandler.instance().getClient();

        Config.doConfig(evt.getSuggestedConfigurationFile());

        SEL.disabled = false;
        SEL.lightValueMap = new HashMap<Class<? extends Entity>, Boolean>();
        SEL.glowValueMap = new HashMap<Class<? extends Entity>, Integer>();

        ClientProxy.nextKeyTriggerTime = System.currentTimeMillis();
        
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        CapabilityManager.INSTANCE.register(ILightSourceCapability.class, new NoStorage<ILightSourceCapability>(), DefaultLightSourceCapability::new);
        CapabilityManager.INSTANCE.register(ILitChunkCache.class, new NoStorage<ILitChunkCache>(), LitChunkCacheCapability::new);                
    }

    public void init() {
        mcProfiler = Minecraft.getMinecraft().profiler;

        ClientProxy.toggleButton = new KeyBinding("Toggle Smooth Entity Lights", Keyboard.KEY_L, "key.categories.gameplay");                
        ClientRegistry.registerKeyBinding(ClientProxy.toggleButton);
        SEL.coloredLights = Loader.isModLoaded("easycoloredlights");
        
        try {
        	try {
    			viewFrustumField = RenderGlobal.class.getDeclaredField("field_175008_n");        		
        	} catch (Exception e) {        		
    			viewFrustumField = RenderGlobal.class.getDeclaredField("viewFrustum");        		
        	}
			viewFrustumField.setAccessible(true);

        	try {
    			renderDispatcher = RenderGlobal.class.getDeclaredField("field_174995_M");
        	} catch (Exception e) {        		
    			renderDispatcher = RenderGlobal.class.getDeclaredField("renderDispatcher");
        	}
			renderDispatcher.setAccessible(true);

			try {
    			renderWorker = ChunkRenderDispatcher.class.getDeclaredField("field_178525_i");
        	} catch (Exception e) {        		
    			renderWorker = ChunkRenderDispatcher.class.getDeclaredField("renderWorker");
        	}
			renderWorker.setAccessible(true);

        	try {
        		
        		// field_178478_c
        		// field_178550_e
        		
    			regionRenderCacheBuilder = ChunkRenderWorker.class.getDeclaredField("field_178478_c");
        	} catch (Exception e) {        		
    			regionRenderCacheBuilder = ChunkRenderWorker.class.getDeclaredField("regionRenderCacheBuilder");
        	}
			regionRenderCacheBuilder.setAccessible(true);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

        // Set up our main worker thread
		SEL.lightWorker = new LightWorker();	
		SEL.lightWorker.start();
                
    }

}
