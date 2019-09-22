package lakmoore.sel.client;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lakmoore.sel.world.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.ChunkCoordIntPair;

public class FMLEventHandler {
	
	private static boolean forceUpdate = false;
	
    /*
     * Number of milliseconds between dynamic light updates
     */
    public static int updateInterval = 40;

	public static LinkedList<Integer> counts = new LinkedList<Integer>();
	public static int tickCount = 0;
	
	public static Set<BlockPos> blocksToUpdate = ConcurrentHashMap.newKeySet();
	public static Set<BlockPos> chunksToUpdate;
	
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
        	&& ClientProxy.mcinstance.theWorld != null
        	&& SEL.enabledForDimension(ClientProxy.mcinstance.thePlayer.dimension)
        ) {
        	
    		//Check for global lights key press
            if (
            	ClientProxy.mcinstance.currentScreen == null 
            	&& ClientProxy.toggleButton.getIsKeyPressed()
                && System.currentTimeMillis() >= ClientProxy.nextKeyTriggerTime
                && !forceUpdate
            ) {
        		//key-repeat delay
                ClientProxy.nextKeyTriggerTime = System.currentTimeMillis() + 1000l;
                //toggle the setting
                SEL.disabled = !SEL.disabled;
                //player notification
                ClientProxy.mcinstance.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(
                        "Smooth Entity Lights " + (SEL.disabled ? "off" : "on")));
                forceUpdate = true;
                
            }

            //check every loaded entity and update any that have light sources
            if (
            	forceUpdate 
            	|| (!SEL.disabled && (System.currentTimeMillis() - SEL.lastLightUpdateTime > updateInterval))
            ) {

                // Tick all entities
                @SuppressWarnings("unchecked")
				List<Entity> allEntities = ClientProxy.mcinstance.theWorld.loadedEntityList;
                
            	allEntities.parallelStream().forEach(new Consumer<Entity>() {
					@Override
					public void accept(Entity entity) {
					    SELSourceContainer sources = (SELSourceContainer)entity.getExtendedProperties(SEL.modId);                		
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
                
                chunksToUpdate = ConcurrentHashMap.newKeySet();

                // update all blocks that have been marked dirty since last tick
                blocksToUpdate.parallelStream().forEach(new Consumer<BlockPos>() {
					@Override
					public void accept(BlockPos pos) {
						int x = pos.getX() >> 4;
						int z = pos.getZ() >> 4;
						LightCache lc = LightUtils.lightCache.get(new ChunkCoordIntPair(x, z));
						if (lc != null) {
							int y = pos.getY();
							if (y < 0) y = 0;
					        lc.lights[pos.getX() & 15][y][pos.getZ() & 15] = LightUtils.getEntityLightLevel(ClientProxy.mcinstance.theWorld, pos.getX(), y, pos.getZ());
							chunksToUpdate.add(new BlockPos(x, pos.getY() >> 4, z));
						}                	
					}
				});
                blocksToUpdate.clear();
                                 
                // mark for update the chunks that contain dirty blocks
                chunksToUpdate.parallelStream().forEach(new Consumer<BlockPos>() {
					@Override
					public void accept(BlockPos chunkCoords) {
						int x = chunkCoords.getX() << 4;
						int y = chunkCoords.getY() << 4;
						int z = chunkCoords.getZ() << 4;
						x++;
						y++;
						z++;
	    				ClientProxy.mcinstance.renderGlobal.markBlockForUpdate(x, y, z);                							
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
