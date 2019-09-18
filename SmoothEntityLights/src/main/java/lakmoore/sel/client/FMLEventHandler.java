package lakmoore.sel.client;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import lakmoore.sel.capabilities.ILightSourceCapability;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class FMLEventHandler {
	
	private static boolean forceUpdate = false;

	public static LinkedList<Integer> counts = new LinkedList<Integer>();
	public static int tickCount = 0;
	
	public static HashSet<BlockPos> blocksToUpdate = new HashSet<BlockPos>();
	public static HashSet<ChunkPos> chunksToUpdate = new HashSet<ChunkPos>();

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

            //check every loaded entity and update any that have light sources
            if (forceUpdate || !SEL.disabled) {

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
                
                chunksToUpdate = new HashSet<ChunkPos>();

                // update all blocks that have been marked dirty since last tick
                blocksToUpdate.parallelStream().forEach(new Consumer<BlockPos>() {
					@Override
					public void accept(BlockPos pos) {
						ChunkPos chunkCoords = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
						chunksToUpdate.add(chunkCoords);
						LightCache lc = LightUtils.lightCache.get(chunkCoords);
						if (lc != null) {
							int y = pos.getY();
							if (y < 0) y = 0;
					        lc.lights[pos.getX() & 15][y][pos.getZ() & 15] = LightUtils.getEntityLightLevel(ClientProxy.mcinstance.world, pos);
						}                	
					}
				});
                blocksToUpdate.clear();
                                 
                // mark for update the chunks that contain dirty blocks
                chunksToUpdate.parallelStream().forEach(new Consumer<ChunkPos>() {
					@Override
					public void accept(ChunkPos chunkPos) {
						int x = chunkPos.x << 4;
						int z = chunkPos.z << 4;
						x++;
						z++;
						// Marks surrounding blocks too!
	    				ClientProxy.mcinstance.renderGlobal.markBlockRangeForRenderUpdate(x, 1, z, x, 1, z);                							
					}
				});
                chunksToUpdate.clear();

        		forceUpdate = false;
                
                SEL.lastLightUpdateTime = System.currentTimeMillis();
            }

        }
//        SEL.mcProfiler.endSection();
    }
	
}
