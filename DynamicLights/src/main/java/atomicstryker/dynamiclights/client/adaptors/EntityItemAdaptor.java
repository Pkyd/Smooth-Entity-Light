package atomicstryker.dynamiclights.client.adaptors;

import atomicstryker.dynamiclights.client.Config;
import cpw.mods.fml.common.registry.GameData;
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
				stack = entity.getDataWatcher().getWatchableObjectItemStack(10);
			}
			if (stack != null)
			{
				notWaterProof = Config.notWaterProofItems.retrieveValue(GameData.getItemRegistry().getNameForObject(stack.getItem()), stack.getMetadata()) == 1;
				stackLightlevel = Config.itemsMap.getLightFromItemStack(stack);            		                		
			}            		

			if (stack == null)
				return 0;
			else
			{
				if (notWaterProof &&
						entity.worldObj.getBlock((int)entity.posX, (int)entity.posY, (int)entity.posZ).getMaterial() == Material.water)
					return 0;
				else
					return stackLightlevel;                	
			}
		}
	}
	
	@Override
	public void kill() {
		super.kill();
		stack = null;
	}

}
