package lakmoore.sel.client.adaptors;

import net.minecraft.entity.Entity;

public class PartialLightAdaptor extends BaseAdaptor
{
	public int lightLevel = 0;
	public int Id;

	public PartialLightAdaptor(Entity entity) {
		super(entity);
		Id = FloodLightAdaptor.ID;
		FloodLightAdaptor.ID++;
		FloodLightAdaptor.lights.add(this);
	}

	@Override
	public int getLightLevel() {			
        return lightLevel;        
	}
	
	@Override
	public void kill() {
		super.kill();
	}
	
}
