package atomicstryker.dynamiclights.client.adaptors;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import atomicstryker.dynamiclights.client.Config;
import atomicstryker.dynamiclights.client.DynamicLights;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import cpw.mods.fml.common.registry.GameData;

/**
 * 
 * @author AtomicStryker
 *
 * Offers Dynamic Light functionality to the Player Entity itself.
 * Handheld Items and Armor can give off Light through this Module.
 * 
 * With version 1.1.3 and later you can also use FMLIntercomms to use this 
 * and have the player shine light. Like so:
 * 
 * FMLInterModComms.sendRuntimeMessage(sourceMod, "DynamicLights_thePlayer", "forceplayerlighton", "");
 * FMLInterModComms.sendRuntimeMessage(sourceMod, "DynamicLights_thePlayer", "forceplayerlightoff", "");
 * 
 * Note you have to track this yourself. Dynamic Lights will accept and obey, but not recover should you
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
    public void onTick()
    {
        if (thePlayer != null && thePlayer.isEntityAlive() && !DynamicLights.globalLightsOff)
        {
            List<IMCMessage> messages = FMLInterModComms.fetchRuntimeMessages(this);
            if (messages.size() > 0)
            {
                // just get the last one
                IMCMessage imcMessage = messages.get(messages.size()-1);
                if (imcMessage.key.equalsIgnoreCase("forceplayerlighton"))
                {
                    if (!DynamicLights.fmlOverrideEnable)
                    {
                    	DynamicLights.fmlOverrideEnable = true;
                        if (!enabled)
                        {
                            lightLevel = 15;
                            enableLight();
                        }
                    }
                }
                else if (imcMessage.key.equalsIgnoreCase("forceplayerlightoff"))
                {
                    if (DynamicLights.fmlOverrideEnable)
                    {
                    	DynamicLights.fmlOverrideEnable = false;
                        if (enabled)
                        {
                            disableLight();
                        }
                    }
                }
            }
            
            if (!DynamicLights.fmlOverrideEnable)
            {
                int prevLight = lightLevel;
                
                ItemStack item = thePlayer.getCurrentEquippedItem();
                lightLevel = Config.itemsMap.getLightFromItemStack(item);
                
                for (ItemStack armor : thePlayer.inventory.armorInventory)
                {
                    lightLevel = DynamicLights.maxLight(lightLevel, Config.itemsMap.getLightFromItemStack(armor));
                }
                
                if (prevLight != 0 && lightLevel != prevLight)
                {
                    lightLevel = 0;
                }
                else
                {
                    if (thePlayer.isBurning())
                    {
                        lightLevel = 15;
                    }
                    else
                    {
                        if (checkPlayerWater(thePlayer)
                        && item != null
                        && Config.notWaterProofItems.retrieveValue(GameData.getItemRegistry().getNameForObject(item.getItem()), item.getMetadata()) == 1)
                        {
                            lightLevel = 0;
                            
                            for (ItemStack armor : thePlayer.inventory.armorInventory)
                            {
                                if (armor != null && Config.notWaterProofItems.retrieveValue(GameData.getItemRegistry().getNameForObject(armor.getItem()), item.getMetadata()) == 0)
                                {
                                    lightLevel = DynamicLights.maxLight(lightLevel, Config.itemsMap.getLightFromItemStack(armor));
                                }
                            }
                        }
                    }
                }

                this.checkForchange();
            }
        }
    }
    
    private boolean checkPlayerWater(EntityPlayer thePlayer)
    {
        if (thePlayer.isInWater())
        {
            int x = MathHelper.floor_double(thePlayer.posX + 0.5D);
            int y = MathHelper.floor_double(thePlayer.posY + thePlayer.getEyeHeight());
            int z = MathHelper.floor_double(thePlayer.posZ + 0.5D);
            return thePlayer.worldObj.getBlock(x, y, z).getMaterial() == Material.water;
        }
        return false;
    }
        
    @Override
    public void disableLight()
    {
        if (!DynamicLights.fmlOverrideEnable)
        {
            DynamicLights.removeLightSource(this);
            enabled = false;
        }
    }

}
