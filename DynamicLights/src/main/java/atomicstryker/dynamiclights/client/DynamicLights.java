package atomicstryker.dynamiclights.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;

import org.lwjgl.input.Keyboard;

import atomicstryker.dynamiclights.client.adaptors.BaseAdaptor;
import atomicstryker.dynamiclights.client.adaptors.BrightAdaptor;
import atomicstryker.dynamiclights.client.adaptors.CreeperAdaptor;
import atomicstryker.dynamiclights.client.adaptors.EntityBurningAdaptor;
import atomicstryker.dynamiclights.client.adaptors.EntityItemAdaptor;
import atomicstryker.dynamiclights.client.adaptors.FloodLightAdaptor;
import atomicstryker.dynamiclights.client.adaptors.MobLightAdaptor;
import atomicstryker.dynamiclights.client.adaptors.PlayerOtherAdaptor;
import atomicstryker.dynamiclights.client.adaptors.PlayerSelfAdaptor;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

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
@Mod(modid = DynamicLights.modId, name = "Dynamic Lights", version = "2.0.0", acceptedMinecraftVersions = "1.7.10")
public class DynamicLights
{
    public final static String modId = "DynamicLights";

    private Minecraft mcinstance;
    private PlayerSelfAdaptor thePlayer;

    private static IBlockAccess lastWorld;
    private static ConcurrentLinkedQueue<DynamicLightSourceContainer> lastList;
    private ArrayList<BaseAdaptor> trackedEntities;

    /**
     * This Map contains a List of DynamicLightSourceContainer for each World. Since the client can only
     * be in a single World, the other Lists just float idle when unused.
     */
    private static ConcurrentHashMap<World, ConcurrentLinkedQueue<DynamicLightSourceContainer>> worldLightsMap;

    /**
     * Keeps track of the toggle button.
     */
    public static boolean globalLightsOff;
    private long nextLightUpdateTime;

    /**
     * The Keybinding instance to monitor
     */
    private KeyBinding toggleButton;
    private long nextKeyTriggerTime;

    /**
     * whether or not the colored lights mod is present
     */
    private static boolean coloredLights;

    private Configuration config;
    private static int updateInterval;
    private static boolean lightBurningEntities;
    private static boolean lightGlowingEntities;
    private static boolean lightChargingCreepers;
    private static boolean lightDroppedItems;
    private static boolean lightMobEquipment;
    private static boolean lightFlamingArrows;
    private static boolean lightFloodLight;
    private static boolean lightThisPlayer;
    private static boolean lightOtherPlayers;
    private static boolean optifineOverride;

    public static boolean simpleMode;

    public static boolean fmlOverrideEnable;

    public static ItemConfigHelper itemsMap;
    public static ItemConfigHelper notWaterProofItems;
    public static HashMap<Class<? extends Entity>, Boolean> lightValueMap;
    public static HashMap<Class<? extends Entity>, Integer> glowValueMap;
    public static ItemConfigHelper floodLights;

    private final static String catFloodlight = "floodlights";
    private final static String catAdaptors = "adaptors";
    private final static String catMobGlow = "glowing entities";
    private final static String catMobFire = "flamable entities";

    private static final int LIGHT_LEVEL_BLAZE = 10;
    private static final int LIGHT_LEVEL_MAGMA_CUBE = 8;

    @EventHandler
    public void preInit(FMLPreInitializationEvent evt)
    {
        globalLightsOff = false;
        mcinstance = FMLClientHandler.instance().getClient();
        worldLightsMap = new ConcurrentHashMap<World, ConcurrentLinkedQueue<DynamicLightSourceContainer>>();
        lightValueMap = new HashMap<Class<? extends Entity>, Boolean>();
        glowValueMap = new HashMap<Class<? extends Entity>, Integer>();

        config = new Configuration(evt.getSuggestedConfigurationFile());
        config.load();

        config.addCustomCategoryComment(catAdaptors, "Each Adaptor is responsible for a certain type of dynamic light.  Turn them on or off here.");
        config.addCustomCategoryComment(catFloodlight, "Floodlights can simulate a single point light or a cone of light (May be slower!)");
        config.addCustomCategoryComment(catMobGlow, "Mobs/Entities will naturally radiate light with the value of this setting.");
        config.addCustomCategoryComment(catMobFire, "Set to false if you don't want that Entity Class to emit dynamic light when on fire.");

        Property burningEnts = config.get(catAdaptors, "Light from Burning Entities", true);
        burningEnts.comment = "Set to false to disable dynamic light from mobs on fire.";
        lightBurningEntities = burningEnts.getBoolean();

        Property glowingEnts = config.get(catAdaptors, "Entities Naturally Glow", true);
        glowingEnts.comment = "Set to false to disable natural dynamic light from mobs.";
        lightGlowingEntities = glowingEnts.getBoolean();

        Property chargingCreepers = config.get(catAdaptors, "Light from Charging Creepers", true);
        chargingCreepers.comment = "Set to false to disable dynamic light from creepers while charging.";
        lightChargingCreepers = chargingCreepers.getBoolean();

        Property droppedItems = config.get(catAdaptors, "Light from Dropped Items", true);
        droppedItems.comment = "Set to false to disable dynamic light from dropped items, like torches.";
        lightDroppedItems = droppedItems.getBoolean();

        Property mobEquip = config.get(catAdaptors, "Light from Mob Equipment", true);
        mobEquip.comment = "Set to false to disable dynamic light from mobs holding torches, etc.";
        lightMobEquipment = mobEquip.getBoolean();

        Property flamingArrows = config.get(catAdaptors, "Light from Flaming Arrows", true);
        flamingArrows.comment = "Set to false to disable dynamic light from flaming arrows.";
        lightFlamingArrows = flamingArrows.getBoolean();

        Property floodLight = config.get(catAdaptors, "Flood Light", true, "Set to false to disable dynamic flood light from certain held items.");
        floodLight.comment = "Set to false to disable dynamic flood light from certain held items.";
        lightFloodLight = floodLight.getBoolean();

        simpleMode = config.get(catFloodlight, "Simple Floodlight Mode", true, "Simulate a single point light instead of a cone of light (May be slower!)").getBoolean(true);

        Property floodLightItems = config.get(catFloodlight, "Flood Light Items", "ender_eye");
        floodLightItems.comment = "List of comma separated items that shine floodlight while held.";
        floodLights = new ItemConfigHelper(floodLightItems.getString(), 15);

        Property thisPlayer = config.get(catAdaptors, "Light from Held Items", true);
        thisPlayer.comment = "Set to false to disable dynamic light from held items.";
        lightThisPlayer = thisPlayer.getBoolean();

        Property otherPlayers = config.get(catAdaptors, "Light from Other Players", true);
        otherPlayers.comment = "Set to false to disable dynamic light from items held by other players.";
        lightOtherPlayers = otherPlayers.getBoolean();

        Property updateI = config.get(Configuration.CATEGORY_GENERAL, "Update Interval", 50);
        updateI.comment = "Update Interval time in milliseconds. The lower the better and costlier.";
        updateInterval = updateI.getInt();

        Property itemsList = config.get(Configuration.CATEGORY_GENERAL, "Light Items", "torch,glowstone=12,glowstone_dust=10,lit_pumpkin,lava_bucket,redstone_torch=10,redstone=10,golden_helmet=14,easycoloredlights:easycoloredlightsCLStone=-1");
        itemsList.comment = "Comma separated list of items that shine light when dropped in the World or held in player's or mob's hands.";
        itemsMap = new ItemConfigHelper(itemsList.getString(), 15);

        Property notWaterProofList = config.get(Configuration.CATEGORY_GENERAL, "Items Turned Off By Water", "torch,lava_bucket");
        notWaterProofList.comment = "Comma separated list of items that do not give off light when dropped and in water, have to be present in Light Items.";
        notWaterProofItems = new ItemConfigHelper(notWaterProofList.getString(), 1);

        Property optifine = config.get(Configuration.CATEGORY_GENERAL, "Optifine Override", false);
        optifine.comment = "Optifine has a Dynamic Lights of its own.  This mod will turn itself off if Optifine is loaded." + Configuration.NEW_LINE + "Set this to true if you aren't going to use Optifine's Dynamic Lights (even though they work just as well!).";
        optifineOverride = optifine.getBoolean();

        config.save();

        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);

        nextKeyTriggerTime = System.currentTimeMillis();
        nextLightUpdateTime = System.currentTimeMillis();

    }

    @EventHandler
    public void load(FMLInitializationEvent evt)
    {
        trackedEntities = new ArrayList<BaseAdaptor>();
        toggleButton = new KeyBinding("Dynamic Lights toggle", Keyboard.KEY_L, "key.categories.gameplay");
        ClientRegistry.registerKeyBinding(toggleButton);
        coloredLights = Loader.isModLoaded("easycoloredlights");
    }	

    @SubscribeEvent
    public void onJoinWorld(EntityJoinWorldEvent event)
    {
        if(!event.world.isRemote)
            return;

        Entity entity = event.entity;

        if (!entity.isEntityAlive())
            return;

        if (entity instanceof EntityItem)
        {
            if(!lightDroppedItems)
                return;

            EntityItemAdaptor adapter = new EntityItemAdaptor((EntityItem)entity);
            adapter.onTick();
            trackedEntities.add(adapter);    		
        }
        else if (entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer))
        {
            int minLight = 0;
            boolean catchesFire = false;

            if (lightBurningEntities)
            {
                if (!lightValueMap.containsKey(entity.getClass()))
                {
                    boolean value = config.get(DynamicLights.catMobFire, entity.getClass().getSimpleName(), true).getBoolean();
                    config.save();

                    lightValueMap.put(entity.getClass(), value);
                    catchesFire = value;
                }
                else
                {
                    catchesFire = lightValueMap.get(entity.getClass());
                }
            }

            if (lightGlowingEntities)
            {
                if (!glowValueMap.containsKey(entity.getClass()))
                {

                    int value = 0;
                    if (entity instanceof EntityBlaze)
                        value = DynamicLights.LIGHT_LEVEL_BLAZE;
                    else if (entity instanceof EntityMagmaCube)
                        value = DynamicLights.LIGHT_LEVEL_MAGMA_CUBE;

                    value = config.get(DynamicLights.catMobGlow, entity.getClass().getSimpleName(), value).getInt();
                    config.save();

                    glowValueMap.put(entity.getClass(), value);
                    minLight = value;
                }
                else
                {
                    minLight = glowValueMap.get(entity.getClass());
                }

            }			

            if (catchesFire)
            {
                EntityBurningAdaptor adaptor = new EntityBurningAdaptor(entity);
                adaptor.minLight = minLight;
                adaptor.onTick();
                trackedEntities.add(adaptor);
            }
            else if (minLight > 0)
            {
                BrightAdaptor adaptor = new BrightAdaptor(entity, minLight);
                adaptor.onTick();
                trackedEntities.add(adaptor);				
            }

            if (lightMobEquipment)
            {
                MobLightAdaptor adapter = new MobLightAdaptor((EntityLivingBase)entity);
                adapter.onTick();
                trackedEntities.add(adapter);				
            }

        }    
        else if (entity instanceof EntityArrow || entity instanceof EntityFireball)
        {
            if(!lightFlamingArrows)
                return;

            EntityBurningAdaptor adapter = new EntityBurningAdaptor(entity);
            adapter.onTick();
            trackedEntities.add(adapter);
        }
        else if (entity instanceof EntityOtherPlayerMP)
        {
            if(!lightOtherPlayers)
                return;

            PlayerOtherAdaptor adapter = new PlayerOtherAdaptor((EntityOtherPlayerMP)entity);
            adapter.onTick();
            trackedEntities.add(adapter);
        }
        else if (entity instanceof EntityPlayerSP)
        {
            if (lightFloodLight)
            {
                FloodLightAdaptor adaptor = new FloodLightAdaptor(entity, simpleMode);
                adaptor.onTick();
                trackedEntities.add(adaptor);
            }

            if (!lightThisPlayer)
                return;

            thePlayer = new PlayerSelfAdaptor((EntityPlayer)entity);
            thePlayer.onTick();
            trackedEntities.add(thePlayer);

            checkForOptifine();			

        }
        else
        {
            //Do nothing
        }

    }

    private void checkForOptifine() 
    {
        if (FMLClientHandler.instance().hasOptifine() && !optifineOverride)
        {
            mcinstance.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("Optifine is loaded.  Disabling LakMoore's Dynamic Lights.  Check the config file to override."));			
            DynamicLights.globalLightsOff = true;
        }
    }

    @SubscribeEvent
    public void onPlaySoundAtEntity(PlaySoundAtEntityEvent event)
    {
        if (lightChargingCreepers && event.name != null && event.name.equals("creeper.primed") && event.entity != null && event.entity instanceof EntityCreeper)
        {
            if (event.entity.isEntityAlive())
            {
                CreeperAdaptor creeper = new CreeperAdaptor((EntityCreeper) event.entity);
                creeper.onTick();
                DynamicLights.addLightSource(creeper);
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent tick)
    {	
        mcinstance.mcProfiler.startSection("dynamicLightsTick");
        if (tick.phase == Phase.END && mcinstance.theWorld != null)
        {
            if (!globalLightsOff && System.currentTimeMillis() >= nextLightUpdateTime)
            {
                nextLightUpdateTime = System.currentTimeMillis() + updateInterval;

                //Update all the adaptors
                Iterator<BaseAdaptor> entIter = trackedEntities.iterator();
                BaseAdaptor adaptor;

                while (entIter.hasNext())
                {
                    adaptor = entIter.next();
                    if(adaptor.getAttachmentEntity().isDead)
                    {
                        adaptor.disableLight();
                        entIter.remove();
                    }
                    else
                    {            		
                        adaptor.onTick();
                    }
                }

                //Update all the lights we found
                ConcurrentLinkedQueue<DynamicLightSourceContainer> worldLights = worldLightsMap.get(mcinstance.theWorld);

                if (worldLights != null)
                {
                    Iterator<DynamicLightSourceContainer> iter = worldLights.iterator();
                    while (iter.hasNext())
                    {
                        DynamicLightSourceContainer tickedLightContainer = iter.next();
                        if (tickedLightContainer.onUpdate())
                        {
                            iter.remove();
                            mcinstance.theWorld.updateLightByType(EnumSkyBlock.Block, tickedLightContainer.getX(), tickedLightContainer.getY(), tickedLightContainer.getZ());
                            //System.out.println("Dynamic Lights killing off LightSource on dead Entity "+tickedLightContainer.getLightSource().getAttachmentEntity());
                        }
                    }
                }

            }

            if (mcinstance.currentScreen == null && toggleButton.getIsKeyPressed() && System.currentTimeMillis() >= nextKeyTriggerTime)
            {
                nextKeyTriggerTime = System.currentTimeMillis() + 1000l;
                globalLightsOff = !globalLightsOff;
                mcinstance.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("Dynamic Lights globally " + (globalLightsOff ? "off" : "on")));

                World world = mcinstance.theWorld;
                if (world != null)
                {
                    ConcurrentLinkedQueue<DynamicLightSourceContainer> worldLights = worldLightsMap.get(mcinstance.theWorld);

                    if (worldLights != null)
                    {
                        Iterator<DynamicLightSourceContainer> iter = worldLights.iterator();
                        while (iter.hasNext())
                        {
                            DynamicLightSourceContainer c = iter.next();
                            world.updateLightByType(EnumSkyBlock.Block, c.getX(), c.getY(), c.getZ());
                        }
                    }
                }
            }
        }
        mcinstance.mcProfiler.endSection();
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
                ConcurrentLinkedQueue<DynamicLightSourceContainer> lightList = worldLightsMap.get(lightToAdd.getAttachmentEntity().worldObj);
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
