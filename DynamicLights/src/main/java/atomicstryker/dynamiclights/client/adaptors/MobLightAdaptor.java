package atomicstryker.dynamiclights.client.adaptors;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import atomicstryker.dynamiclights.client.Config;
import atomicstryker.dynamiclights.client.DynamicLights;

/**
 * 
 * @author AtomicStryker
 *
 * Offers Dynamic Light functionality to EntityLiving instances, or rather their respective
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
					return Config.itemsMap.getLightFromItemStack(new ItemStack(Items.golden_horse_armor)); // horsearmorgold
				}
				if (horseArmorTexture.equals("textures/entity/horse/armor/horse_armor_iron.png"))
				{
					return Config.itemsMap.getLightFromItemStack(new ItemStack(Items.iron_horse_armor)); // horsearmormetal
				}
				if (horseArmorTexture.equals("textures/entity/horse/armor/horse_armor_diamond.png"))
				{
					return Config.itemsMap.getLightFromItemStack(new ItemStack(Items.diamond_horse_armor)); // butt stallion
				}
			}
		}

		return getMobEquipMaxLight(ent);
	}

	private int getMobEquipMaxLight(EntityLivingBase ent)
	{
		int light = Config.itemsMap.getLightFromItemStack(ent.getEquipmentInSlot(0));
		for (int i = 1; i < 4; i++)
		{
			light = DynamicLights.maxLight(light, Config.itemsMap.getLightFromItemStack(ent.getEquipmentInSlot(i)));
		}
		return light;
	}

	public void onTick()
	{            
		// infernal mobs yay
		if (entity.getEntityData().getString("InfernalMobsMod").length() > 0)
		{
			lightLevel = 15;
		}
		else
			lightLevel = getEquipmentLightLevel((EntityLivingBase)entity);

		checkForchange();
	}

}
