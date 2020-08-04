package lakmoore.sel.client.adaptors;

import lakmoore.sel.client.Config;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;

public class EntityItemAdaptor extends BaseAdaptor
{
	private int stackLightlevel;
	private boolean notWaterProof;

	public EntityItemAdaptor(EntityItem eI)
	{
		super(eI);
		if (eI != null) {
			String stackName = eI.getItem().getTranslationKey();
			notWaterProof = Config.notWaterProofItems.contains(stackName);
			stackLightlevel = Config.lightValueMap.get(stackName);			
		}
	}

	public int getLightLevel()
	{        	        	
		if (entity == null || !entity.isAlive()) {
			return 0;
		}
		else if (entity.isBurning())
		{
			return 15;
		}
		else
		{           
			if (
				notWaterProof 
				&& entity.world.getBlockState(entity.getPosition()).getMaterial() == Material.WATER
			)
				return 0;
			else
				return stackLightlevel;                	
		}
	}
	
	@Override
	public void kill() {
		super.kill();
	}

}
