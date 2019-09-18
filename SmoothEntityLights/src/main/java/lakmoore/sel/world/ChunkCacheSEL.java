package lakmoore.sel.world;

import lakmoore.sel.client.LightCache;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ChunkCacheSEL extends ChunkCache {

	public ChunkCacheSEL(
		World world, 
		BlockPos posFrom,
		BlockPos posTo,
		int diffSize
	) {
	    super(world, posFrom, posTo, diffSize);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
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

}
