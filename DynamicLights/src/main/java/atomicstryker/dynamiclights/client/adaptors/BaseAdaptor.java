package atomicstryker.dynamiclights.client.adaptors;

import atomicstryker.dynamiclights.client.DynamicLights;
import atomicstryker.dynamiclights.client.IDynamicLightSource;
import net.minecraft.entity.Entity;

public class BaseAdaptor implements IDynamicLightSource
{
	protected Entity entity;
	protected int lightLevel;
	protected boolean enabled;

	BaseAdaptor(Entity entity)
	{
		this.entity = entity;		
        lightLevel = 0;
        enabled = false;
	}

	@Override
    public Entity getAttachmentEntity()
    {
        return entity;
    }

	public int getLightLevel() {
		return lightLevel;
	}
	
	public void enableLight()
    {
        DynamicLights.addLightSource(this);
        enabled = true;
    }
    
    public void disableLight()
    {
        DynamicLights.removeLightSource(this);
        enabled = false;
    }
    
    protected void checkForchange()
    {
        if (!enabled && lightLevel > 0)
        {
            enableLight();
        }
        else if (enabled && lightLevel < 1)
        {
            disableLight();
        }
    }

	public void onTick() {
		checkForchange();		
	}

}
