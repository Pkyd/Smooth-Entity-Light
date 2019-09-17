package lakmoore.sel.client;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lakmoore.sel.client.adaptors.BrightAdaptor;
import lakmoore.sel.client.adaptors.CreeperAdaptor;
import lakmoore.sel.client.adaptors.EntityBurningAdaptor;
import lakmoore.sel.client.adaptors.EntityItemAdaptor;
import lakmoore.sel.client.adaptors.FloodLightAdaptor;
import lakmoore.sel.client.adaptors.MobLightAdaptor;
import lakmoore.sel.client.adaptors.PlayerOtherAdaptor;
import lakmoore.sel.client.adaptors.PlayerSelfAdaptor;
import lakmoore.sel.world.WorldSEL;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;

public class ForgeEventHandler 
{    
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
		FMLEventHandler.blocksToUpdate.addAll(LightUtils.getVolumeForRelight(event.x, event.y, event.z, 8));
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
		FMLEventHandler.blocksToUpdate.addAll(LightUtils.getVolumeForRelight(event.x, event.y, event.z, 8));
	}

    @SubscribeEvent
	@SideOnly(Side.CLIENT)
    public void onChunkLoad(ChunkEvent.Load event)
    {
    	if (event.world instanceof WorldSEL && SEL.enabledForDimension(ClientProxy.mcinstance.thePlayer.dimension)) {
    		Chunk chunk = event.getChunk();
    		ChunkCoordIntPair coords = new ChunkCoordIntPair(chunk.xPosition, chunk.zPosition);
    		if (!LightUtils.lightCache.containsKey(coords)) {
    			LightUtils.lightCache.put(coords, new LightCache());					
    		}
    	}
    }

    @SubscribeEvent
	@SideOnly(Side.CLIENT)
    public void onChunkUnload(ChunkEvent.Unload event)
    {
    	if(event.world instanceof WorldSEL) {
    		Chunk chunk = event.getChunk();
    		LightUtils.lightCache.remove(new ChunkCoordIntPair(chunk.xPosition, chunk.zPosition));		
    	}
    }

    @SubscribeEvent
    public void onDebugOverlay(RenderGameOverlayEvent.Text event)
    {    	
        if(Minecraft.getMinecraft().gameSettings.showDebugInfo)
        {
    		//There used to be some interesting stats to look at!
    		event.left.add("DL " + (SEL.disabled ? "OFF" : "ON"));
    		// Light levels
    		Entity player = Minecraft.getMinecraft().thePlayer;
    		World world = Minecraft.getMinecraft().theWorld;
    		int x = (int)player.posX;
			int y = (int)player.posY;
			int z = (int)player.posZ;
    		Block block = world.getBlock(x, y, z);
    		ChunkCoordIntPair coords = new ChunkCoordIntPair(x >> 4, z >> 4);
    		event.left.add(
    			"Vanilla BL: " + block.getLightValue(world, x, y, z) 
    			+ " SEL: " 
    			+ (
    				LightUtils.lightCache != null 
    				&& LightUtils.lightCache.get(coords) != null 
    				?	
    					Math.round(10f * LightUtils.getEntityLightLevel(world, x, y, z)) / 10f
    					+ " Cached: " + Math.round(10f * LightUtils.lightCache.get(coords).lights[x & 15][y][z & 15]) / 10f
	    			:
	    				"Disabled for this dimension"
	    		)
    		);
    		event.left.add(
    			"SEL avg blocks re-lit: " + Math.round(10f * FMLEventHandler.totalBlockCount() / FMLEventHandler.counts.size()) / 10f
    		);
        }
    } 
    
    @SubscribeEvent
    public void onJoinWorld(EntityJoinWorldEvent event)
    {
        Entity entity = event.entity;
        if(entity == null || !entity.isEntityAlive())
            return;

        World world = entity.worldObj;
        if(world == null || !world.isRemote)
            return;

        // Don't even add light sources to Entities in blacklisted dimensions
        if (!SEL.enabledForDimension(entity.dimension)) {
        	return;
        }
        
        SELSourceContainer sources = (SELSourceContainer)entity.getExtendedProperties(SEL.modId);
        if(sources == null) {
    		sources = new SELSourceContainer(entity, world);
    		entity.registerExtendedProperties(SEL.modId, sources);
        }

        if (entity instanceof EntityItem)
        {
            if(!Config.lightDroppedItems)
                return;

            EntityItemAdaptor adaptor = new EntityItemAdaptor((EntityItem)entity);
            sources.addLightSource(adaptor);
        }
        else if (entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer))
        {
            int minLight = 0;
            boolean catchesFire = false;

            if (Config.lightBurningEntities)
            {
                if (!SEL.lightValueMap.containsKey(entity.getClass()))
                {
                    boolean value = Config.getMobFire(entity.getClass().getSimpleName());
                    
                    SEL.lightValueMap.put(entity.getClass(), value);
                    catchesFire = value;
                }
                else
                {
                    catchesFire = SEL.lightValueMap.get(entity.getClass());
                }
            }

            if (Config.lightGlowingEntities)
            {
                if (!SEL.glowValueMap.containsKey(entity.getClass()))
                {
                    int value = Config.getMobGlow(entity);
                    SEL.glowValueMap.put(entity.getClass(), value);
                    minLight = value;
                }
                else
                {
                    minLight = SEL.glowValueMap.get(entity.getClass());
                }

            }           

            if (catchesFire)
            {
                EntityBurningAdaptor adaptor = new EntityBurningAdaptor(entity);
                adaptor.minLight = minLight;
                sources.addLightSource(adaptor);
            }
            else if (minLight > 0)
            {
                BrightAdaptor adaptor = new BrightAdaptor(entity, minLight);
                sources.addLightSource(adaptor);
            }

            if (Config.lightMobEquipment)
            {
                MobLightAdaptor adaptor = new MobLightAdaptor((EntityLivingBase)entity);
                sources.addLightSource(adaptor);
            }

        }    
        else if (entity instanceof EntityArrow || entity instanceof EntityFireball)
        {
            if(!Config.lightFlamingArrows)
                return;

            EntityBurningAdaptor adaptor = new EntityBurningAdaptor(entity);
            sources.addLightSource(adaptor);
        }
        else if (entity instanceof EntityXPOrb)
        {
            if(!Config.lightXP)
                return;

            BrightAdaptor adaptor = new BrightAdaptor(entity, 10);
            sources.addLightSource(adaptor);
        }
        else if (entity instanceof EntityOtherPlayerMP)
        {
            if(!Config.lightOtherPlayers)
                return;

            PlayerOtherAdaptor adaptor = new PlayerOtherAdaptor((EntityOtherPlayerMP)entity);
            sources.addLightSource(adaptor);
        }
        else if (entity instanceof EntityClientPlayerMP)
        {
            if (Config.lightFloodLight)
            {
                FloodLightAdaptor adaptor = new FloodLightAdaptor(entity, Config.simpleMode);
                sources.addLightSource(adaptor);
            }

            if (!Config.lightThisPlayer)
                return;

            PlayerSelfAdaptor adaptor = new PlayerSelfAdaptor((EntityPlayer)entity);
            sources.addLightSource(adaptor);

            checkForOptifine();
        }
        else
        {
            //Do nothing
        }

    }
    
    @SubscribeEvent
    public void onPlaySoundAtEntity(PlaySoundAtEntityEvent event)
    {
        if (
        	!SEL.disabled 
        	&& Config.lightChargingCreepers 
        	&& event.name != null 
        	&& event.name.equals("creeper.primed") 
        	&& event.entity != null 
        	&& event.entity instanceof EntityCreeper
        	&& event.entity.isEntityAlive()
        	&& SEL.enabledForDimension(event.entity.dimension)
        ) {
            SELSourceContainer sources = (SELSourceContainer)event.entity.getExtendedProperties(SEL.modId);                		
            if (sources == null)
        		return;
            CreeperAdaptor creeper = new CreeperAdaptor((EntityCreeper) event.entity);
            sources.addLightSource(creeper);
        }
    }
    
    private void checkForOptifine() 
    {
        if (FMLClientHandler.instance().hasOptifine() && !Config.optifineOverride)
        {
            ClientProxy.mcinstance.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("Optifine is loaded.  Disabling Atomic Stryker's Dynamic Lights.  Check the config file to override."));         
            SEL.disabled = true;
        }
    }

}
