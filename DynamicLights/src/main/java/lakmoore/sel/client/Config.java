package lakmoore.sel.client;

import java.io.File;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class Config 
{
    
    private static Configuration config;

    /*
     * Number of milliseconds between dynamic light updates
     */
    public static int updateInterval;

    /*
     * Configurable flags
     */
    public static boolean lightBurningEntities;
    public static boolean lightGlowingEntities;
    public static boolean lightChargingCreepers;
    public static boolean lightDroppedItems;
    public static boolean lightMobEquipment;
    public static boolean lightFlamingArrows;
    public static boolean lightFloodLight;
    public static boolean lightThisPlayer;
    public static boolean lightOtherPlayers;
    public static boolean lightXP;
    public static boolean optifineOverride;

    public static boolean simpleMode;
    
    public static ItemConfigHelper itemsMap;
    public static ItemConfigHelper notWaterProofItems;
    public static ItemConfigHelper floodLights;
    
    public static int[] dimensionBlacklist = {};
    
    /*
     * Category Names
     */
    private final static String catFloodlight = "floodlights";
    private final static String catAdaptors = "adaptors";
    private final static String catMobGlow = "glowing entities";
    private final static String catMobFire = "flamable entities";
    
    /*
     * Default light levels for glowing vanilla mobs
     */
    static final int LIGHT_LEVEL_BLAZE = 10;
    static final int LIGHT_LEVEL_MAGMA_CUBE = 8;
            
    public static void doConfig(File configFile)
    {
        config = new Configuration(configFile);
        config.load();

        config.addCustomCategoryComment(catAdaptors, "Each Adaptor is responsible for a certain type of entity light.  Turn them on or off here.");
        config.addCustomCategoryComment(catFloodlight, "Floodlights can simulate a single point light or a cone of light (May be slower!)");
        config.addCustomCategoryComment(catMobGlow, "Mobs/Entities will naturally radiate light with the value of this setting.");
        config.addCustomCategoryComment(catMobFire, "Set to false if you don't want that Entity Class to emit light when on fire.");

        Property burningEnts = config.get(catAdaptors, "Light from Burning Entities", true);
        burningEnts.comment = "Set to false to disable light from mobs on fire.";
        lightBurningEntities = burningEnts.getBoolean();

        Property glowingEnts = config.get(catAdaptors, "Entities Naturally Glow", true);
        glowingEnts.comment = "Set to false to disable natural light from mobs.";
        lightGlowingEntities = glowingEnts.getBoolean();

        Property chargingCreepers = config.get(catAdaptors, "Light from Charging Creepers", true);
        chargingCreepers.comment = "Set to false to disable light from creepers while charging.";
        lightChargingCreepers = chargingCreepers.getBoolean();

        Property droppedItems = config.get(catAdaptors, "Light from Dropped Items", true);
        droppedItems.comment = "Set to false to disable light from dropped items, like torches.";
        lightDroppedItems = droppedItems.getBoolean();

        Property mobEquip = config.get(catAdaptors, "Light from Mob Equipment", true);
        mobEquip.comment = "Set to false to disable light from mobs holding torches, etc.";
        lightMobEquipment = mobEquip.getBoolean();

        Property flamingArrows = config.get(catAdaptors, "Light from Flaming Arrows", true);
        flamingArrows.comment = "Set to false to disable light from flaming arrows.";
        lightFlamingArrows = flamingArrows.getBoolean();

        Property floodLight = config.get(catAdaptors, "Flood Light", true);
        floodLight.comment = "Set to false to disable flood light from certain held items.";
        lightFloodLight = floodLight.getBoolean();
        
        Property xpLight = config.get(catAdaptors, "XP Light", true);
        xpLight.comment = "Set to false to disable light from XP orbs.";
        lightXP = xpLight.getBoolean();

        simpleMode = config.get(catFloodlight, "Simple Floodlight Mode", true, "Simulate a single point light instead of a cone of light (May be slower!)").getBoolean(true);

        Property floodLightItems = config.get(catFloodlight, "Flood Light Items", "ender_eye");
        floodLightItems.comment = "List of comma separated items that shine floodlight while held.";
        floodLights = new ItemConfigHelper(floodLightItems.getString(), 15);

        Property thisPlayer = config.get(catAdaptors, "Light from Held Items", true);
        thisPlayer.comment = "Set to false to disable light from held items.";
        lightThisPlayer = thisPlayer.getBoolean();

        Property otherPlayers = config.get(catAdaptors, "Light from Other Players", true);
        otherPlayers.comment = "Set to false to disable light from items held by other players.";
        lightOtherPlayers = otherPlayers.getBoolean();

        Property itemsList = config.get(Configuration.CATEGORY_GENERAL, "Light Items", "torch,glowstone=12,glowstone_dust=10,lit_pumpkin,lava_bucket,redstone_torch=10,redstone=10,golden_helmet=14,easycoloredlights:easycoloredlightsCLStone=-1");
        itemsList.comment = "Comma separated list of items that shine light when dropped in the World or held in player's or mob's hands.";
        itemsMap = new ItemConfigHelper(itemsList.getString(), 15);

        Property notWaterProofList = config.get(Configuration.CATEGORY_GENERAL, "Items Turned Off By Water", "torch,lava_bucket");
        notWaterProofList.comment = "Comma separated list of items that do not give off light when dropped and in water, have to be present in Light Items.";
        notWaterProofItems = new ItemConfigHelper(notWaterProofList.getString(), 1);

        Property optifine = config.get(Configuration.CATEGORY_GENERAL, "Optifine Override", false);
        optifine.comment = "Optifine has an Entity Lights of its own.  This mod will turn itself off if Optifine is loaded." + Configuration.NEW_LINE + "Set this to true if you aren't going to use Optifine's Dynamic Lights (even though they work just as well!).";
        optifineOverride = optifine.getBoolean();
        
        Property dimBlacklist = config.get(Configuration.CATEGORY_GENERAL, "Dimension Blacklist", new int[] {});
        dimBlacklist.comment = "List of IDs for dimensions where Entity Lights should always be disabled.";
        Config.dimensionBlacklist = dimBlacklist.getIntList();

        config.save();
    }

    public static boolean getMobFire(String entityName) {
        boolean value = config.get(catMobFire, entityName, true).getBoolean();
        config.save();
        return value;
    }

    public static int getMobGlow(Entity entity) {
        int value = 0;
        if (entity instanceof EntityBlaze)
            value = LIGHT_LEVEL_BLAZE;
        else if (entity instanceof EntityMagmaCube)
            value = LIGHT_LEVEL_MAGMA_CUBE;

        value = config.get(catMobGlow, entity.getClass().getSimpleName(), value).getInt();
        config.save();
        return value;
    }
    
    public static void setHeldLight(ItemStack item, int lightLevel)
    {
        itemsMap.addItem(item, lightLevel);
        Property itemsList = config.get(Configuration.CATEGORY_GENERAL, "Light Items", "torch,glowstone=12,glowstone_dust=10,lit_pumpkin,lava_bucket,redstone_torch=10,redstone=10,golden_helmet=14,easycoloredlights:easycoloredlightsCLStone=-1");
        itemsList.comment = "Comma separated list of items that shine light when dropped in the World or held in player's or mob's hands.";
        itemsList.set(itemsMap.toString());
        config.getCategory(Configuration.CATEGORY_GENERAL).put("Light Items", itemsList);
        config.save();
    }

    public static void toggleFloodlight(ItemStack item)
    {
        if (floodLights.getLightFromItemStack(item) == 15)
            floodLights.addItem(item, 0);            
        else
            floodLights.addItem(item, 15);                      
      
        Property floodLightItems = config.get(catFloodlight, "Flood Light Items", "ender_eye");
        floodLightItems.comment = "List of comma separated items that shine floodlight while held.";
        floodLightItems.set(floodLights.toString());
        
        config.getCategory(catFloodlight).put("Flood Light Items", floodLightItems);
        config.save();        
    }

    public static void toggleWaterproof(ItemStack item)
    {
        if (notWaterProofItems.getLightFromItemStack(item) == 1)
            notWaterProofItems.addItem(item, 0);            
        else
            notWaterProofItems.addItem(item, 1);                      
      
        Property notWaterProofList = config.get(Configuration.CATEGORY_GENERAL, "Items Turned Off By Water", "torch,lava_bucket");
        notWaterProofList.comment = "Comma separated list of items that do not give off light when dropped and in water, have to be present in Light Items.";
        notWaterProofList.set(notWaterProofItems.toString());
        
        config.getCategory(Configuration.CATEGORY_GENERAL).put("Items Turned Off By Water", notWaterProofList);
        config.save();        
    }

}
