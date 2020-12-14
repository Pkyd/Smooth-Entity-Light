package lakmoore.sel.capabilities;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

public class DummyChunkCache implements ILitChunkCache {

	@Override
	public void setChunkPos(int x, int z) {
		// Do Nothing
	}

	@Override
	public int getX() {
		return 0;
	}

	@Override
	public int getZ() {
		return 0;
	}

	@Override
	public void setRenderChunk(int yChunk, ChunkRenderDispatcher.ChunkRender renderChunk) {
		// Do Nothing
	}

	@Override
	public Set<Integer> getDirtyRenderChunkYs() {
		return new HashSet<Integer>();
	}

	@Override
	public ChunkRenderDispatcher.ChunkRender getRenderChunk(int yChunk) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBlockLight(int x, int y, int z, short light) {
		// Do Nothing
	}

	@Override
	public void setVertexLight(int x, int y, int z, short light) {
		// Do Nothing
	}

	@Override
	public void setMCVertexLight(double x, double y, double z, short light) {
		// Do Nothing
	}

	@Override
	public short getBlockLight(int x, int y, int z) {
		return 0;
	}

	@Override
	public short getVertexLight(double x, double y, double z) {
		return 0;
	}

	@Override
	public Set<BlockPos> getDirtyBlocks(int yChunk) {
		return new HashSet<BlockPos>();
	}

	@Override
	public boolean hasDirtyBlocks(int yChunk) {
		return false;
	}

	@Override
	public void markBlockDirty(BlockPos pos) {
		// Do Nothing
	}

	@Override
	public boolean needsReLight(int yChunk) {
		return false;
	}

	@Override
	public void reLight(int yChunk, List<Entity> nearbyEntities, float partialTicks) {
		// Do Nothing
	}

	@Override
	public void reLightDone(int yChunk) {
		// Do Nothing
	}

}
