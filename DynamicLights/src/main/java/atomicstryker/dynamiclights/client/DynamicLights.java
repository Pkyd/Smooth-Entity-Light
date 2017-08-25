package atomicstryker.dynamiclights.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import atomicstryker.dynamiclights.client.adaptors.BaseAdaptor;
import atomicstryker.dynamiclights.client.adaptors.PlayerSelfAdaptor;
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
 *         Then LakMoore took out the threads and multiple mods and implemented
 *         tracking "per chunk" - as it turns out... its faster and smoother.
 *
 */
@Mod(modid = DynamicLights.modId, name = "Dynamic Lights", version = "MODVERSION", acceptedMinecraftVersions = "MCVERSION")
public class DynamicLights {
    public final static String modId = "dynamiclights";

    static PlayerSelfAdaptor thePlayer;

    private static IBlockAccess lastWorld;
    private static HashMap<Long, ArrayList<DynamicLightSourceContainer>> lastList;
    static HashSet<Entity> trackedEntities;
    static HashSet<BaseAdaptor> trackedAdaptors;

    /**
     * This Map contains a List of DynamicLightSourceContainer for each World.
     * Since the client can only be in a single World, the other Lists just
     * float idle when unused.
     */
    static HashMap<World, HashMap<Long, ArrayList<DynamicLightSourceContainer>>> worldLightsMap;

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
    public static int getLightValue(IBlockAccess world, Block block, int x, int y, int z) {
        int vanillaValue = block.getLightValue(world, x, y, z);

        if (vanillaValue == 15 || globalLightsOff || world instanceof WorldServer) {
            return vanillaValue;
        }

        if (!world.equals(lastWorld) || lastList == null) {
            lastWorld = world;
            lastList = worldLightsMap.get(world);
        }

        if (world instanceof World) {
            int dynamicValue = 0;
            if (lastList != null && !lastList.isEmpty()) {
                ArrayList<DynamicLightSourceContainer> chunkList = lastList
                        .get(ChunkCoordIntPair.chunkXZ2Int(x >> 4, z >> 4));
                if (chunkList != null) {
                    for (DynamicLightSourceContainer light : chunkList) {
                        if (light.getX() == x && light.getY() == y && light.getZ() == z) {
                            dynamicValue = maxLight(dynamicValue, light.getLightSource().getLightLevel());
                        }
                        if (dynamicValue == 15)
                            return 15;
                    }
                }
            }
            return maxLight(vanillaValue, dynamicValue);

        }

        return vanillaValue;

    }

    /**
     * Exposed method to register active Dynamic Light Sources with. Does all
     * the necessary checks, prints errors if any occur, creates new World
     * entries in the worldLightsMap
     * 
     * @param lightToAdd
     *            IDynamicLightSource to register
     */
    public static void addLightSource(IDynamicLightSource lightToAdd) {
        // System.out.println("Calling addLightSource "+lightToAdd+", world
        // "+lightToAdd.getAttachmentEntity().worldObj);
        Entity entity = lightToAdd.getAttachmentEntity();
        if (entity != null) {
            if (entity.isEntityAlive()) {
                DynamicLightSourceContainer newLightContainer = new DynamicLightSourceContainer(lightToAdd);

                ArrayList<DynamicLightSourceContainer> lightList = getLightListForEntitiesChunk(entity);

                if (!lightList.contains(newLightContainer)) {
                    // System.out.println("Successfully registered Dynamic Light
                    // on Entity:
                    // "+newLightContainer.getLightSource().getAttachmentEntity()+"
                    // in list "+lightList);
                    lightList.add(newLightContainer);
                } else {
                    log.warn("Cannot add Dynamic Light: Attachment Entity is already registered!");
                }
            } else {
                log.warn("Cannot add Dynamic Light: Attachment Entity is dead!");
            }
        } else {
            log.warn("Cannot add Dynamic Light: Attachment Entity is null!");
        }
    }

    public static ArrayList<DynamicLightSourceContainer> getLightListForEntitiesChunk(Entity entity) {
        return getLightListForChunkXZ(entity.worldObj, entity.chunkCoordX, entity.chunkCoordZ);
    }

    public static ArrayList<DynamicLightSourceContainer> getLightListForChunkXZ(World world, int chunkX, int chunkZ) {
        if (world != null) {
            HashMap<Long, ArrayList<DynamicLightSourceContainer>> chunkList = DynamicLights.worldLightsMap.get(world);
            if (chunkList == null) {
                chunkList = new HashMap<Long, ArrayList<DynamicLightSourceContainer>>();
                DynamicLights.worldLightsMap.put(world, chunkList);
            }
            Long chunk = ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ);
            ArrayList<DynamicLightSourceContainer> lightList = chunkList.get(chunk);
            if (lightList == null) {
                lightList = new ArrayList<DynamicLightSourceContainer>();
                chunkList.put(chunk, lightList);
            }
            return lightList;
        } else {
            log.error("Cannot getLightListForChunkXZ(): World does not exist!");
            return new ArrayList<DynamicLightSourceContainer>();
        }
    }

    /**
     * Exposed method to remove active Dynamic Light sources with. If it fails
     * for whatever reason, it does so quietly.
     * 
     * @param lightToRemove
     *            IDynamicLightSource you want removed.
     */
    public static void removeLightSource(IDynamicLightSource lightToRemove) {
        if (lightToRemove != null && lightToRemove.getAttachmentEntity() != null) {
            Entity entity = lightToRemove.getAttachmentEntity();
            World world = entity.worldObj;
            if (world != null) {
                DynamicLightSourceContainer iterContainer = null;
                ArrayList<DynamicLightSourceContainer> lightList = getLightListForEntitiesChunk(entity);
                Iterator<DynamicLightSourceContainer> iter = lightList.iterator();
                while (iter.hasNext()) {
                    iterContainer = (DynamicLightSourceContainer) iter.next();
                    if (iterContainer.getLightSource().equals(lightToRemove)) {
                        iter.remove();
                        break;
                    }
                }

                if (iterContainer != null) {
                    world.updateLightByType(EnumSkyBlock.Block, iterContainer.getX(), iterContainer.getY(),
                            iterContainer.getZ());
                }
            }
        }
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
