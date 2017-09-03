package atomicstryker.dynamiclights.client.adaptors;

import net.minecraft.entity.Entity;

public class EntityBurningAdaptor extends BaseAdaptor
{        
	public int minLight = 0;
 	
	public EntityBurningAdaptor(Entity entity) {
		super(entity);
	}
	    
	@Override
    public int getLightLevel()
    {
        if (entity.isBurning())
        {
            return 15;
        }
        else
        {
            return minLight;
        }
    }
    
}
