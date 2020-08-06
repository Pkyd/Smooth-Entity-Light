package lakmoore.sel.client.adaptors;

import lakmoore.sel.client.Config;
import lakmoore.sel.client.LightUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * 
 * @author AtomicStryker, LakMoore
 *
 * Offers Entity Light functionality to EntityLiving instances, or rather their respective
 * armor and held Itemstacks. Lights up golden armor and torch Zombies
 *
 */
public class MobLightAdaptor extends BaseAdaptor
{

	public MobLightAdaptor(Entity entity) {
		super(entity);
	}

	private int getEquipmentLightLevel(LivingEntity ent)
	{
		if (ent instanceof HorseEntity)
		{
			if (((HorseEntity)ent).wearsArmor()) {
				ItemStack horseArmor = ((HorseEntity)ent).func_213803_dV();
				if (horseArmor != null && !horseArmor.isEmpty())
				{
					return Config.lightValueMap.getOrDefault(horseArmor.getItem().getRegistryName(), 0);
				}				
			}
		}

		return getMobEquipMaxLight(ent);
	}

	private int getMobEquipMaxLight(LivingEntity ent)
	{
		int light = 0;
		for (ItemStack stack : ent.getEquipmentAndArmor())
		{
			if (!stack.isEmpty()) {				
				light = LightUtils.maxLight(light, Config.lightValueMap.getOrDefault(stack.getItem().getRegistryName().toString(), 0));
			}
		}
		return light;
	}

	@Override
	public int getLightLevel()
	{            
		// atomic stryker's infernal mobs - yay
		if (entity.getPersistentData().getString("InfernalMobsMod").length() > 0)
		{
			return 10;
		}
		else
			return getEquipmentLightLevel((LivingEntity)entity);
	}

}
