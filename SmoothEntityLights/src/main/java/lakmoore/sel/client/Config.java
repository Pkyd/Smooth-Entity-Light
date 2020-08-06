package lakmoore.sel.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.BlazeEntity;
import net.minecraft.entity.monster.MagmaCubeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig.Type;

public class Config 
{
    /*
     * Default light levels for glowing vanilla mobs
     */
    static final int LIGHT_LEVEL_BLAZE = 10;
    static final int LIGHT_LEVEL_MAGMA_CUBE = 8;

	// Forge Config Boilerplate
	public static final Spec CLIENT;
	public static final Spec SERVER;
	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final ForgeConfigSpec SERVER_SPEC;
	static {
		final Pair<Spec, ForgeConfigSpec> specPairClient = new ForgeConfigSpec.Builder().configure(Spec::new);
		CLIENT_SPEC = specPairClient.getRight();
		CLIENT = specPairClient.getLeft();

		final Pair<Spec, ForgeConfigSpec> specPairServer = new ForgeConfigSpec.Builder().configure(Spec::new);
		SERVER_SPEC = specPairServer.getRight();
		SERVER = specPairServer.getLeft();
}
	
    public static boolean allowClientOverride = true;
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
    public static List<ResourceLocation> notWaterProofItems;
    public static List<ResourceLocation> floodLights;
    public static List<Integer> dimensionBlacklist;
    public static List<String> nonFlamableMobs;
    
    public static Map<ResourceLocation, Integer> lightValueMap;
    private static Map<String, Integer> glowValueMap;
    
    // ====== GETTERS =======

    public static int getMobGlow(Entity entity) {
        int value = 0;
        if (entity instanceof BlazeEntity)
            value = LIGHT_LEVEL_BLAZE;
        else if (entity instanceof MagmaCubeEntity)
            value = LIGHT_LEVEL_MAGMA_CUBE;

        Integer configValue = Config.glowValueMap.putIfAbsent(entity.getClass().getSimpleName(), value);
        if (configValue == null) {
        	List<String> saveAs = new ArrayList<String>();
        	Config.glowValueMap.forEach((name, lightVal) -> {
        		saveAs.add(String.format("%s=%s", name.toString(), lightVal));    		
        	});    	
            SEL.modConfig.getConfigData().set(Config.CLIENT.glowingMobs.getPath(), saveAs);
            SEL.modConfig.save();
            return value;        	
        } else {
        	return configValue;
        }
    }
        
    // ====== SETTERS =======
    
    public static void setHeldLight(ItemStack stack, int lightLevel)
    {
    	Config.lightValueMap.put(stack.getItem().getRegistryName(), lightLevel);
    	List<String> saveAs = new ArrayList<String>();
    	Config.lightValueMap.forEach((name, value) -> {
    		saveAs.add(String.format("%s=%s", name.toString(), value));    		
    	});    	
        SEL.modConfig.getConfigData().set(Config.CLIENT.itemsMap.getPath(), saveAs);
        SEL.modConfig.save();
    }

    public static void toggleFloodlight(ItemStack stack)
    {
        if (Config.floodLights.contains(stack.getItem().getRegistryName()))
        	Config.floodLights.remove(stack.getItem().getRegistryName());            
        else
        	Config.floodLights.add(stack.getItem().getRegistryName());                      

    	List<String> saveAs = new ArrayList<String>();
    	Config.floodLights.forEach((name) -> {
    		saveAs.add(name.toString());    		
    	});    	

        SEL.modConfig.getConfigData().set(Config.CLIENT.floodLights.getPath(), saveAs);
        SEL.modConfig.save();
    }

    public static void toggleWaterproof(ItemStack stack)
    {
        if (Config.notWaterProofItems.contains(stack.getItem().getRegistryName()))
        	Config.notWaterProofItems.remove(stack.getItem().getRegistryName());            
        else
        	Config.notWaterProofItems.add(stack.getItem().getRegistryName());                      

    	List<String> saveAs = new ArrayList<String>();
    	Config.notWaterProofItems.forEach((name) -> {
    		saveAs.add(name.toString());    		
    	});    	

        SEL.modConfig.getConfigData().set(Config.CLIENT.notWaterProofItems.getPath(), saveAs);
        SEL.modConfig.save();
    }
    
    // ======= BAKE CONFIG =======
	public static void bakeConfig(Type type) {
		
		if (type == Type.SERVER) {
			allowClientOverride = SERVER.allowClientOverride.get();			
			if (!allowClientOverride) {
				getConfig(SERVER);				
			}
		} else {
			if (allowClientOverride) {
				getConfig(CLIENT);				
			}
		}		
	}
	
	private static void getConfig(Spec specToUse) {
		lightBurningEntities = specToUse.lightBurningEntities.get();
		lightGlowingEntities = specToUse.lightGlowingEntities.get();
		lightChargingCreepers = specToUse.lightChargingCreepers.get();
		lightDroppedItems = specToUse.lightDroppedItems.get();
		lightMobEquipment = specToUse.lightMobEquipment.get();
		lightFlamingArrows = specToUse.lightFlamingArrows.get();
		lightFloodLight = specToUse.lightFloodLight.get();
		lightThisPlayer = specToUse.lightThisPlayer.get();
		lightOtherPlayers = specToUse.lightOtherPlayers.get();
		lightXP = specToUse.lightXP.get();
		optifineOverride = specToUse.optifineOverride.get();
		simpleMode = specToUse.simpleMode.get();
		notWaterProofItems = specToUse.notWaterProofItems.get().stream().map((str) -> {
			return new ResourceLocation(str);
		}).collect(Collectors.toList());
		floodLights = specToUse.floodLights.get().stream().map((str) -> {
			return new ResourceLocation(str);
		}).collect(Collectors.toList());
		dimensionBlacklist = specToUse.dimensionBlacklist.get();		
		nonFlamableMobs = specToUse.nonFlamableMobs.get();		

		lightValueMap = getResourceMapFromList(specToUse.itemsMap.get());
		glowValueMap = getMapFromList(specToUse.glowingMobs.get());
	}
	
	private static Map<String, Integer> getMapFromList(List<String> list) {
		return list.stream().map(configString -> {
			return configString.split("=");
		}).collect(
				Collectors.toMap(
						result -> result[0], 
						result -> Integer.parseInt(result[1])
				)
		);
	}

	private static Map<ResourceLocation, Integer> getResourceMapFromList(List<String> list) {
		return list.stream().map(configString -> {
			return configString.split("=");
		}).collect(
				Collectors.toMap(
						result -> new ResourceLocation(result[0]), 
						result -> Integer.parseInt(result[1])
				)
		);
	}

	public static class Spec {
				
	    /*
	     * Configurable flags
	     */
		public final ForgeConfigSpec.BooleanValue allowClientOverride;
	    public final ForgeConfigSpec.BooleanValue lightBurningEntities;
	    public final ForgeConfigSpec.BooleanValue lightGlowingEntities;
	    public final ForgeConfigSpec.BooleanValue lightChargingCreepers;
	    public final ForgeConfigSpec.BooleanValue lightDroppedItems;
	    public final ForgeConfigSpec.BooleanValue lightMobEquipment;
	    public final ForgeConfigSpec.BooleanValue lightFlamingArrows;
	    public final ForgeConfigSpec.BooleanValue lightFloodLight;
	    public final ForgeConfigSpec.BooleanValue lightThisPlayer;
	    public final ForgeConfigSpec.BooleanValue lightOtherPlayers;
	    public final ForgeConfigSpec.BooleanValue lightXP;
	    public final ForgeConfigSpec.BooleanValue optifineOverride;	
	    public final ForgeConfigSpec.BooleanValue simpleMode;
	    
	    public final ForgeConfigSpec.ConfigValue<List<String>> itemsMap;
	    public final ForgeConfigSpec.ConfigValue<List<String>> notWaterProofItems;
	    public final ForgeConfigSpec.ConfigValue<List<String>> floodLights;
	    public final ForgeConfigSpec.ConfigValue<List<String>> glowingMobs;
	    public final ForgeConfigSpec.ConfigValue<List<String>> nonFlamableMobs;
	    
	    public final ForgeConfigSpec.ConfigValue<List<Integer>> dimensionBlacklist;
	    	    	            
	    Spec(ForgeConfigSpec.Builder builder) {
	    	
	    	builder.comment("Each Adaptor is responsible for a certain type of entity light.  Turn them on or off here.").push("Adaptors");
	    		    			    		
			this.lightBurningEntities = builder
			    .comment("\nSet to false to disable light from mobs on fire.")
				.translation("sel.configgui.lightBurningEntities")
				.worldRestart()
				.define("Light from Burning Entities", true);

			this.lightGlowingEntities = builder
				    .comment("\nSet to false to disable natural light from mobs.")
					.translation("sel.configgui.lightGlowingEntities")
					.worldRestart()
					.define("Entities Naturally Glow", true);
	    	
			this.lightChargingCreepers = builder
				    .comment("\nSet to false to disable light from creepers while charging.")
					.translation("sel.configgui.lightChargingCreepers")
					.worldRestart()
					.define("Light from Charging Creepers", true);

			this.lightDroppedItems = builder
				    .comment("\nSet to false to disable light from dropped items, like torches.")
					.translation("sel.configgui.lightDroppedItems")
					.worldRestart()
					.define("Light from Dropped Items", true);	
	        
			this.lightMobEquipment = builder
				    .comment("\nSet to false to disable light from mobs holding torches, etc.")
					.translation("sel.configgui.lightMobEquipment")
					.worldRestart()
					.define("Light from Mob Equipment", true);

			this.lightFlamingArrows = builder
				    .comment("\nSet to false to disable light from flaming arrows.")
					.translation("sel.configgui.lightFlamingArrows")
					.worldRestart()
					.define("Light from Flaming Arrows", true);

			this.lightFloodLight = builder
				    .comment("\nSet to false to disable flood light (flash-light) from certain held items.")
					.translation("sel.configgui.lightFloodLight")
					.worldRestart()
					.define("Flood Light", true);

			this.lightXP = builder
				    .comment("\nSet to false to disable light from XP orbs.")
					.translation("sel.configgui.lightXP")
					.worldRestart()
					.define("XP Light", true);

			this.lightThisPlayer = builder
				    .comment("\nSet to false to disable light from held items.")
					.translation("sel.configgui.lightThisPlayer")
					.worldRestart()
					.define("Light from Held Items", true);

			this.lightOtherPlayers = builder
				    .comment("\nSet to false to disable light from items held by other players.")
					.translation("sel.configgui.lightOtherPlayers")
					.worldRestart()
					.define("Light from Other Players", true);

	        builder.pop();

	        // ============================
	    	
	    	builder.comment("Floodlights/Flashlights can simulate a single point light or a cone of light").push("Floodlights");

	    	this.simpleMode = builder
				    .comment("\nSimulate a single point light instead of a cone of light")
					.translation("sel.configgui.simpleMode")
					.worldRestart()
					.define("Simple Floodlight Mode", false);
	    	
	    	this.floodLights = builder
	    			.comment("\nList of comma separated items that shine floodlight while held.")
	    			.translation("sel.configgui.floodLights")
					.worldRestart()
					.define("Flood Light Items", Lists.newArrayList("ender_eye"));
	
	        builder.pop();

	        // ============================
	        
	    	builder.comment("General settings and light values").push("General");
	    		    	
	    	this.itemsMap = builder
	    			.comment("\nList of items that shine light at the given brightness when dropped in the World or held in player's or mob's hands.")
	    			.translation("sel.configgui.itemsList")
					.worldRestart()
					.define("Light Items", Lists.newArrayList(
							"minecraft:torch=15",
							"minecraft:glowstone=12",
							"minecraft:glowstone_dust=10",
							"minecraft:lit_pumpkin=15",
							"minecraft:lava_bucket=15",
							"minecraft:redstone_torch=10",
							"minecraft:redstone=10",
							"minecraft:golden_helmet=12"
							)
					);

	    	this.notWaterProofItems = builder
	    			.comment("\nList of items that do not give off light when dropped and in water, have to be present in Light Items.")
	    			.translation("sel.configgui.notWaterProofItems")
					.worldRestart()
					.define("Items Turned Off By Water", Lists.newArrayList(
							"minecraft:torch",
							"minecraft:lava_bucket"
							)
					);

	    	this.optifineOverride = builder
				    .comment("\nOptifine has an Entity Lights of its own.  This mod will turn itself off if Optifine is loaded.\nSet this to true if you aren't going to use Optifine's Dynamic Lights.")
					.translation("sel.configgui.optifineOverride")
					.worldRestart()
					.define("Optifine Override", false);

	    	this.dimensionBlacklist = builder
	    			.comment("\nList of IDs for Dimensions where Entity Lights should always be disabled.")
	    			.translation("sel.configgui.dimensionBlacklist")
					.worldRestart()
					.define("Dimension Blacklist", Lists.newArrayList()
					);	    	
	    	
	    	this.glowingMobs = builder
	    			.comment("\nList of Mobs that will naturally radiate light with the given brightness.")
	    			.translation("sel.configgui.glowingMobs")
					.worldRestart()
					.define("Glowing Entities", Lists.newArrayList(
						    "EntityBat=0",
						    "EntityBlaze=10",
						    "EntityCaveSpider=0",
						    "EntityChicken=0",
						    "EntityCow=0",
						    "EntityCreeper=0",
						    "EntityDonkey=0",
						    "EntityEnderman=0",
						    "EntityHorse=0",
						    "EntityLlama=0",
						    "EntityPig=0",
						    "EntitySheep=0",
						    "EntitySkeleton=0",
						    "EntitySkeletonHorse=0",
						    "EntitySlime=0",
						    "EntitySpider=0",
						    "EntitySquid=0",
						    "EntityVex=0",
						    "EntityVillager=0",
						    "EntityWitch=0",
						    "EntityZombie=0",
						    "EntityZombieVillager=0"
							)
					);

	    	this.nonFlamableMobs = builder
	    			.comment("\nIf the mob should NOT give off light when on fire, add their name to this list.")
	    			.translation("sel.configgui.nonFlamableMobs")
					.worldRestart()
					.define("Flaming Mob Blacklist", Lists.newArrayList(
						    "EntityBlaze"
							)
					);

		    builder.pop();

	        // ============================
		    
		    builder
		    	.comment("There are config files for both server and client for this client-side mod.\nUse this setting if you run a server and you don't want your users to be able to change their config.\nCould be used to force entity light on (for a nice effect) or force lights off (for increased difficulty).")
		    	.push("Client Override");

		    this.allowClientOverride = builder
	    			.comment("\nSet to false on the server to force clients to use server side config only.\nChanging this value on the Client has no effect.")
	    			.translation("sel.configgui.clientOverride")
	    			.worldRestart()
	    			.define("Allow Client to Override Server Config", true);

		    builder.pop();
	        
	    }
	    
	}
}
