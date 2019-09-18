package lakmoore.sel.client.adaptors;

import net.minecraft.entity.monster.EntityCreeper;

public class CreeperAdaptor extends BaseAdaptor
{
	protected EntityCreeper creeper;

	public CreeperAdaptor(EntityCreeper creeper)
	{
		super(creeper);
		this.creeper = creeper;
	}

	@Override
	public int getLightLevel()
	{
		return this.creeper.getCreeperState() == 1 ? 15 : 0;
	}
}