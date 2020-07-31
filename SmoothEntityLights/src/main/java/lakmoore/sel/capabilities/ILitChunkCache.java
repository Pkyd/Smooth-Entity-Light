package lakmoore.sel.capabilities;

import java.util.List;
import java.util.Set;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

public interface ILitChunkCache {
	
	/*
	 * A 3D array of entity light values
	 * 3 dimensions are x, y, z
	 * locations match the blocks in this chunk
	 * therefore each dimension has a range of 0 to 15
	 * 
	 * yChunk value is also between 0 and 15
	 */
	
	public void setChunk(Chunk chunk);
	public Chunk getChunk();
	
	public void setRenderChunk(int yChunk, RenderChunk renderChunk);
	public Set<Integer> getDirtyRenderChunkYs();
	public RenderChunk getRenderChunk(int yChunk);
	
	/*
	 * set the block light value in the cache
	 */
	public void setBlockLight(int x, int y, int z, short light);

	/*
	 * set the vertex light value in the cache
	 */
	public void setVertexLight(int x, int y, int z, short light);

	/*
	 * set the vanilla light value in the cache
	 */
	public void setMCVertexLight(double x, double y, double z, short light);

	/*
	 * get the SEL light value from the cache (ignoring MC Light values)
	 */
	public short getBlockLight(int x, int y, int z);

	/*
	 * get the light value from the cache
	 */
	public short getVertexLight(double x, double y, double z);

	/*
	 * A list of BlockPos in this chunk that need re-lighting 
	 * before they are OK to display again
	 */
	public Set<BlockPos> getDirtyBlocks(int yChunk);

	/*
	 * A list of BlockPos in this chunk that need re-lighting 
	 * before they are OK to display again
	 */
	public boolean hasDirtyBlocks(int yChunk);
	
	/*
	 * add a BlockPos to the set of dirty blocks
	 */
	public void markBlockDirty(BlockPos pos);

	/*
	 * returns true if we have have contained dirty blocks since last frame
	 */
	public boolean needsReLight(int yChunk);

	/*
	 * Re calculates the light value for all dirty blocks
	 * does not clear the re-lit flag
	 */
	public void reLight(int yChunk, List<Entity> nearbyEntities, float partialTicks);

	/*
	 * Update to record that this chunk has been re-lit
	 */
	public void reLightDone(int yChunk);
	
}
