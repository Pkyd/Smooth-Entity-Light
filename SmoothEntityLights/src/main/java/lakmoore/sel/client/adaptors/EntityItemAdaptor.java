package lakmoore.sel.client.adaptors;

import lakmoore.sel.client.Config;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.ResourceLocation;

public class EntityItemAdaptor extends BaseAdaptor
{
	private int stackLightlevel = -1;
	private boolean notWaterProof;

	public EntityItemAdaptor(EntityItem eI)
	{
		super(eI);
		// When EntityItems are spawned they have no knowledge of what item they will become!
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
			if (this.stackLightlevel == -1) {
				ResourceLocation stackName = ((EntityItem)this.entity).getItem().getItem().getRegistryName();
				notWaterProof = Config.notWaterProofItems.contains(stackName);
				stackLightlevel = Config.lightValueMap.getOrDefault(stackName, 0);			
			}

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
