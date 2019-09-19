package lakmoore.sel.client.adaptors;

import lakmoore.sel.client.Config;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

public class EntityItemAdaptor extends BaseAdaptor
{
	private ItemStack stack;
	private int stackLightlevel;
	private boolean notWaterProof;

	public EntityItemAdaptor(EntityItem eI)
	{
		super(eI);
	}

	public int getLightLevel()
	{        	        	
		if (entity == null || entity.isDead) {
			return 0;
		}
		else if (entity.isBurning())
		{
			return 15;
		}
		else
		{           
			if (stack == null) {
				stack = ((EntityItem)entity).getItem();
				if (stack != null) {
					notWaterProof = Config.notWaterProofItems.retrieveValue(stack.getItem().getRegistryName(), stack.getMetadata()) == 1;
					stackLightlevel = Config.itemsMap.getLightFromItemStack(stack);            		                		
				}
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
		stack = null;
	}

}
