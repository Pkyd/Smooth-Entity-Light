package lakmoore.sel.capabilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lakmoore.sel.client.ClientProxy;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class LitChunkCacheCapability implements ICapabilityProvider, ILitChunkCache {

	private short[][][][] blockLight;
	private short[][][][] selLight;
	private short[][][][] mcLight;
	private List<Set<BlockPos>> dirtyBlocks;
	private boolean[] dirtyChunk;
	private Chunk chunk;
	private RenderChunk[] renderChunks;

	public LitChunkCacheCapability() {
		blockLight = new short[16][16][16][16];
		selLight = new short[16][16][16][16];
		mcLight = new short[16][16][16][16];
		dirtyBlocks = new ArrayList<Set<BlockPos>>(16);
		for (int i = 0; i < 16; i++) {
			dirtyBlocks.add(ConcurrentHashMap.newKeySet());
		}
		dirtyChunk = new boolean[16];
		renderChunks = new RenderChunk[16];
	}
	
	@Override
	public void setChunk(Chunk chunk) {
		this.chunk = chunk;
	}
	
	@Override
	public Chunk getChunk() {
		return this.chunk;
	}
	
	@Override
	public void setRenderChunk(int yChunk, RenderChunk renderChunk) {
		this.renderChunks[yChunk & 0xF] = renderChunk;
	}

	@Override
	public Set<Integer> getDirtyRenderChunkYs() {
		Set<Integer> result = ConcurrentHashMap.newKeySet();
		for (int y = 0; y < 16; y++) {
			if (this.needsReLight(y)) {
				result.add(y);
			}
		}
		return result;
	}

	@Override
	public RenderChunk getRenderChunk(int yChunk) {
		return this.renderChunks[yChunk & 0xF];
	}

	@Override
	public void setBlockLight(int x, int y, int z, short light) {
		if (y < 0)
			y = 0;
		blockLight[(y >> 4) & 0xF][x & 0xF][y & 0xF][z & 0xF] = light;
	}

	@Override
	public void setVertexLight(int x, int y, int z, short light) {
		if (y < 0)
			y = 0;
		selLight[(y >> 4) & 0xF][x & 0xF][y & 0xF][z & 0xF] = light;	
	}

	@Override
	public void setMCVertexLight(double x, double y, double z, short light) {
		
		int intX = (int)Math.round(x);
		int intY = (int)Math.round(y);
		int intZ = (int)Math.round(z);
		float err = 1f / 32f;

		// only save the light value if the position is close enough to a vertex
		if (Math.abs(x - intX) < err &&
			Math.abs(y - intY) < err &&
			Math.abs(z - intZ) < err				
		) {
			if (intY < 0)
				intY = 0;
			mcLight[(intY >> 4) & 0xF][intX & 0xF][intY & 0xF][intZ & 0xF] = light;
		}
		
	}

	@Override
	public short getBlockLight(int x, int y, int z) {
		if (y < 0)
			y = 0;
		if (y > 255)
			y = 255;
		return blockLight[(y >> 4) & 0xF][x & 0xF][y & 0xF][z & 0xF];
	}

	@Override
	public short getVertexLight(double x, double y, double z) {
		if (y < 0f)
			y = 0f;
		if (y > 255f)
			y = 255f;

		int intX = (int)Math.round(x);
		int intY = (int)Math.round(y);
		int intZ = (int)Math.round(z);

		short mc = mcLight[(intY >> 4) & 0xF][intX & 0xF][intY & 0xF][intZ & 0xF];
		short sel = selLight[(intY >> 4) & 0xF][intX & 0xF][intY & 0xF][intZ & 0xF];
		return (short) Math.min(0xF0, Math.max(mc, sel));		
	}

	@Override
	public void markBlockDirty(BlockPos pos) {
		if (pos.getY() > -1 && pos.getY() < 256) {
			int yChunk = (pos.getY() >> 4) & 0xF;
			dirtyBlocks.get(yChunk).add(pos);
			dirtyChunk[yChunk] = true;			
			if ((pos.getY() & 0xF) == 0 && yChunk > 0) {
				yChunk--;
				dirtyBlocks.get(yChunk).add(pos.add(0, -1, 0));
				dirtyChunk[yChunk] = true;			
			}
			if ((pos.getY() & 0xF) == 0xF && yChunk < 0xF) {
				yChunk++;
				dirtyBlocks.get(yChunk).add(pos.add(0, 1, 0));
				dirtyChunk[yChunk] = true;			
			}
			
		}
	}

	@Override
	public Set<BlockPos> getDirtyBlocks(int yChunk) {
		return dirtyBlocks.get(yChunk);
	}

	@Override
	public boolean hasDirtyBlocks(int yChunk) {
		return dirtyBlocks.get(yChunk).size() > 0;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == SEL.LIT_CHUNK_CACHE_CAPABILITY;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == SEL.LIT_CHUNK_CACHE_CAPABILITY) {
			return SEL.LIT_CHUNK_CACHE_CAPABILITY.cast(this);
		}
		return null;
	}

	@Override
	public boolean needsReLight(int yChunk) {
		return dirtyChunk[yChunk];
	}

	@Override
	public void reLightDone(int yChunk) {
		dirtyChunk[yChunk] = false;
	}

	@Override
	public void reLight(int yChunk, List<Entity> nearbyEntities, float partialTicks) {
		if (!this.hasDirtyBlocks(yChunk)) {
			return;
		}

		HashSet<BlockPos> dirtyVerticesLocal = new HashSet<BlockPos>();
		HashSet<BlockPos> dirtyVertices = new HashSet<BlockPos>();

		this.dirtyBlocks.get(yChunk).forEach(blockPos -> {
			// Add all 8 vertices to a set
			for (int x = 0; x < 2; x++) {
				for (int y = 0; y < 2; y++) {
					for (int z = 0; z < 2; z++) {
						BlockPos vertexPos = blockPos.add(x, y, z);
						if ((blockPos.getX() & 0xF) != 0xF && (blockPos.getY() & 0xF) != 0xF && (blockPos.getZ() & 0xF) != 0xF) {
							dirtyVerticesLocal.add(vertexPos);
						} else {
							dirtyVertices.add(vertexPos);
						}
					}
				}			
			}
			
			// refresh the BlockLight once per dirty block
			this.setBlockLight(blockPos.getX(), blockPos.getY(), blockPos.getZ(), LightUtils.getEntityLightLevel(
					ClientProxy.mcinstance.world, nearbyEntities, new Vec3d(blockPos).add(0.5f, 0.5f, 0.5f), partialTicks));			
		});
		
		dirtyVerticesLocal.forEach(vertexPos -> {
			// refresh the light at each unique dirty vertex in this chunk
			this.setVertexLight(vertexPos.getX(), vertexPos.getY(), vertexPos.getZ(), LightUtils
					.getEntityLightLevel(ClientProxy.mcinstance.world, nearbyEntities, new Vec3d(vertexPos), partialTicks));				
		});

		dirtyVertices.forEach(vertexPos -> {
			// refresh the light at each unique dirty vertex, looking up the chunk each time
			ILitChunkCache lcc = LightUtils.getLitChunkCache(ClientProxy.mcinstance.world, vertexPos.getX() >> 4, vertexPos.getZ() >> 4);
			lcc.setVertexLight(vertexPos.getX(), vertexPos.getY(), vertexPos.getZ(), LightUtils
					.getEntityLightLevel(ClientProxy.mcinstance.world, nearbyEntities, new Vec3d(vertexPos), partialTicks));				
		});

		this.getDirtyBlocks(yChunk).clear();
	}

}
