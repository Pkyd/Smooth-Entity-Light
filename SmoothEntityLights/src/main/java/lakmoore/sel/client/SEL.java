package lakmoore.sel.client;

import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

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
	acceptedMinecraftVersions = "MCVERSION"
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
    public static Profiler mcProfiler;

    @EventHandler
    public void preInit(FMLPreInitializationEvent evt) {
        log = evt.getModLog();
        proxy.preInit(evt);
    }

    @EventHandler
    public void init(FMLInitializationEvent evt) {
        proxy.init();
        mcProfiler = Minecraft.getMinecraft().mcProfiler;
    }
    
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new Command());
    }

	public static void onEntityRemoved(Entity entity) {
        SELSourceContainer sources = (SELSourceContainer)entity.getExtendedProperties(SEL.modId);                		
        if (sources != null)
    		sources.destroy();
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
