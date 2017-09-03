package atomicstryker.dynamiclights.client;

import java.util.ArrayList;

import atomicstryker.dynamiclights.client.adaptors.BaseAdaptor;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**
 * 
 * @author AtomicStryker
 * 
 * Container class to keep track of IDynamicLightSource instances. Remembers
 * their last position and calls World updates if they move.
 *
 */
public class DynamicLightSourceContainer implements IExtendedEntityProperties
{
	protected World world;
	protected Entity entity;
	
	private ArrayList<BaseAdaptor> adaptors;
    
    private int prevLight;

    private int prevX;
    private int prevY;
    private int prevZ;
    
    private boolean hasPrevious = false;
            
    public DynamicLightSourceContainer(Entity entity, World world)
    {
		this.world = world;
		this.entity = entity;
    		adaptors = new ArrayList<BaseAdaptor>();
        prevLight = getLightLevel();
        prevX = prevY = prevZ = 0;
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
    public int getLightLevel() {
    		if (DynamicLights.globalLightsOff)
    			return 0;

    		int light = 0;
    		for (BaseAdaptor adaptor : adaptors) {
    			light = DynamicLights.maxLight(light, adaptor.getLightLevel());
    		}
    		return Math.min(15, light);
    }
    
    /**
     * Update passed on from the World tick. Checks for the Light Source Entity to be alive,
     * and for it to have changed Coordinates or light level. Marks it's current Block for Update if it has
     * moved. When this method returns true, the Light Source Entity has died and it should
     * be removed from the List!
     * 
     * @return true when the Light Source has died, false otherwise
     */
    public void update()
    {    		
        if (entity == null || world == null)
        {
            return;
        }

        int currentX = MathHelper.floor_double(entity.posX);
        int currentY = MathHelper.floor_double(entity.posY);
        int currentZ = MathHelper.floor_double(entity.posZ);
        int lightLevel = getLightLevel();

        if(lightLevel == 0 && prevLight == 0) {
        		return;
        }
        
        if (currentX != prevX || currentY != prevY || currentZ != prevZ)
        {
            /*
             * This is the critical point, by this we tell Minecraft to ask for the BlockLight value
             * at the coordinates, which in turn triggers the Dynamic Lights response pointing to
             * this Light's value, which in turn has Minecraft update all surrounding Blocks :3
             * 
             * We also have to call an update for the previous coordinates, otherwise they would
             * stay lit up.
             */
        		if (hasPrevious) {
	    	    		int minX = Math.min(prevX, currentX);
	    	    		int minY = Math.min(prevY, currentY);
	    			int minZ = Math.min(prevZ, currentZ);
	        		int maxX = Math.max(prevX, currentX);
	        		int maxY = Math.max(prevY, currentY);
	    			int maxZ = Math.max(prevZ, currentZ);
            		for (int x = minX; x <= maxX; x++) {
                		for (int y = minY; y <= maxY; y++) {
                    		for (int z = minZ; z <= maxZ; z++) {
                    			world.updateLightByType(EnumSkyBlock.Block, x, y, z);
                    		}            			
                		}
            		}        			
        		}
        		else {
        			world.updateLightByType(EnumSkyBlock.Block, currentX, currentY, currentZ);        			
        		}

    	        prevX = currentX;
    	        prevY = currentY;
    	        prevZ = currentZ;
    	        hasPrevious = true;
            prevLight = lightLevel;
        } 
        else if (prevLight != lightLevel) 
        {
	        world.updateLightByType(EnumSkyBlock.Block, currentX, currentY, currentZ);
            prevLight = lightLevel;
        }
    }

    public void destroy() {    		
    		for (BaseAdaptor adaptor : adaptors) {
    			adaptor.kill();
		}
    		adaptors.clear();
    		update();
    		entity = null;
    		world = null;
    }
    
	@Override
	public void saveNBTData(NBTTagCompound compound) {
	}

	@Override
	public void loadNBTData(NBTTagCompound compound) {		
	}

}
