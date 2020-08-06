package lakmoore.sel.client.adaptors;

import net.minecraft.entity.monster.CreeperEntity;

public class CreeperAdaptor extends BaseAdaptor
{
	protected CreeperEntity creeper;

	public CreeperAdaptor(CreeperEntity creeper)
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