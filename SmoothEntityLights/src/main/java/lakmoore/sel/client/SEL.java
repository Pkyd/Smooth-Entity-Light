package lakmoore.sel.client;

import java.util.HashMap;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.capabilities.ILitChunkCache;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.InterModComms.IMCMessage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

/**
 * 
 * @author LakMoore
 * 
 *     Forked the concept of Atomic Styker's Dynamic Lights and re-written for better performance
 *     and smooth lighting.
 *
 */
@Mod(SEL.modId)
@Mod.EventBusSubscriber(bus = Bus.MOD)
public class SEL {
    public final static String modId = "sel";

    // main "worker" thread
	public static LightWorker lightWorker;

    // Global off-switch
    public static boolean disabled;
	public static boolean forceUpdate = false;

    public static final int maxLightDist = 12;
    public static final int maxLightDistSq = maxLightDist * maxLightDist;

    /**
     * whether or not the colored lights mod is present
     */
    static boolean coloredLights;

    public static int fmlOverride = -1;

    public static HashMap<Class<? extends Entity>, Boolean> lightValueMap;
    public static HashMap<Class<? extends Entity>, Integer> glowValueMap;

    public static CommonProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);

    public static Logger log = LogManager.getLogger(SEL.modId);
    private static final Marker MARKER = MarkerManager.getMarker("SEL");
        
    @CapabilityInject(ILightSourceCapability.class)
    public static Capability<ILightSourceCapability> LIGHT_SOURCE_CAPABILITY = null;
    public static ResourceLocation LIGHT_SOURCE_CAPABILITY_NAME = new ResourceLocation(SEL.modId, "sel_source_cap");

    @CapabilityInject(ILitChunkCache.class)
    public static Capability<ILitChunkCache> LIT_CHUNK_CACHE_CAPABILITY = null;
    public static ResourceLocation LIT_CHUNK_CACHE_CAPABILITY_NAME = new ResourceLocation(SEL.modId, "sel_lit_chunk_cache_cap");
    
    public static ModConfig modConfig;

    public SEL() {
    	log.info(SEL.MARKER, "we are here - constructor");
    }

    @SubscribeEvent
    public static void setupCommon(FMLCommonSetupEvent evt) {
    	// First event - not in the main thread
    	// Registry is now valid
    	// Things to do:  
    	//      Creating and reading the config files
    	//      Registering Capabilities
    	log.info(SEL.MARKER, "we are here - setupCommon");
        proxy.setupCommon(evt);
    }

    @SubscribeEvent
    public static void setupClient(FMLClientSetupEvent evt) {
    	// Second event (sided) - not in the main thread
    	// Client only - Do Keybindings???
    	
        proxy.setupClient(evt);
    }
        
    /* 
     * With version 1.1.3 and later you can also use FMLIntercomms so any other mod can force
	 * the player to shine light (or not). Like so:
	 * 
	 * InterModComms.sendTo("sel", "forceplayerlighton", "");
	 * InterModComms.sendTo("sel", "forceplayerlightoff", "");
	 * 
	 * Note you have to track this yourself. Smooth Entity Light will accept and obey, but not recover should you
	 * get stuck in the on or off state inside your own code. It will not revert to off on its own.
	 */
    @SubscribeEvent
    public static void processIMC(InterModProcessEvent event) {    	
        if (InterModComms.getMessages(SEL.modId).anyMatch(m -> m.getMethod().equalsIgnoreCase("forceplayerlighton"))) {
        	SEL.fmlOverride = 15;
        } else if (InterModComms.getMessages(SEL.modId).anyMatch(m -> m.getMethod().equalsIgnoreCase("forceplayerlightoff"))) {
        	SEL.fmlOverride = 0;
        }        
    }
	
	public static boolean enabledForDimension(DimensionType dimension) {
		if (Config.dimensionBlacklist == null || Config.dimensionBlacklist.size() == 0) {
			return true;
		}
		for (int dim : Config.dimensionBlacklist) {
			if (dim == dimension.getId()) {
				return false;
			}
		}
		return true;		
	}
	
    @SubscribeEvent
	public static void onModConfigEvent(final ModConfig.ModConfigEvent configEvent) {
		if (configEvent.getConfig().getSpec() == Config.CLIENT_SPEC) {
			SEL.modConfig = configEvent.getConfig();
			Config.bakeConfig();
		}
	}
    
}
