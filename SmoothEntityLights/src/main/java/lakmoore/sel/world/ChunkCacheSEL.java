package lakmoore.sel.world;

import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.client.ClientProxy;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;
import net.minecraft.client.renderer.chunk.RenderChunkCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class ChunkCacheSEL extends RenderChunkCache {
	
	World world;
	
	public ChunkCacheSEL(
		World worldIn, 
		int chunkStartXIn, 
		int chunkStartZIn, 
		Chunk[][] chunksIn, 
		BlockPos startPos, 
		BlockPos endPos		
	) {
	    super(worldIn, chunkStartXIn, chunkStartZIn, chunksIn, startPos, endPos);

	    this.world = worldIn;
	}
	
	@Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
//		SEL.mcProfiler.startSection(SEL.modId + ":getLightBrightness");
		
		// nasty hack to avoid re-writing the Fluid Renderer!
		int thisLight = this.getBlockState(pos).getLightValue();
		if (thisLight > 0) {
			for (int x = 0; x < 2; x++) {
				for (int y = 0; y < 2; y++) {
					for (int z = 0; z < 2; z++) {
						BlockPos blockPos = pos.add(x, y, z);
						ILitChunkCache lcc = LightUtils.getLitChunkCache(ClientProxy.mcinstance.world, blockPos.getX() >> 4, blockPos.getZ() >> 4);
						lcc.setMCVertexLight(blockPos.getX(), blockPos.getY(), blockPos.getZ(), (short)(16f * thisLight));
					}
				}
			}
		}

		int light = super.getCombinedLight(pos, lightValue);
		// light is of the form: XXXXXXXX00000000YYYYYYYY
		// where:
		// X = SkyLight (ignoring time of day!)
		// Y = BlockLight
		if (
        	!SEL.disabled   							// Lights are not disabled
        	&& (light & 0xFF) < 0xF0					// Block light is not already at max
        	&& !this.getBlockState(pos).isOpaqueCube((IBlockReader)this, pos)	// Block needs lighting
//        	&& SEL.enabledForDimension(Minecraft.getMinecraft().thePlayer.dimension)
        ) {  
			ILitChunkCache lc = LightUtils.getLitChunkCache(world, pos.getX() >> 4, pos.getZ() >> 4);
			if (lc != null) {
				// If we add this in then our SELLight gets baked in to the chunks = BAD!
//				short lightPlayer = lc.getBlockLight(pos.getX(), pos.getY(), pos.getZ());
//	            light = LightUtils.getCombinedLight(lightPlayer, light);
			}	

        }	
		
//		SEL.mcProfiler.endSection();
        return light;
    }

}
