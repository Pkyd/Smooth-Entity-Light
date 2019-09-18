package lakmoore.sel.client;

import java.util.ArrayList;

import lakmoore.sel.client.adaptors.BaseAdaptor;
import lakmoore.sel.world.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**
 * 
 * @author LakMoore
 * 
 * Container class to keep track of Entity Light sources. 
 * Remembers their last position and calls World updates if they move.
 * Updates the lightmap onTick, if they have moved or changed brightness
 *
 */
public class SELSourceContainer implements IExtendedEntityProperties
{
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
		thePlayer = Minecraft.getMinecraft().thePlayer;
    }
                
    public SELSourceContainer(Entity entity, World world)
    {
		this.world = world;
		this.entity = entity;
		adaptors = new ArrayList<BaseAdaptor>();
        prevLight = 0;
        prevX = (float)entity.posX;
        prevY = (float)entity.posY;
        prevZ = (float)entity.posZ;
        
        checkDistanceLOD();
    }
    
    /**
     * Used to initialize the extended properties with the entity that this is attached to, as well
     * as the world object.
     * Called automatically if you register with the EntityConstructing event.
     * May be called multiple times if the extended properties is moved over to a new entity.
     *  Such as when a player switches dimension {Minecraft re-creates the player entity}
     * @param entity  The entity that this extended properties is attached to
     * @param world  The world in which the entity exists
     */
    @Override
	public void init(Entity entity, World world) {
		this.world = world;
		this.entity = entity;
	}
    
    public void addLightSource(BaseAdaptor adaptor) {
		adaptors.add(adaptor);
    }
    
    /**
     * Values above 15 will not be considered, 15 is the MC max level. Values below 1 are considered disabled.
     * Values can be changed on the fly.
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
    public ArrayList<BlockPos> getBlocksToUpdate()
    {    		
    	ArrayList<BlockPos> result = new ArrayList<BlockPos>();
        if (entity == null || world == null)
        {
            return result;
        }

        float currentX = (float)entity.posX;
        float currentY = (float)entity.posY;
        float currentZ = (float)entity.posZ;
        
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
        if (
        	sqDist > maxDiff 
        	|| lightLevel != prevLight
        ) {
            prevLight = lightLevel;
            
            int radius = 8;
            int pX = (int)prevX;
            int pY = (int)prevY;
            int pZ = (int)prevZ;
            int cX = (int)currentX;
            int cY = (int)currentY;
            int cZ = (int)currentZ;
            
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

            Block block = world.getBlock(MathHelper.floor_float(currentX), MathHelper.floor_float(currentY), MathHelper.floor_float(currentZ));
            this.underwater = (block == Blocks.water);            
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
    
	@Override
	public void saveNBTData(NBTTagCompound compound) {
	}

	@Override
	public void loadNBTData(NBTTagCompound compound) {		
	}
	
	private void checkDistanceLOD() {
        maxDiff = maxDiffNear;
        if (
        	Math.pow(thePlayer.posX - prevX, 2)
        	+ Math.pow(thePlayer.posY - prevY, 2)
        	+ Math.pow(thePlayer.posZ - prevZ, 2)
        	> farDistSq
		) {
        	maxDiff = maxDiffFar;
        }

	}

}
