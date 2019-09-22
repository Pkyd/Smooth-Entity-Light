package lakmoore.sel.client.adaptors;

import lakmoore.sel.client.Config;
import lakmoore.sel.client.LightUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

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

	private int getEquipmentLightLevel(EntityLivingBase ent)
	{
		if (ent instanceof EntityHorse)
		{
			// Horse armor texture is the only thing "visible" on client, inventory is not synced.
			// Armor layer is at index 2 in texture layers
			String horseArmorTexture = ((EntityHorse)ent).getVariantTexturePaths()[2];
			if (horseArmorTexture != null)
			{
				if (horseArmorTexture.equals("textures/entity/horse/armor/horse_armor_gold.png"))
				{
					return Config.itemsMap.getLightFromItemStack(new ItemStack(Items.GOLDEN_HORSE_ARMOR)); // horsearmorgold
				}
				if (horseArmorTexture.equals("textures/entity/horse/armor/horse_armor_iron.png"))
				{
					return Config.itemsMap.getLightFromItemStack(new ItemStack(Items.IRON_HORSE_ARMOR)); // horsearmormetal
				}
				if (horseArmorTexture.equals("textures/entity/horse/armor/horse_armor_diamond.png"))
				{
					return Config.itemsMap.getLightFromItemStack(new ItemStack(Items.DIAMOND_HORSE_ARMOR)); // butt stallion
				}
			}
		}

		return getMobEquipMaxLight(ent);
	}

	private int getMobEquipMaxLight(EntityLivingBase ent)
	{
		int light = 0;
		for (ItemStack item : ent.getEquipmentAndArmor())
		{
			light = LightUtils.maxLight(light, Config.itemsMap.getLightFromItemStack(item));
		}
		return light;
	}

	@Override
	public int getLightLevel()
	{            
		// infernal mobs yay
		if (entity.getEntityData().getString("InfernalMobsMod").length() > 0)
		{
			return 15;
		}
		else
			return getEquipmentLightLevel((EntityLivingBase)entity);
	}

}
