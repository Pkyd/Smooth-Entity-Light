package lakmoore.sel.client.adaptors;

import lakmoore.sel.client.Config;
import lakmoore.sel.client.LightUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class PlayerOtherAdaptor extends BaseAdaptor
    {        
        private EntityPlayer player;
        
        public PlayerOtherAdaptor(EntityPlayer p)
        {
        		super(p);
            player = p;
        }
        
        public int getLightLevel()
        {
            if (player.isBurning())
            {
                return 15;
            }
            else
            {                
                int lightLevel = Config.itemsMap.getLightFromItemStack(player.getCurrentEquippedItem());
                for (ItemStack armor : player.inventory.armorInventory)
                {
                    lightLevel = LightUtils.maxLight(lightLevel, Config.itemsMap.getLightFromItemStack(armor));
                }            	
                return lightLevel;
            }            
        }
        
    	@Override
    	public void kill() {
    		super.kill();
    		player = null;
    	}

             
    }
