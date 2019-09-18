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
import net.minecraft.util.math.MathHelper;
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
	private float prevX;
	private float prevY;
	private float prevZ;
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
		prevX = (float) entity.posX;
		prevY = (float) entity.posY;
		prevZ = (float) entity.posZ;

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

		float currentX = (float) entity.posX;
		float currentY = (float) entity.posY;
		float currentZ = (float) entity.posZ;

		// Re-calculate the light level
		int lightLevel = getLightLevel();

		// If this entity has and had no light level
		if (lightLevel == 0 && prevLight == 0) {
			// Do nothing
			return result;
		}

		float dX = currentX - prevX;
		float dY = currentY - prevY;
		float dZ = currentZ - prevZ;
		float sqDist = (dX * dX) + (dY * dY) + (dZ * dZ);

		// If the entity has moved or changed light level
		if (sqDist > maxDiff || lightLevel != prevLight) {
			prevLight = lightLevel;

			int radius = 8;
			int pX = (int) prevX;
			int pY = (int) prevY;
			int pZ = (int) prevZ;
			int cX = (int) currentX;
			int cY = (int) currentY;
			int cZ = (int) currentZ;

			// always re-light the old position (think extinguished torches!)
			result.addAll(LightUtils.getVolumeForRelight(pX, pY, pZ, radius));

			// If we have moved to another block
			if (pX != cX || pY != cY || pZ != cZ) {
				// re-light the current position
				result.addAll(LightUtils.getVolumeForRelight(cX, cY, cZ, radius));
			}

			// update the old position to the new position
			prevX = currentX;
			prevY = currentY;
			prevZ = currentZ;

			checkDistanceLOD();

			Block block = world.getBlockState(new BlockPos(MathHelper.floor(currentX), MathHelper.floor(currentY), MathHelper.floor(currentZ))).getBlock();
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
		if (Math.pow(thePlayer.posX - prevX, 2) + Math.pow(thePlayer.posY - prevY, 2)
				+ Math.pow(thePlayer.posZ - prevZ, 2) > farDistSq) {
			maxDiff = maxDiffFar;
		}

	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (capability == SEL.LIGHT_SOURCE_CAPABILITY) {
			return true;
		}
		return false;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == SEL.LIGHT_SOURCE_CAPABILITY) {
			return SEL.LIGHT_SOURCE_CAPABILITY.cast(this);
		}
		return null;
	}

}
