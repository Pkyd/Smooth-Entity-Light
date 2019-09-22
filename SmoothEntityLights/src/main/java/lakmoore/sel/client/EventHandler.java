package lakmoore.sel.client;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import lakmoore.sel.capabilities.DefaultLightSourceCapability;
import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.client.adaptors.BrightAdaptor;
import lakmoore.sel.client.adaptors.CreeperAdaptor;
import lakmoore.sel.client.adaptors.EntityBurningAdaptor;
import lakmoore.sel.client.adaptors.EntityItemAdaptor;
import lakmoore.sel.client.adaptors.FloodLightAdaptor;
import lakmoore.sel.client.adaptors.MobLightAdaptor;
import lakmoore.sel.client.adaptors.PartialLightAdaptor;
import lakmoore.sel.client.adaptors.PlayerOtherAdaptor;
import lakmoore.sel.client.adaptors.PlayerSelfAdaptor;
import lakmoore.sel.world.WorldSEL;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EventHandler {
	
	private static boolean forceUpdate = false;

	public static LinkedList<Integer> counts = new LinkedList<Integer>();
	public static int tickCount = 0;
	public static int ticksSkippedCount = 0;
	
	public static HashSet<BlockPos> blocksToUpdate = new HashSet<BlockPos>();
    //Using a BlockPos to hold ChunkPos - with a Y value!
	public static HashSet<BlockPos> chunksToUpdate = new HashSet<BlockPos>();

	public static int totalBlockCount() {
		int count = 0;
		for (Object i : counts.toArray()) {
			count += (int)i;			
		}
		return count;
	}
			
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent tick) {
//		OFDL.mcProfiler.startSection(OFDL.modId + ":tick");
        if (
        	tick.phase == Phase.END 
        	&& ClientProxy.mcinstance.world != null
        	&& SEL.enabledForDimension(ClientProxy.mcinstance.player.dimension)
        ) {
        	
    		//Check for global lights key press
            if (
            	ClientProxy.mcinstance.currentScreen == null 
            	&& ClientProxy.toggleButton.isPressed()
                && System.currentTimeMillis() >= ClientProxy.nextKeyTriggerTime
                && !forceUpdate
            ) {
        		//key-repeat delay
                ClientProxy.nextKeyTriggerTime = System.currentTimeMillis() + 1000l;
                //toggle the setting
                SEL.disabled = !SEL.disabled;
                //player notification
                ClientProxy.mcinstance.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                        "Smooth Entity Lights " + (SEL.disabled ? "off" : "on")));
                forceUpdate = true;
                
            }
            
            // Check every loaded entity and update any that have light sources
            // No need to do this more than 25 times per second
            if (
            	forceUpdate 
            	|| (!SEL.disabled && System.currentTimeMillis() - SEL.lastLightUpdateTime > 40)
            ) {

                // Tick all entities
				List<Entity> allEntities = ClientProxy.mcinstance.world.loadedEntityList;
            	allEntities.parallelStream().forEach(new Consumer<Entity>() {
					@Override
					public void accept(Entity entity) {
			            ILightSourceCapability sources = entity.getCapability(SEL.LIGHT_SOURCE_CAPABILITY, null);                		
					    if (sources != null) {
					    	// getBlocksToUpdate ticks the source container and returns a list of dirty blocks
					    	for (BlockPos pos : sources.getBlocksToUpdate()) {
					    		blocksToUpdate.add(pos);
					    	}
					    }  
					}
				});
            	
            	// Let's record how much work we are doing
                counts.add(blocksToUpdate.size());
                tickCount++;
                while (counts.size() > 60) {
                	counts.remove();
                }
                
                //Using BlockPos to hold ChunkPos with a Y value!
                chunksToUpdate = new HashSet<BlockPos>();

                // update all blocks that have been marked dirty since last tick
                blocksToUpdate.parallelStream().forEach(new Consumer<BlockPos>() {
					@Override
					public void accept(BlockPos pos) {
						int x = pos.getX() >> 4;
						int z = pos.getZ() >> 4;
						BlockPos chunkCoords = new BlockPos(x, pos.getY() >> 4, z);
						chunksToUpdate.add(chunkCoords);
						LightCache lc = LightUtils.lightCache.get(new ChunkPos(x, z));
						if (lc != null) {
							int y = pos.getY();
							if (y < 0) y = 0;
					        lc.lights[pos.getX() & 15][y][pos.getZ() & 15] = LightUtils.getEntityLightLevel(ClientProxy.mcinstance.world, pos);
						}                	
					}
				});
                blocksToUpdate.clear();
                                 
                // mark for update the chunks that contain dirty blocks
                chunksToUpdate.parallelStream().forEach(new Consumer<BlockPos>() {
					@Override
					public void accept(BlockPos chunkPos) {
						int x = chunkPos.getX() << 4;
						int y = chunkPos.getY() << 4;
						int z = chunkPos.getZ() << 4;
						x++;
						y++;
						z++;
						// Marks surrounding blocks too!
	    				ClientProxy.mcinstance.renderGlobal.markBlockRangeForRenderUpdate(x, y, z, x, y, z);      
					}
				});
                chunksToUpdate.clear();

        		forceUpdate = false;
                
                SEL.lastLightUpdateTime = System.currentTimeMillis();
            } else {
            	if (!SEL.disabled) {
            		ticksSkippedCount++;
            	}
            }

        }
//        SEL.mcProfiler.endSection();
    }
	
	// We used to use ForgeEvents, but something was causing concurrency issues!
//	@SideOnly(Side.CLIENT)
//	@SubscribeEvent
//    public void onBlockBreak(BlockEvent.BreakEvent event) {
//		FMLEventHandler.blocksToUpdate.addAll(LightUtils.getVolumeForRelight(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), 8));
//	}
//
//	@SideOnly(Side.CLIENT)
//	@SubscribeEvent
//    public void onBlockPlace(BlockEvent.PlaceEvent event) {
//		FMLEventHandler.blocksToUpdate.addAll(LightUtils.getVolumeForRelight(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), 8));
//	}

    @SubscribeEvent
	@SideOnly(Side.CLIENT)
    public void onChunkLoad(ChunkEvent.Load event)
    {
    	if (event.getWorld() instanceof WorldSEL && SEL.enabledForDimension(ClientProxy.mcinstance.player.dimension)) {
    		ChunkPos chunkPos = event.getChunk().getPos();
    		if (!LightUtils.lightCache.containsKey(chunkPos)) {
    			LightUtils.lightCache.put(chunkPos, new LightCache());					
    		}
    	}
    }

    @SubscribeEvent
	@SideOnly(Side.CLIENT)
    public void onChunkUnload(ChunkEvent.Unload event)
    {
    	if(event.getWorld() instanceof WorldSEL) {
    		LightUtils.lightCache.remove(event.getChunk().getPos());		
    	}
    }

    @SubscribeEvent
    public void onDebugOverlay(RenderGameOverlayEvent.Text event)
    {    	
        if(Minecraft.getMinecraft().gameSettings.showDebugInfo)
        {
    		//There used to be some interesting stats to look at!
    		event.getLeft().add("DL " + (SEL.disabled ? "OFF" : "ON"));
    		// Light levels
    		Entity player = Minecraft.getMinecraft().player;
    		World world = Minecraft.getMinecraft().world;
    		BlockPos pos = player.getPosition();
    		IBlockState state = world.getBlockState(pos);
    		ChunkPos coords = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    		event.getLeft().add(
    			"Vanilla BL: " + state.getLightValue(world, pos) 
    			+ " SEL: " 
    			+ (
    				LightUtils.lightCache != null 
    				&& LightUtils.lightCache.get(coords) != null 
    				?	
    					Math.round(10f * LightUtils.getEntityLightLevel(world, pos)) / 10f
    					+ " Cached: " + Math.round(10f * LightUtils.lightCache.get(coords).lights[pos.getX() & 15][Math.max(0, pos.getY())][pos.getZ() & 15]) / 10f
	    			:
	    				"Disabled for this dimension"
	    		)
    		);
    		event.getLeft().add(
    			"SEL avg blocks re-lit: " + Math.round(10f * EventHandler.totalBlockCount() / EventHandler.counts.size()) / 10f
    			+ " skipped ticks: " + EventHandler.ticksSkippedCount
    		);
        }
    } 
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        Entity entity = event.getObject();
        if(entity == null || !entity.isEntityAlive())
            return;

        World world = entity.getEntityWorld();
        if(world == null || !world.isRemote)
            return;

        // Don't even add light sources to Entities in blacklisted dimensions
        if (!SEL.enabledForDimension(entity.dimension)) {
        	return;
        }
        
        DefaultLightSourceCapability sources = new DefaultLightSourceCapability();
        sources.init(entity, world);

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
            
            if (Config.lightChargingCreepers && entity instanceof EntityCreeper) {
            	CreeperAdaptor adaptor = new CreeperAdaptor((EntityCreeper)entity);
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
        else if (entity instanceof EntityPlayerSP)
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
        else if (entity instanceof FloodLightAdaptor.DummyEntity)
        {
            if (Config.lightFloodLight)
            {
            	PartialLightAdaptor adaptor = new PartialLightAdaptor(entity);
                sources.addLightSource(adaptor);        		    			
            }
        }
        else
        {
            //Do nothing
        }
        
        if (sources.hasSources()) {
            event.addCapability(SEL.LIGHT_SOURCE_CAPABILITY_NAME, sources);
        }

    }
    
    private void checkForOptifine() 
    {
        if (FMLClientHandler.instance().hasOptifine() && !Config.optifineOverride)
        {
            ClientProxy.mcinstance.ingameGUI.getChatGUI().printChatMessage(new TextComponentString("Optifine is loaded.  Disabling Atomic Stryker's Dynamic Lights.  Check the config file to override."));         
            SEL.disabled = true;
        }
    }
	
}
