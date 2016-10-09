package atomicstryker.dynamiclights.client.adaptors;

import net.minecraft.entity.Entity;

public class EntityBurningAdaptor extends BaseAdaptor
{        
	public int minLight = 0;
	
    public EntityBurningAdaptor(Entity e)
    {
    	super(e);
    }
    
    public void onTick()
    {
        if (entity.isBurning())
        {
            lightLevel = 15;
        }
        else
        {
            lightLevel = minLight;
        }
        
        checkForchange();
    }
    
}
