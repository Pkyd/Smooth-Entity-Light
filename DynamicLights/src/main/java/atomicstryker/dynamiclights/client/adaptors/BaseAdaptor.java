package atomicstryker.dynamiclights.client.adaptors;

import net.minecraft.entity.Entity;

public abstract class BaseAdaptor
{
	protected Entity entity;

	BaseAdaptor(Entity entity)
	{
		this.entity = entity;		
	}

	public abstract int getLightLevel();
	    
    public void kill()
    {
        entity = null;
    }
    
}
