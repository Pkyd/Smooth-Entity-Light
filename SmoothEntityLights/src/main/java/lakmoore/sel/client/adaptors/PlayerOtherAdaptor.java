package lakmoore.sel.client.adaptors;

import lakmoore.sel.client.Config;
import lakmoore.sel.client.LightUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class PlayerOtherAdaptor extends BaseAdaptor
    {        
        private PlayerEntity player;
        
        public PlayerOtherAdaptor(PlayerEntity p)
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
                int lightLevel = 0;
            	for (ItemStack stack : player.getEquipmentAndArmor()) {
                    lightLevel = LightUtils.maxLight(lightLevel, Config.lightValueMap.get(stack.getItem().getRegistryName()));        		
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
