package lakmoore.sel.client.adaptors;

import net.minecraft.entity.monster.EntityCreeper;

public class CreeperAdaptor extends BaseAdaptor
{
	public CreeperAdaptor(EntityCreeper eC)
	{
		super(eC);
	}

	@Override
	public int getLightLevel()
	{
		return ((EntityCreeper)entity).getCreeperState() == 1 ? 15 : 0;
	}
}