package lakmoore.sel.client.adaptors;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.InterModComms.IMCMessage;
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
	EntityPlayer thePlayer;
	
    public PlayerSelfAdaptor(EntityPlayer entity) {
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
                    	&& (!inWater || !Config.notWaterProofItems.contains(stack.getTranslationKey())))
                    {
                        lightLevel = LightUtils.maxLight(lightLevel, Config.lightValueMap.get(stack.getTranslationKey()));
                    }
                }
                return lightLevel;
            }
            
        }        
        return 0;
    }
    
    private boolean checkPlayerWater(EntityPlayer thePlayer)
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
