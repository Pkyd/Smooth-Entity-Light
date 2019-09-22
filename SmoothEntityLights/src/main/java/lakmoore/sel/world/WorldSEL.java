package lakmoore.sel.world;

import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.client.EventHandler;
import lakmoore.sel.client.LightCache;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

public abstract class WorldSEL extends World {
		
	public WorldSEL(
		ISaveHandler saveHandler, 
		WorldInfo info, 
		WorldProvider provider, 
		Profiler profiler, 
		boolean client
	) {
		super(saveHandler, info, provider, profiler, client);
	}
	
	@Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
//		SEL.mcProfiler.startSection(SEL.modId + ":getLightBrightness");
						
		int light = super.getCombinedLight(pos, lightValue);
		// light is of the form: XXXXXXXX00000000YYYYYYYY
		// where:
		// X = SkyLight (ignoring time of day!)
		// Y = BlockLight
		if (
        	!SEL.disabled   							// Lights are not disabled
        	&& (light & 0xF0) < 0xF0					// Block light is not already at max
        	&& !this.getBlockState(pos).isOpaqueCube()	// Block needs lighting
//        	&& SEL.enabledForDimension(Minecraft.getMinecraft().thePlayer.dimension)
        ) {  
			LightCache lc = LightUtils.lightCache.get(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
			if (lc != null) {
				int y = pos.getY();
				if (y < 0) {
					y = 0;
				}
	            float lightPlayer = lc.lights[pos.getX() & 15][y][pos.getZ() & 15];
	            light = LightUtils.getCombinedLight(lightPlayer, light);
			}
        }
		
//		SEL.mcProfiler.endSection();
        return light;
    }
	
	@Override
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
		EventHandler.blocksToUpdate.addAll(LightUtils.getVolumeForRelight(pos, 8));		
		return super.setBlockState(pos, newState, flags);    	
    }
	
	@Override
    public void removeEntity(Entity entity)
    {
		ILightSourceCapability sources = entity.getCapability(SEL.LIGHT_SOURCE_CAPABILITY, null);                		
        if (sources != null)
    		sources.destroy();
        
		super.removeEntity(entity);
    }

}
