package lakmoore.sel.world;

import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;

public class ChunkCacheSEL extends ChunkCache {
	
	World world;

	public ChunkCacheSEL(
		World world, 
		BlockPos posFrom,
		BlockPos posTo,
		int diffSize
	) {
	    super(world, posFrom, posTo, diffSize);
	    this.world = world;
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
			ILitChunkCache lc = LightUtils.getLitChunkCache(world, pos.getX() >> 4, pos.getZ() >> 4);
			if (lc != null) {
//				short lightPlayer = lc.getBlockLight(pos.getX(), pos.getY(), pos.getZ());
//	            light = LightUtils.getCombinedLight(lightPlayer, light);
			}					
        }	
		
//		SEL.mcProfiler.endSection();
        return light;
    }

}
