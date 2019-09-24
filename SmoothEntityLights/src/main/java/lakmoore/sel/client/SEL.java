package lakmoore.sel.client;

import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import lakmoore.sel.capabilities.ILightSourceCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * 
 * @author LakMoore
 * 
 *     Forked the concept of Atomic Styker's Dynamic Lights and re-written for better performance
 *     and smooth lighting.
 *
 */
@Mod(
	modid = SEL.modId, 
	name = "Smooth Entity Light", 
	version = "MODVERSION", 
	acceptedMinecraftVersions = "MCVERSION",
	dependencies="required-after:forge@[FORGEVERSION,)"
)
public class SEL {
    public final static String modId = "sel";

    // Global off-switch
    public static boolean disabled;
    static long lastLightUpdateTime;
    static final int maxSearchDist = 8;

    /**
     * whether or not the colored lights mod is present
     */
    static boolean coloredLights;

    public static boolean fmlOverrideEnable;

    public static HashMap<Class<? extends Entity>, Boolean> lightValueMap;
    public static HashMap<Class<? extends Entity>, Integer> glowValueMap;

    // Proxy
    @SidedProxy(
    	clientSide = "lakmoore.sel.client.ClientProxy", 
    	serverSide = "lakmoore.sel.client.CommonProxy"
    )
    public static CommonProxy proxy;

    public static Logger log;
    
    @CapabilityInject(ILightSourceCapability.class)
    public static Capability<ILightSourceCapability> LIGHT_SOURCE_CAPABILITY = null;
    public static ResourceLocation LIGHT_SOURCE_CAPABILITY_NAME = new ResourceLocation(SEL.modId, "SELSourceCap");

    @EventHandler
    public void preInit(FMLPreInitializationEvent evt) {
        log = evt.getModLog();
        proxy.preInit(evt);
    }

    @EventHandler
    public void init(FMLInitializationEvent evt) {
        proxy.init();
    }
    
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new Command());
    }
	
	public static boolean enabledForDimension(int dimensionID) {
		if (Config.dimensionBlacklist == null || Config.dimensionBlacklist.length == 0) {
			return true;
		}
		for (int dim : Config.dimensionBlacklist) {
			if (dim == dimensionID) {
				return false;
			}
		}
		return true;		
	}
    
}
