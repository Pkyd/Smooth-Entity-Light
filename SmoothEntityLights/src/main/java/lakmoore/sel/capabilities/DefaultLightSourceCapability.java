package lakmoore.sel.capabilities;

import java.util.ArrayList;

import lakmoore.sel.client.FMLEventHandler;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;
import lakmoore.sel.client.adaptors.BaseAdaptor;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * 
 * @author LakMoore
 * 
 *         Container class to keep track of Entity Light sources. Remembers
 *         their last position and calls World updates if they move. Updates the
 *         lightmap onTick, if they have moved or changed brightness
 *
 */
public class DefaultLightSourceCapability implements ICapabilityProvider, ILightSourceCapability {
	private static Entity thePlayer;
	private static final float maxDiffNear = 0.01f;
	private static final float maxDiffFar = 1.4f;
	private static final float farDistSq = 1024.0f;

	protected World world;
	protected Entity entity;

	private ArrayList<BaseAdaptor> adaptors;

	private float maxDiff;
	private BlockPos prev;
	private int prevLight;
	private boolean underwater = false;

	static {
		thePlayer = Minecraft.getMinecraft().player;
	}
	public DefaultLightSourceCapability() {		
	}

	public void init(Entity entity, World world) {
		this.world = world;
		this.entity = entity;
		adaptors = new ArrayList<BaseAdaptor>();
		prevLight = 0;
		prev = new BlockPos(entity.posX, entity.posY, entity.posZ);

		checkDistanceLOD();
	}

	public void addLightSource(BaseAdaptor adaptor) {
		adaptors.add(adaptor);
	}
	
	public boolean hasSources() {
		return adaptors.size() > 0;
	}

	/**
	 * Values above 15 will not be considered, 15 is the MC max level. Values below
	 * 1 are considered disabled. Values can be changed on the fly.
	 * 
	 * @return int value of Minecraft Light level at the Dynamic Light Source
	 */
	private int getLightLevel() {
//    	Minecraft.getMinecraft().mcProfiler.startSection(SEL.modId + ":getLightLevel");

		int light = 0;
		if (!SEL.disabled) {
			for (BaseAdaptor adaptor : adaptors) {
				light = LightUtils.maxLight(light, adaptor.getLightLevel());
				// Don't exit loop early, getLightLevel() might also update the source!
			}
			light = Math.min(15, light);
		}

//		Minecraft.getMinecraft().mcProfiler.endSection();
		return light;
	}

	public int getLastLightLevel() {
		return prevLight;
	}

	/**
	 * Mainly passed on from the World tick. Checks for the Light Source Entity to
	 * have changed Coordinates or light level. Returns set of blocks that may need
	 * re-lighting if something has changed.
	 */
	public ArrayList<BlockPos> getBlocksToUpdate() {
		ArrayList<BlockPos> result = new ArrayList<BlockPos>();
		if (entity == null || world == null) {
			return result;
		}

		BlockPos current = new BlockPos(entity.posX, entity.posY, entity.posZ);

		// Re-calculate the light level
		int lightLevel = getLightLevel();

		// If this entity has and had no light level
		if (lightLevel == 0 && prevLight == 0) {
			// Do nothing
			return result;
		}

		// If the entity has moved or changed light level
		if (current.distanceSq(prev) > maxDiff || lightLevel != prevLight) {
			prevLight = lightLevel;

			int radius = 8;

			// always re-light the old position (think extinguished torches!)
			result.addAll(LightUtils.getVolumeForRelight(prev, radius));

			// If we have moved to another block
			if (!current.equals(prev)) {
				// re-light the current position
				result.addAll(LightUtils.getVolumeForRelight(current, radius));
			}

			// update the old position to the new position
			prev = current;

			checkDistanceLOD();

			Block block = world.getBlockState(current).getBlock();
			this.underwater = (block == Blocks.WATER);
		}
		return result;
	}

	public boolean isUnderwater() {
		return this.underwater;
	}

	public void destroy() {
//    	SEL.mcProfiler.startSection(SEL.modId + ":destroy");
		for (BaseAdaptor adaptor : adaptors) {
			adaptor.kill();
		}
		adaptors.clear();
		FMLEventHandler.blocksToUpdate.addAll(getBlocksToUpdate());
		entity = null;
		world = null;
//		SEL.mcProfiler.endSection();
	}

	private void checkDistanceLOD() {
		maxDiff = maxDiffNear;
		if (
			thePlayer != null
			&& prev.distanceSq(thePlayer.posX, thePlayer.posY, thePlayer.posZ) > farDistSq
		) {
			maxDiff = maxDiffFar;
		}
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == SEL.LIGHT_SOURCE_CAPABILITY;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == SEL.LIGHT_SOURCE_CAPABILITY) {
			return SEL.LIGHT_SOURCE_CAPABILITY.cast(this);
		}
		return null;
	}

}
