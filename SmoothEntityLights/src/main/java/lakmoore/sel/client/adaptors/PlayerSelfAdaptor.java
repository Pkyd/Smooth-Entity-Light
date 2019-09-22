package lakmoore.sel.client.adaptors;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLInterModComms.IMCMessage;
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
 * With version 1.1.3 and later you can also use FMLIntercomms to use this 
 * and have the player shine light. Like so:
 * 
 * FMLInterModComms.sendRuntimeMessage(sourceMod, "DynamicLights_thePlayer", "forceplayerlighton", "");
 * FMLInterModComms.sendRuntimeMessage(sourceMod, "DynamicLights_thePlayer", "forceplayerlightoff", "");
 * 
 * Note you have to track this yourself. Smooth Entity Light will accept and obey, but not recover should you
 * get stuck in the on or off state inside your own code. It will not revert to off on its own.
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
        if (thePlayer != null && thePlayer.isEntityAlive() && !SEL.disabled && thePlayer.addedToChunk)
        {
            List<IMCMessage> messages = FMLInterModComms.fetchRuntimeMessages(this);
            if (messages.size() > 0)
            {
                // just get the last one
                IMCMessage imcMessage = messages.get(messages.size()-1);
                if (imcMessage.key.equalsIgnoreCase("forceplayerlighton"))
                {
                    if (!SEL.fmlOverrideEnable)
                    {
                    		SEL.fmlOverrideEnable = true;
		                	return 15;
                    }
                }
                else if (imcMessage.key.equalsIgnoreCase("forceplayerlightoff"))
                {
                    if (SEL.fmlOverrideEnable)
                    {
                    		SEL.fmlOverrideEnable = false;
                    		return 0;
                    }
                }
            }
            
            if (!SEL.fmlOverrideEnable)
            {
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
                        	&& (!inWater || Config.notWaterProofItems.retrieveValue(stack.getItem().getRegistryName(), stack.getMetadata()) == 0))
                        {
                            lightLevel = LightUtils.maxLight(lightLevel, Config.itemsMap.getLightFromItemStack(stack));
                        }
                    }
                    return lightLevel;
                }
            }
        }        
        return 0;
    }
    
    private boolean checkPlayerWater(EntityPlayer thePlayer)
    {
        if (thePlayer.isInWater())
        {
            return thePlayer.world.getBlockState(new BlockPos(thePlayer.getPositionEyes(1f))).getMaterial() == Material.WATER;
        }
        return false;
    }
    
	@Override
	public void kill() {
		super.kill();
		thePlayer = null;
	}


}
