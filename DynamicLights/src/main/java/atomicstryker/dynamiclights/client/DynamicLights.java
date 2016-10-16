package atomicstryker.dynamiclights.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
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

/**
 * 
 * @author AtomicStryker, LakMoore
 * 
 * Rewritten and now-awesome Dynamic Lights Mod.
 * 
 * Instead of the crude base edits and inefficient giant loops of the original,
 * this Mod uses ASM transforming to hook into Minecraft with style and has an
 * API that does't suck. It also uses Forge events to register dropped Items.
 * 
 * Then LakMoore took out the threads and multiple mods to see if simpler is better??
 *
 */
@Mod(modid = DynamicLights.modId, name = "Dynamic Lights", version = "1.3.9a", acceptedMinecraftVersions = "1.7.10")
public class DynamicLights
{
    public final static String modId = "DynamicLights";

    static Minecraft mcinstance;
    static PlayerSelfAdaptor thePlayer;

    private static IBlockAccess lastWorld;
    private static ConcurrentLinkedQueue<DynamicLightSourceContainer> lastList;
    static ArrayList<BaseAdaptor> trackedEntities;

    /**
     * This Map contains a List of DynamicLightSourceContainer for each World. Since the client can only
     * be in a single World, the other Lists just float idle when unused.
     */
    static ConcurrentHashMap<World, ConcurrentLinkedQueue<DynamicLightSourceContainer>> worldLightsMap;

    /**
     * Keeps track of the toggle button.
     */
    public static boolean globalLightsOff;
    static long nextLightUpdateTime;

    /**
     * The Keybinding instance to monitor
     */
    static KeyBinding toggleButton;
    static long nextKeyTriggerTime;

    /**
     * whether or not the colored lights mod is present
     */
    static boolean coloredLights;

    public static boolean fmlOverrideEnable;

    public static HashMap<Class<? extends Entity>, Boolean> lightValueMap;
    public static HashMap<Class<? extends Entity>, Integer> glowValueMap;
    
    // Proxy
    @SidedProxy(clientSide="atomicstryker.dynamiclights.client.ClientProxy", serverSide="atomicstryker.dynamiclights.client.CommonProxy")
    public static CommonProxy proxy;


    @EventHandler
    public void preInit(FMLPreInitializationEvent evt)
    {
        proxy.preInit(evt);
    }
    
    @EventHandler
    public void init(FMLInitializationEvent evt)
    {
        proxy.init();
    }   

    /**
     * Exposed method which is called by the transformed World.computeBlockLightValue method instead of
     * Block.blocksList[blockID].getLightValue. Loops active Dynamic Light Sources and if it finds
     * one for the exact coordinates asked, returns the Light value from that source if higher.
     * 
     * @param world World queried
     * @param block Block instance of target coords
     * @param x coordinate queried
     * @param y coordinate queried
     * @param z coordinate queried
     * @return Block.blocksList[blockID].getLightValue or Dynamic Light value, whichever is higher
     */
    public static int getLightValue(IBlockAccess world, Block block, int x, int y, int z)
    {
        int vanillaValue = block.getLightValue(world, x, y, z);

        if (globalLightsOff || world instanceof WorldServer)
        {
            return vanillaValue;
        }

        if (!world.equals(lastWorld) || lastList == null)
        {
            lastWorld = world;
            lastList = worldLightsMap.get(world);
        }

        int dynamicValue = 0;
        if (lastList != null && !lastList.isEmpty())
        {
            for (DynamicLightSourceContainer light : lastList)
            {
                if (light.getX() == x)
                {
                    if (light.getY() == y)
                    {
                        if (light.getZ() == z)
                        {
                            dynamicValue = maxLight(dynamicValue, light.getLightSource().getLightLevel());
                        }
                    }
                }
            }
        }
        return maxLight(vanillaValue, dynamicValue);
    }

    /**
     * Exposed method to register active Dynamic Light Sources with. Does all the necessary
     * checks, prints errors if any occur, creates new World entries in the worldLightsMap
     * @param lightToAdd IDynamicLightSource to register
     */
    public static void addLightSource(IDynamicLightSource lightToAdd)
    {
        //System.out.println("Calling addLightSource "+lightToAdd+", world "+lightToAdd.getAttachmentEntity().worldObj);
        if (lightToAdd.getAttachmentEntity() != null)
        {
            if (lightToAdd.getAttachmentEntity().isEntityAlive())
            {
                DynamicLightSourceContainer newLightContainer = new DynamicLightSourceContainer(lightToAdd);
                ConcurrentLinkedQueue<DynamicLightSourceContainer> lightList = DynamicLights.worldLightsMap.get(lightToAdd.getAttachmentEntity().worldObj);
                if (lightList != null)
                {
                    if (!lightList.contains(newLightContainer))
                    {
                        //System.out.println("Successfully registered Dynamic Light on Entity: "+newLightContainer.getLightSource().getAttachmentEntity()+" in list "+lightList);
                        lightList.add(newLightContainer);
                    }
                    else
                    {
                        System.out.println("Cannot add Dynamic Light: Attachment Entity is already registered!");
                    }
                }
                else
                {
                    lightList = new ConcurrentLinkedQueue<DynamicLightSourceContainer>();
                    lightList.add(newLightContainer);
                    worldLightsMap.put(lightToAdd.getAttachmentEntity().worldObj, lightList);
                }
            }
            else
            {
                System.err.println("Cannot add Dynamic Light: Attachment Entity is dead!");
            }
        }
        else
        {
            System.err.println("Cannot add Dynamic Light: Attachment Entity is null!");
        }
    }

    /**
     * Exposed method to remove active Dynamic Light sources with. If it fails for whatever reason,
     * it does so quietly.
     * @param lightToRemove IDynamicLightSource you want removed.
     */
    public static void removeLightSource(IDynamicLightSource lightToRemove)
    {
        if (lightToRemove != null && lightToRemove.getAttachmentEntity() != null)
        {
            World world = lightToRemove.getAttachmentEntity().worldObj;
            if (world != null)
            {
                DynamicLightSourceContainer iterContainer = null;
                ConcurrentLinkedQueue<DynamicLightSourceContainer> lightList = worldLightsMap.get(world);
                if (lightList != null)
                {
                    Iterator<DynamicLightSourceContainer> iter = lightList.iterator();
                    while (iter.hasNext())
                    {
                        iterContainer = (DynamicLightSourceContainer) iter.next();
                        if (iterContainer.getLightSource().equals(lightToRemove))
                        {
                            iter.remove();
                            break;
                        }
                    }

                    if (iterContainer != null)
                    {
                        world.updateLightByType(EnumSkyBlock.Block, iterContainer.getX(), iterContainer.getY(), iterContainer.getZ());
                    }
                }
            }
        }
    }

    /**
     * Compatibility extension for CptSpaceToaster's colored lights mod
     */
    public static int maxLight(int a, int b)
    {
        if (coloredLights)
        {
            if ((((0x100000 | b) - a) & 0x84210) > 0)
            {
                // some color components of A > B
                return a;
            }
            return b;
        }
        else
        {
            return Math.max(a, b);
        }
    }
}
