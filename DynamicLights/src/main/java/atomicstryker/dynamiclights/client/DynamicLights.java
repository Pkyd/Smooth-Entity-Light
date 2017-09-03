package atomicstryker.dynamiclights.client;

import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

/**
 * 
 * @author AtomicStryker, LakMoore
 * 
 *         Rewritten and now-awesome Dynamic Lights Mod.
 * 
 *         Instead of the crude base edits and inefficient giant loops of the
 *         original, this Mod uses ASM transforming to hook into Minecraft with
 *         style and has an API that does't suck. It also uses Forge events to
 *         register dropped Items.
 * 
 *         Then LakMoore took out the threads and multiple mods
 *         and it was faster and smoother.
 *         
 *         Then LakMoore removed the custom Entity tracking in favour of Vanilla and Forge systems
 *         and tweaked Entities to be located "per chunk" - as it turns out... its faster.
 *
 */
@Mod(modid = DynamicLights.modId, name = "Dynamic Lights", version = "MODVERSION", acceptedMinecraftVersions = "MCVERSION")
public class DynamicLights {
    public final static String modId = "dynamiclights";

    /**
     * Keeps track of the toggle button.
     */
    public static boolean globalLightsOff;
    static long nextLightUpdateTime;

    /**
     * whether or not the colored lights mod is present
     */
    static boolean coloredLights;

    public static boolean fmlOverrideEnable;

    public static HashMap<Class<? extends Entity>, Boolean> lightValueMap;
    public static HashMap<Class<? extends Entity>, Integer> glowValueMap;

    // Proxy
    @SidedProxy(clientSide = "atomicstryker.dynamiclights.client.ClientProxy", serverSide = "atomicstryker.dynamiclights.client.CommonProxy")
    public static CommonProxy proxy;

    public static Logger log;

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

    /**
     * Exposed method which is called by the transformed
     * World.computeBlockLightValue method instead of
     * Block.blocksList[blockID].getLightValue. Loops active Dynamic Light
     * Sources and if it finds one for the exact coordinates asked, returns the
     * Light value from that source if higher.
     * 
     * @param world
     *            World queried
     * @param block
     *            Block instance of target coords
     * @param x
     *            coordinate queried
     * @param y
     *            coordinate queried
     * @param z
     *            coordinate queried
     * @return Block.blocksList[blockID].getLightValue or Dynamic Light value,
     *         whichever is higher
     */
    @SuppressWarnings("unchecked")
	public static int getLightValue(IBlockAccess world, Block block, int x, int y, int z) {
        int vanillaValue = block.getLightValue(world, x, y, z);

        if (vanillaValue == 15 || globalLightsOff || world == null || world instanceof WorldServer) {
            return vanillaValue;
        }

        if (world instanceof World) {
            int dynamicValue = 0;
            int i, j, k;
            List<Entity> entities = ((World) world).getChunkFromBlockCoords(x, z).entityLists[y >> 4];
            for (Entity entity : entities) {
            		i = MathHelper.floor_double(entity.posX);
            		j = MathHelper.floor_double(entity.posY);
            		k = MathHelper.floor_double(entity.posZ);
                if (i == x && j == y && k == z) {
                    DynamicLightSourceContainer sources = (DynamicLightSourceContainer)entity.getExtendedProperties(DynamicLights.modId);                		
                    if (sources == null) continue;
                    dynamicValue = maxLight(dynamicValue, sources.getLightLevel());
                }
                if (dynamicValue == 15)
                    return 15;            	
            }
            return maxLight(vanillaValue, dynamicValue);
        }
        return vanillaValue;
    }

	public static void onEntityRemoved(Entity entity) {
        DynamicLightSourceContainer sources = (DynamicLightSourceContainer)entity.getExtendedProperties(DynamicLights.modId);                		
        if (sources != null)
        		sources.destroy();
	}

    /**
     * Compatibility extension for CptSpaceToaster's colored lights mod
     */
    public static int maxLight(int a, int b) {
        if (coloredLights) {
            if ((((0x100000 | b) - a) & 0x84210) > 0) {
                // some color components of A > B
                return a;
            }
            return b;
        } else {
            return Math.max(a, b);
        }
    }
}
