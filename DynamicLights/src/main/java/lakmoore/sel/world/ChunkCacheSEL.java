package lakmoore.sel.world;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lakmoore.sel.client.LightCache;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public class ChunkCacheSEL extends ChunkCache {

	public ChunkCacheSEL(
		World world, 
		int xMin, 
		int yMin, 
		int zMin, 
		int xMax, 
		int yMax, 
		int zMax, 
		int diffSize
	) {
        super(world, xMin, yMin, zMin, xMax, yMax, zMax, diffSize);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
//		SEL.mcProfiler.startSection(SEL.modId + ":getLightBrightness");
		
		if (y < 0) {
			y = 0;
		}

		int light = super.getLightBrightnessForSkyBlocks(x, y, z, lightValue);
		// light is of the form: XXXXXXXX00000000YYYYYYYY
		// where:
		// X = SkyLight (ignoring time of day!)
		// Y = BlockLight
		if (
        	!SEL.disabled   							// Lights are not disabled
        	&& (light & 0xF0) < 0xF0					// Block light is not already at max
        	&& !this.getBlock(x, y, z).isOpaqueCube()	// Block needs lighting
//        	&& SEL.enabledForDimension(Minecraft.getMinecraft().thePlayer.dimension)
        ) {  
			LightCache lc = LightUtils.lightCache.get(new ChunkCoordIntPair(x >> 4, z >> 4));
			if (lc != null) {
	            float lightPlayer = lc.lights[x & 15][y][z & 15];
	            light = LightUtils.getCombinedLight(lightPlayer, light);
			}					
        }	
		
//		SEL.mcProfiler.endSection();
        return light;
    }

}
