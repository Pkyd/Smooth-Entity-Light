package lakmoore.sel.world;

import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
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
        	&& (light & 0xFF) < 0xF0					// Block light is not already at max
        	&& !this.getBlockState(pos).isOpaqueCube()	// Block needs lighting
//        	&& SEL.enabledForDimension(Minecraft.getMinecraft().thePlayer.dimension)
        ) {  
			ILitChunkCache lc = LightUtils.getLitChunkCache(this, pos.getX() >> 4, pos.getZ() >> 4);
			if (lc != null) {
				// This is needed to light entities in the world with entity light
				short lightPlayer = lc.getBlockLight(pos.getX(), pos.getY(), pos.getZ());
	            light = LightUtils.getCombinedLight(lightPlayer, light);
			}					
        }
		
//		SEL.mcProfiler.endSection();
        return light;
    }
	
	@Override
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
		// If the change is supposed to cause a re-render 
		if ((flags & 0x4) == 0) {
			// search the immediately surrounding blocks
	        for (BlockPos.MutableBlockPos nearPos : BlockPos.getAllInBoxMutable(pos.add(-1, -1, -1), pos.add(1, 1, 1)))
	        {	        	
	        	// for ANY entity light
				ILitChunkCache lc = LightUtils.getLitChunkCache(this, nearPos.getX() >> 4, nearPos.getZ() >> 4);
				if (lc != null) {
					short lightPlayer = lc.getBlockLight(nearPos.getX(), nearPos.getY(), nearPos.getZ());

					if (lightPlayer > 0) {
		        		// as soon as we see some, mark ALL the surrounding blocks in our radius as "dirty"
						SEL.lightWorker.reLightAVolume(pos, SEL.maxLightDist);
						// stop our search
						break;	        		
					}
	        	}
	        }					
		}
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
