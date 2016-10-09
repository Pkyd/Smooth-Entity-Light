package atomicstryker.dynamiclights.client.adaptors;

import net.minecraft.entity.Entity;

public class BrightAdaptor extends BaseAdaptor
{        
    public BrightAdaptor(Entity e)
    {
    	super(e);
    	this.lightLevel = 15;
    }
    
    public BrightAdaptor(Entity e, int lightLevel)
    {
    	super(e);
    	this.lightLevel = lightLevel;
    }

}
