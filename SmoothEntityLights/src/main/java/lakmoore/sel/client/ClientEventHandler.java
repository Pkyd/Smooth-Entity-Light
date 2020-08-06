package lakmoore.sel.client;

import java.nio.IntBuffer;

import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import lakmoore.sel.capabilities.DefaultLightSourceCapability;
import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.capabilities.LitChunkCacheCapability;
import lakmoore.sel.client.adaptors.BrightAdaptor;
import lakmoore.sel.client.adaptors.CreeperAdaptor;
import lakmoore.sel.client.adaptors.EntityBurningAdaptor;
import lakmoore.sel.client.adaptors.EntityItemAdaptor;
import lakmoore.sel.client.adaptors.FloodLightAdaptor;
import lakmoore.sel.client.adaptors.MobLightAdaptor;
import lakmoore.sel.client.adaptors.PartialLightAdaptor;
import lakmoore.sel.client.adaptors.PlayerOtherAdaptor;
import lakmoore.sel.client.adaptors.PlayerSelfAdaptor;
import net.minecraft.block.BlockState;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

public class ClientEventHandler {
	
    @SuppressWarnings("unchecked")
    @SubscribeEvent
	public void serverStarting(FMLServerStartingEvent event) {
    	// Setup Commands in this event
	    event.getCommandDispatcher().register((LiteralArgumentBuilder<CommandSource>) CommandSEL.register());
    }
    
    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
    	// Client Thread only please
    	if (event.getWorld().isRemote()) {
            // Set up our main worker thread
    		SEL.lightWorker = new LightWorker();	
    		SEL.lightWorker.start();    		
    	}
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
    	// Client Thread only please
    	if (event.getWorld().isRemote()) {
    		SEL.lightWorker.interrupt();    	
    	}
    }

	@SubscribeEvent
	public void onTick(TickEvent.ClientTickEvent event) {
		if (event.phase == Phase.END && ClientProxy.mcinstance.world != null
				&& SEL.enabledForDimension(ClientProxy.mcinstance.player.dimension)) {

			// Check for global lights key press
			if (ClientProxy.mcinstance.currentScreen == null && ClientProxy.toggleButton.isPressed()
					&& System.currentTimeMillis() >= ClientProxy.nextKeyTriggerTime) {
				// key-repeat delay
				ClientProxy.nextKeyTriggerTime = System.currentTimeMillis() + 1000l;
				// toggle the setting
				SEL.disabled = !SEL.disabled;
				// player notification
				ClientProxy.mcinstance.ingameGUI.getChatGUI().printChatMessage(
						new StringTextComponent("Smooth Entity Lights " + (SEL.disabled ? "off" : "on")));
				if (SEL.disabled) {
					SEL.lightWorker.shutdown();
					SEL.forceUpdate = true;
				} else {
					SEL.lightWorker.restart();
				}
			}
		}
	}

	/*
	 * RenderWorldLastEvent is not a great place to put extra logic, but is the only
	 * event I can find where it is not necessary to re-build the Frustum on the
	 * Camera!
	 */
	@SubscribeEvent
	public void afterRender(RenderWorldLastEvent event) {
		// ClientProxy.mcProfiler.startSection(SEL.modId + ":afterRender");
		if (ClientProxy.mcinstance.world != null
				&& (ClientProxy.mcinstance.currentScreen == null
						|| !ClientProxy.mcinstance.currentScreen.isPauseScreen())
				&& SEL.enabledForDimension(ClientProxy.mcinstance.player.dimension)
				&& (SEL.forceUpdate || !SEL.disabled)) {

			// update the Light Worker with the current Frustum
			updateFrustum();

			// update the Light Worker with current camera position
			SEL.lightWorker.updateCamera(new Frustum(), event.getPartialTicks(),
					ClientProxy.mcinstance.gameSettings.renderDistanceChunks);

			// Process the chunk updates that have been queued since last frame
			SEL.lightWorker.chunksToBeReRendered.forEach(lightCache -> {
				if (lightCache != null) {

					SEL.lightWorker.chunksToBeReRendered.remove(lightCache);

//						ChunkPos chunkPos = lightCache.getChunk().getPos();
//						if (chunkPos.getY()==0) {
//							System.out.println("******** Chunk at " + chunkPos.toString() + " ********");									
//						}
//						if (chunkPos.getY()==0) {
//							System.out.println("******** Chunk at " + chunkPos.toString() + " ********");
//						}

					lightCache.getDirtyRenderChunkYs().forEach(y -> {

						ChunkRender renderChunk = lightCache.getRenderChunk(y);
						if (renderChunk != null) {

							lightCache.reLightDone(y);

							// int stride = 7;
							// Vertex is:
							// 0 <= X (Float - 4 bytes)
							// 1 <= Y (Float - 4 bytes)
							// 2 <= Z (Float - 4 bytes)
							// 3 <= Color RGBA (4 x Unsigned Byte)
							// 4 <= Texture Co-ord "U" (Float - 4 bytes)
							// 5 <= Texture Co-ord "V" (Float - 4 bytes)
							// 6 <= Light level (2 x Short) (also a texture co-ord)
							
							for (BlockRenderLayer layer : BlockRenderLayer.values()) {

								VertexBuffer vbo = renderChunk.getVertexBufferByLayer(layer.ordinal());

								vbo.bindBuffer();
								int byteCount = GL15.glGetBufferParameteri(GLX.GL_ARRAY_BUFFER,
										GL15.GL_BUFFER_SIZE);

								if (byteCount > 0) {
									// int buffers stored as bytes
									int integerCount = byteCount / 4;

									IntBuffer data = BufferUtils.createIntBuffer(integerCount);
									
									int[] rawBuffer = null;
									if (layer == BlockRenderLayer.TRANSLUCENT && renderChunk.getCompiledChunk() != null && renderChunk.getCompiledChunk().getState() != null && renderChunk.getCompiledChunk().getState().getRawBuffer() != null) {
										// minecraft does this janky thing where it re-sorts the vertices in the translucent layer
										// that is not the problem, the problem is that it does it whenever the camera moves
										// further than 1 block from its last position... janky!
										rawBuffer = renderChunk.getCompiledChunk().getState().getRawBuffer();
									} else {
										// Fetch the Vertex Buffer from the GPU
										GL15.glGetBufferSubData(GLX.GL_ARRAY_BUFFER, 0, data);

										rawBuffer = new int[integerCount];
										data.get(rawBuffer);
									}

									if (rawBuffer.length > 0) {
										boolean changed = false;
										int putIndex = 0;
										// System.out.println("******** Chunk at " + chunkPos.toString() + " "
										// + layer.name() + " ********");

										int vertCount = integerCount / 7; // each vertex is 7 integers of data (28
																			// bytes)
										int quadCount = vertCount / 4; // each quad is four vertices (duh!)
										
										for (int q = 0; q < quadCount; q++) {
											int[][] thisQuad = new int[4][];
											float[][] position = new float[4][3];
											boolean quadChanged = false;
											
											for (int v = 0; v < 4; v++) {
												thisQuad[v] = new int[9];
												for(int i = 0; i < 7; i++) {
													thisQuad[v][i] = rawBuffer[putIndex + (v * 7) + i];													
												}
												
												position[v][0] = Float.intBitsToFloat(thisQuad[v][0]);
												position[v][1] = Float.intBitsToFloat(thisQuad[v][1]);
												position[v][2] = Float.intBitsToFloat(thisQuad[v][2]);											
											}
																			        																                							                
											for (int v = 0; v < 4; v++) {
												int yChunk = y;
																			                
												// Vertices range from 0 to 16 (not 0 to 15!!)
												int vertX = Math.round(position[v][0]);
												int vertY = Math.round(position[v][1]);
												int vertZ = Math.round(position[v][2]);

												ILitChunkCache thisLitChunkCache = lightCache;

												if (vertX > 15) {
													vertX -= 16;
													thisLitChunkCache = LightUtils.getLitChunkCache(
															ClientProxy.mcinstance.world, lightCache.getX() + 1,
															lightCache.getZ());
												}

												if (vertY > 15) {
													if (yChunk == 15) {
														vertY = 15;
													} else {
														vertY -= 16;
														yChunk += 1;
													}
												}

												if (vertZ > 15) {
													vertZ -= 16;
													thisLitChunkCache = LightUtils.getLitChunkCache(
															ClientProxy.mcinstance.world, thisLitChunkCache.getX(),
															thisLitChunkCache.getZ() + 1);
												}

												int mcLight = thisQuad[v][6];
												short selLight = thisLitChunkCache.getVertexLight(vertX,
														(16 * yChunk) + vertY, vertZ);											
												int newLight = (mcLight & 0xFFFF0000) | selLight;

												if (mcLight != newLight) {
													thisQuad[v][6] = newLight;
													quadChanged = true;
												}
												thisQuad[v][7] = selLight + ((mcLight & 0xFFFF0000) >> 16);

											}

											if (quadChanged) {
												int[] order = { 0, 1, 2, 3 };
												int[] flipped = { 1, 2, 3, 0 };

												// re-order the triangles in the quad so brightness is always blended
												// smoothly
												if (
														(
															thisQuad[3][7] - thisQuad[0][7]
														)
													<
														(
															thisQuad[2][7] - thisQuad[1][7]
														)
												) {
													order = flipped;
												}

												data.position(putIndex);
												// thisQuad contains 4 vertices of data
												for (int v = 0; v < 4; v++) {
													for (int w = 0; w < 7; w++) {
														rawBuffer[putIndex + (v * 7) + w] = thisQuad[order[v]][w];												
													}													
												}
												changed = true;
											}

											putIndex += 28;

										}

										if (changed) {
											data.rewind();
											data.put(rawBuffer);
											data.rewind();
											GL15.glBufferSubData(GLX.GL_ARRAY_BUFFER, 0, data);
										}										
									}

								}

								vbo.unbindBuffer();

							}
						}

					});
				}
			});

			if (SEL.lightWorker.isShutdown() && SEL.lightWorker.chunksToBeReRendered.size() == 0) {
				SEL.forceUpdate = false;
			}
		}

		// ClientProxy.mcProfiler.endSection();
	}

	@SubscribeEvent
	public void onDebugOverlay(RenderGameOverlayEvent.Text event) {
		if (ClientProxy.mcinstance.gameSettings.showDebugInfo) {
			// There used to be some interesting stats to look at!
			event.getLeft().add("DL " + (SEL.disabled ? "OFF" : "ON"));

			// Light levels
			Entity player = ClientProxy.mcinstance.player;
			World world = ClientProxy.mcinstance.world;
			BlockPos pos = player.getPosition();
			BlockState state = world.getBlockState(pos);
			ILitChunkCache litChunkCache = LightUtils.getLitChunkCache(world, pos.getX() >> 4, pos.getZ() >> 4);

			event.getLeft().add("Vanilla BL: " + state.getLightValue(world, pos) + " SEL: "
					+ (litChunkCache != null && SEL.enabledForDimension(ClientProxy.mcinstance.player.dimension)
							? Math.round(
									10f * 16f * LightUtils.getEntityLightLevel(world, pos, event.getPartialTicks()))
									/ 10f
									+ " B-Cache: "
									+ Math.round(10f * litChunkCache.getBlockLight(pos.getX(), pos.getY(), pos.getZ()))
											/ 10f
									+ " V-Cache: "
									+ Math.round(10f * litChunkCache.getVertexLight(pos.getX(), pos.getY(), pos.getZ()))
											/ 10f
							: "Disabled for this dimension"));
			event.getLeft()
					.add("SEL avg blocks re-lit: " + Math.round(10f * SEL.lightWorker.averageBlockCount()) / 10f
							+ " skipped ticks: " + SEL.lightWorker.ticksSkippedCount + " E: "
							+ SEL.lightWorker.entityCount.get());

//			DirtyRayTrace rayTrace = new DirtyRayTrace(world);
//			RayTraceResult rtr = ClientProxy.mcinstance.player.rayTrace(40f, event.getPartialTicks());
//			String message = "";
//			if (rtr != null) {
//				message = "Opacity: ";
//				message += rayTrace.rayTraceForOpacity(
//						ClientProxy.mcinstance.player.getPositionEyes(event.getPartialTicks()), rtr.hitVec);
//			}
//			event.getLeft().add(message);

		}
	}

	@SubscribeEvent
	public void onAttachChunkCapabilities(AttachCapabilitiesEvent<Chunk> event) {
		Chunk chunk = event.getObject();
		if (chunk == null) {
			return;
		}

		LitChunkCacheCapability cap = new LitChunkCacheCapability();
		cap.setChunkPos(chunk.getPos().x, chunk.getPos().z);
		event.addCapability(SEL.LIT_CHUNK_CACHE_CAPABILITY_NAME, cap);
	}

	@SubscribeEvent
	public void onAttachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
		Entity entity = event.getObject();
		if (entity == null || !entity.isAlive())
			return;
		
		World world = entity.getEntityWorld();
		if (world == null || !world.isRemote)
			return;

		// Don't even add light sources to Entities in blacklisted dimensions
		if (!SEL.enabledForDimension(entity.dimension)) {
			return;
		}

		DefaultLightSourceCapability sources = new DefaultLightSourceCapability();
		sources.init(entity, world);

		if (entity instanceof ItemEntity) {
			if (!Config.lightDroppedItems)
				return;

			EntityItemAdaptor adaptor = new EntityItemAdaptor((ItemEntity) entity);
			sources.addLightSource(adaptor);
		} else if (entity instanceof LivingEntity && !(entity instanceof PlayerEntity)) {
			int minLight = 0;
			boolean catchesFire = false;
			
			catchesFire = Config.lightBurningEntities && !Config.nonFlamableMobs.contains(entity.getClass().getSimpleName());

			if (Config.lightGlowingEntities) {
				minLight = Config.getMobGlow(entity);
			}

			if (catchesFire) {
				EntityBurningAdaptor adaptor = new EntityBurningAdaptor(entity);
				adaptor.minLight = minLight;
				sources.addLightSource(adaptor);
			} else if (minLight > 0) {
				BrightAdaptor adaptor = new BrightAdaptor(entity, minLight);
				sources.addLightSource(adaptor);
			}

			if (Config.lightMobEquipment) {
				MobLightAdaptor adaptor = new MobLightAdaptor((LivingEntity) entity);
				sources.addLightSource(adaptor);
			}

			if (Config.lightChargingCreepers && entity instanceof CreeperEntity) {
				CreeperAdaptor adaptor = new CreeperAdaptor((CreeperEntity) entity);
				sources.addLightSource(adaptor);
			}

		} else if (entity instanceof ArrowEntity || entity instanceof FireballEntity) {
			if (!Config.lightFlamingArrows)
				return;

			EntityBurningAdaptor adaptor = new EntityBurningAdaptor(entity);
			sources.addLightSource(adaptor);
		} else if (entity instanceof ExperienceOrbEntity) {
			if (!Config.lightXP)
				return;

			BrightAdaptor adaptor = new BrightAdaptor(entity, 10);
			sources.addLightSource(adaptor);
		} else if (entity instanceof ServerPlayerEntity) {
			if (!Config.lightOtherPlayers)
				return;

			PlayerOtherAdaptor adaptor = new PlayerOtherAdaptor((ServerPlayerEntity) entity);
			sources.addLightSource(adaptor);
		} else if (entity instanceof ClientPlayerEntity) {
			if (Config.lightFloodLight) {
				FloodLightAdaptor adaptor = new FloodLightAdaptor(entity, Config.simpleMode);
				sources.addLightSource(adaptor);
			}

			if (!Config.lightThisPlayer)
				return;

			PlayerSelfAdaptor adaptor = new PlayerSelfAdaptor((PlayerEntity) entity);
			sources.addLightSource(adaptor);

			checkForOptifine();
		} else if (entity instanceof FloodLightAdaptor.DummyEntity) {
			if (Config.lightFloodLight) {
				PartialLightAdaptor adaptor = new PartialLightAdaptor(entity);
				sources.addLightSource(adaptor);
			}
		} else {
			// Do nothing
		}

		if (sources.hasSources()) {
			event.addCapability(SEL.LIGHT_SOURCE_CAPABILITY_NAME, sources);
			SEL.lightWorker.addSourceEntity(sources);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onEntityJoinWorld(EntityJoinWorldEvent event) {
		if (event.getEntity().equals(ClientProxy.mcinstance.player) && SEL.lightWorker != null) {
			SEL.lightWorker.setPlayer(ClientProxy.mcinstance.player);
			updateFrustum();
		}
	}

	private void updateFrustum() {
		SEL.lightWorker.updateFrustum(ClientProxy.mcinstance.worldRenderer.viewFrustum);
	}

	private void checkForOptifine() {
		if (LightUtils.hasOptifine() && !Config.optifineOverride) {
			ClientProxy.mcinstance.ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(
					"Optifine is loaded.  Disabling Smooth Entity Light.  Check the config file to override."));
			SEL.disabled = true;
		}
	}

	// Vertex (4 per quad)
	// Position = 3 floats
	// Brightness
	// Color
	// Color
	// Color
	// Color
	// Position
	private void readState(BufferBuilder.State state, BlockPos pos) {
		VertexFormat format = state.getVertexFormat();
		int intSize = format.getIntegerSize();
		int vertexNum = 0;
		int[] rawInts = state.getRawBuffer();
		int vertexCount = state.getVertexCount();

		float x;
		float y;
		float z;

		while (vertexNum < vertexCount) {
			int iStart = (vertexNum * intSize);

			// String result = "Vertex Data: ";
			// for (int i = 0; i < intSize; i++) {
			// result += String.format("0x%08x", rawInts[iStart + i]) + " ";
			// }
			// System.out.println(result);

			Vector3f vPos = getVertexPos(rawInts, vertexNum);
			int iX = iStart;
			int iY = iX + 1;
			int iZ = iY + 1;
			int iC = iStart + 3;// format.getColorOffset() / 4;
			int colourR = rawInts[iC];
			int colourG = rawInts[iC + 1];
			int colourB = rawInts[iC + 2];
			// rawInts[iC] &= 0x77FF7700;

			String colourRs = String.format("0x%08x", colourR);
			String colourGs = String.format("0x%08x", colourG);
			String colourBs = String.format("0x%08x", colourB);
			x = Float.intBitsToFloat(rawInts[iX]); // pos.getX() +
			y = Float.intBitsToFloat(rawInts[iY]); // pos.getY() + - ClientProxy.mcinstance.player.eyeHeight;
			z = Float.intBitsToFloat(rawInts[iZ]); // pos.getZ() +

			// rawInts[iY] = Float.floatToIntBits(y - 0.1f);
			// System.out.println("Offset = "+ pos.getX() + ", " + pos.getY() + ", " +
			// pos.getZ() +" Found pos: " + x + ", " + y + ", " + z + " with Colour=" +
			// colour);

			// System.out.println("Offset = " + vPos.toString() + " with Colour= " +
			// colourRs + " " + colourGs + " " + colourBs);

			// if (ClientProxy.mcinstance.player.getPosition().distanceSq(new Vec3i(x, y,
			// z)) < 2.0) {
			// System.out.println("boom");
			// }

			vertexNum++;
		}

	}

	private static Vector3f getVertexPos(int[] data, int vertex) {
		int idx = vertex * 7;

		float x = Float.intBitsToFloat(data[idx]);
		float y = Float.intBitsToFloat(data[idx + 1]);
		float z = Float.intBitsToFloat(data[idx + 2]);

		return new Vector3f(x, y, z);
	}

	public static void putQuadColor(BufferBuilder renderer, BakedQuad quad, int color) {
		float cb = color & 0xFF;
		float cg = (color >>> 8) & 0xFF;
		float cr = (color >>> 16) & 0xFF;
		float ca = (color >>> 24) & 0xFF;
		VertexFormat format = quad.getFormat();
		int size = format.getIntegerSize();
		int offset = format.getColorOffset() / 4; // assumes that color is aligned
		for (int i = 0; i < 4; i++) {
			int vc = quad.getVertexData()[offset + size * i];
			float vcr = vc & 0xFF;
			float vcg = (vc >>> 8) & 0xFF;
			float vcb = (vc >>> 16) & 0xFF;
			float vca = (vc >>> 24) & 0xFF;
			int ncr = Math.min(0xFF, (int) (cr * vcr / 0xFF));
			int ncg = Math.min(0xFF, (int) (cg * vcg / 0xFF));
			int ncb = Math.min(0xFF, (int) (cb * vcb / 0xFF));
			int nca = Math.min(0xFF, (int) (ca * vca / 0xFF));
			renderer.putColorRGBA(renderer.getColorIndex(4 - i), ncr, ncg, ncb, nca);
		}
	}

}
