package lakmoore.sel.client.adaptors;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import lakmoore.sel.client.Config;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;

/**
 * 
 * @author AtomicStryker, LakMoore
 * 
 * Offers Entity Light functionality to the Player Entity itself.
 * Handheld Items and Armor can give off Light through this Module.
 *
 */
public class PlayerSelfAdaptor extends BaseAdaptor
{
	PlayerEntity thePlayer;
	
    public PlayerSelfAdaptor(PlayerEntity entity) {
		super(entity);
		thePlayer = entity;
	}

    @Override
    public int getLightLevel()
    {
        if (thePlayer != null && thePlayer.isAlive() && !SEL.disabled && thePlayer.addedToChunk)
        {            
            if (SEL.fmlOverride > -1)
            {
            	return SEL.fmlOverride;
            }
            
            if (thePlayer.isBurning())
            {
                return 15;
            }
            else
            {
                //if we are underwater and the source is extinguishable => light = 0
                boolean inWater = checkPlayerWater(thePlayer);
                
                int lightLevel = 0;

                //go through the item and armor slots looking for brighter items
                for (ItemStack stack : thePlayer.getEquipmentAndArmor())
                {
                    if (
                    	stack != null                     	
                    	&& !stack.isEmpty()
                    	&& (!inWater || !Config.notWaterProofItems.contains(stack.getItem().getRegistryName())))
                    {
                        lightLevel = LightUtils.maxLight(lightLevel, Config.lightValueMap.getOrDefault(stack.getItem().getRegistryName(), 0));
                    }
                }
                return lightLevel;
            }
            
        }        
        return 0;
    }
    
    private boolean checkPlayerWater(PlayerEntity thePlayer)
    {
        if (thePlayer.isInWater())
        {
            return thePlayer.world.getBlockState(new BlockPos(thePlayer.getEyePosition(1f))).getMaterial() == Material.WATER;
        }
        return false;
    }
    
	@Override
	public void kill() {
		super.kill();
		thePlayer = null;
	}


}
