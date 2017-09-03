package atomicstryker.dynamiclights.client.adaptors;

import net.minecraft.entity.Entity;

public class BrightAdaptor extends BaseAdaptor
{        
	private int lightLevel = 15;

	public BrightAdaptor(Entity e)
    {
		super(e);
    }
    
    public BrightAdaptor(Entity e, int lightLevel)
    {
    		super(e);
    		this.lightLevel = lightLevel;
    }

	@Override
	public int getLightLevel() {
		return lightLevel;
	}

}
