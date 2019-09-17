package lakmoore.sel.client.adaptors;

import net.minecraft.entity.Entity;

public class BrightAdaptor extends BaseAdaptor
{        
	private final int lightLevel;

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

	@Override
	public int getLightLevel() {
		return lightLevel;
	}

}
