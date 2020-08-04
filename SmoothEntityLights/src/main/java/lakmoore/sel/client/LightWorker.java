package lakmoore.sel.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.capabilities.ILitChunkCache;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class LightWorker extends Thread {
	// purely internal properties
	private final Set<ILightSourceCapability> allSources = ConcurrentHashMap.newKeySet();
	private final Set<BlockPos> blocksToUpdate = ConcurrentHashMap.newKeySet();
	/*
	 * Target number of milliseconds between entity light updates
	 */
	private int updateInterval = 20; // 20 => 50 updates per second
    private long lastLightUpdateTime;

	// properties that will be updated from the game event(s)
	private volatile Entity player;
	private volatile ICamera iCamera;
	private volatile ViewFrustum frustum;
	private volatile Vec3d cameraPos;
	private volatile float maxDistSq;
	private volatile float partialTicks;
	private volatile short state = 0;
	
	// counters
	public AtomicInteger entityCount = new AtomicInteger();
	public ConcurrentLinkedQueue<Integer> counts = new ConcurrentLinkedQueue<Integer>();
	public int ticksSkippedCount = 0;

	// Ouput
	public Set<ILitChunkCache> chunksToBeReRendered = ConcurrentHashMap.newKeySet();

	LightWorker() {
		this.setName("SEL Light Worker");
        this.lastLightUpdateTime = System.currentTimeMillis();
	}

	public int totalBlockCount() {
		int count = 0;
		for (Object i : this.counts.toArray()) {
			count += (int) i;
		}
		return count;
	}

	public int averageBlockCount() {
		int size = 0; 
		int count = 0;
		for (Object i : this.counts.toArray()) {
			count += (int) i;
			size += 1;
		}
		if (size > 0) {
			return count / size;			
		} else {
			return 0;
		}
	}

	public void setPlayer(Entity player) {
		this.player = player;		
	}
	
	public void addSourceEntity(ILightSourceCapability source) {
		this.allSources.add(source);
	}

	public void removeSourceEntity(ILightSourceCapability source) {
		this.allSources.remove(source);
		this.blocksToUpdate.addAll(source.getBlocksToUpdate());
	}
	
	public void updateCamera(ICamera camera, float partialTicks, int renderDistanceChunks) {
		this.iCamera = camera;
		this.partialTicks = partialTicks;
		this.cameraPos = this.player.getEyePosition(partialTicks);
		this.iCamera.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
		this.maxDistSq = renderDistanceChunks * 0.75f * renderDistanceChunks * 0.75f * 256.0f;
	}
	
	public void updateFrustum(ViewFrustum frustum) {
		if (this.frustum != frustum) {
			this.frustum = frustum;			
		}
	}
	
	public void shutdown() {
		this.state = 1;
	}

	public void restart() {
		this.state = 0;
	}
	
	public boolean isShutdown() {
		return this.state == 3;
	}

	public void reLightAVolume(BlockPos center, int radius) {
		this.blocksToUpdate.addAll(LightUtils.getVolumeForRelight(center, radius));
	}

	public void run() {
				
		while (state > -1) {
			
			if (
				this.state < 3
				&& this.player != null
				&& this.player.world != null
				&& (ClientProxy.mcinstance.currentScreen == null
					|| !ClientProxy.mcinstance.currentScreen.doesGuiPauseGame()
				) && SEL.enabledForDimension(this.player.dimension)
			) {
				this.entityCount.set(0);
				
				if (this.player != null && this.cameraPos != null) {
					// Search for entities of interest
					Set<ILightSourceCapability> interestingSources = allSources.stream().filter(source -> {
						// Is the player, or being ridden by the player
						if (source.getEntity().equals(ClientProxy.mcinstance.player)
								|| source.getEntity().isRidingOrBeingRiddenBy(ClientProxy.mcinstance.player)) {
							return true;
						}
						// OR
						// Is not further away than 75% of the chunk render distance
						if (source.getEntity().getPosition().distanceSq(new BlockPos(this.cameraPos)) > this.maxDistSq) {
							return false;
						}
						// OR
						// Lit volume is at least partially inside the Frustum
						AxisAlignedBB axisalignedbb = source.getEntity().getRenderBoundingBox().grow(SEL.maxLightDist);
						return this.iCamera.isBoundingBoxInFrustum(axisalignedbb);
					}).collect(Collectors.toSet());
					
					// Tick those interesting entities and allow them to mark new dirty blocks
					
					
					this.blocksToUpdate.addAll(interestingSources.stream().flatMap(source -> {
						this.entityCount.addAndGet(1);
						// getBlocksToUpdate ticks the source container and returns a list of dirty blocks
						return source.getBlocksToUpdate().stream();
					})
					.collect(Collectors.toSet()));
										
					AtomicInteger count = new AtomicInteger(0);
					
					this.blocksToUpdate.forEach(blockPos -> {
						if (blockPos != null) {				
							this.blocksToUpdate.remove(blockPos);				

							// never need to light Opaque blocks
							if(!this.player.world.getBlockState(blockPos).isOpaqueCube(this.player.world, blockPos)) {
								// TODO: Test if culling block with no line of sight to camera is faster
								ILitChunkCache litChunk = LightUtils.getLitChunkCache(this.player.world,
										blockPos.getX() >> 4, blockPos.getZ() >> 4);
								if (litChunk != null) {
									litChunk.markBlockDirty(blockPos);
				//					System.out.println("******** Dirty at " + blockPos.toString() + " ********");
									count.incrementAndGet();
								}
							}
						}			
					});


					// Also tried testing for blocks that have no neighbours with textures
					// but that stopped lighting entities when they passed through those un-lit
					// blocks!!!

					// Let's record how much work we are doing
					counts.add(count.intValue());
					while (counts.size() > 60) {
						counts.remove();
					}
					
					if (this.frustum != null) {
						Set<ILitChunkCache> result = new HashSet<ILitChunkCache>();

						// Get all the chunks we are interested in
						Set<RenderChunk> renderChunks = Arrays.asList(this.frustum.renderChunks).stream()
								.filter(renderChunk -> renderChunk != null
									// AND this chunk is visible
//									&& this.iCamera.isBoundingBoxInFrustum(renderChunk.boundingBox)
								)
								.collect(Collectors.toSet());

						// re-light all dirty blocks in those chunks
						renderChunks.forEach(renderChunk -> {
							ILitChunkCache lc = LightUtils.getLitChunkCache(this.player.world,
									renderChunk.getPosition().getX() >> 4, renderChunk.getPosition().getZ() >> 4);
							int yChunk = renderChunk.getPosition().getY() >> 4;
							if (lc != null && lc.hasDirtyBlocks(yChunk)) {
								// Get nearby entities w.r.t. this chunk
								List<Entity> nearEntities = this.player.world.getEntitiesWithinAABB(
										Entity.class, renderChunk.boundingBox.grow(SEL.maxLightDist),
										LightUtils.HAS_ENTITY_LIGHT);

								// Re light ALL dirty blocks/vertices in this chunk
								lc.reLight(yChunk, nearEntities, this.partialTicks);
							
								// Cache the bits we need for later
								lc.setRenderChunk(yChunk, renderChunk);

								// batch up the results
								result.add(lc);
							}
						});

						// Add the batch to the queue for render update in main thread
						this.chunksToBeReRendered.addAll(result);

					}	
				}				
			} else {
				try {
					Thread.sleep(700);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			try {
				long sleepTime = Math.max(0, this.updateInterval - (System.currentTimeMillis() - this.lastLightUpdateTime)); 
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			this.lastLightUpdateTime = System.currentTimeMillis();
			
			if (this.state > 0 && this.state < 3) {
				this.state ++;
			}

		}

    }

}
