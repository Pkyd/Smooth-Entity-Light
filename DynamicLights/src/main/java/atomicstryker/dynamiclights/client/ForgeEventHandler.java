package atomicstryker.dynamiclights.client;

import atomicstryker.dynamiclights.client.adaptors.BrightAdaptor;
import atomicstryker.dynamiclights.client.adaptors.CreeperAdaptor;
import atomicstryker.dynamiclights.client.adaptors.EntityBurningAdaptor;
import atomicstryker.dynamiclights.client.adaptors.EntityItemAdaptor;
import atomicstryker.dynamiclights.client.adaptors.FloodLightAdaptor;
import atomicstryker.dynamiclights.client.adaptors.MobLightAdaptor;
import atomicstryker.dynamiclights.client.adaptors.PlayerOtherAdaptor;
import atomicstryker.dynamiclights.client.adaptors.PlayerSelfAdaptor;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;

public class ForgeEventHandler {

    @SubscribeEvent
    public void onJoinWorld(EntityJoinWorldEvent event)
    {
        if(!event.world.isRemote)
            return;

        Entity entity = event.entity;

        if (!entity.isEntityAlive())
            return;
        
        if (entity instanceof EntityItem)
        {
            if(!Config.lightDroppedItems)
                return;

            EntityItemAdaptor adapter = new EntityItemAdaptor((EntityItem)entity);
            adapter.onTick();
            DynamicLights.trackedEntities.add(adapter);           
        }
        else if (entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer))
        {
            int minLight = 0;
            boolean catchesFire = false;

            if (Config.lightBurningEntities)
            {
                if (!DynamicLights.lightValueMap.containsKey(entity.getClass()))
                {
                    boolean value = Config.getMobFire(entity.getClass().getSimpleName());
                    
                    DynamicLights.lightValueMap.put(entity.getClass(), value);
                    catchesFire = value;
                }
                else
                {
                    catchesFire = DynamicLights.lightValueMap.get(entity.getClass());
                }
            }

            if (Config.lightGlowingEntities)
            {
                if (!DynamicLights.glowValueMap.containsKey(entity.getClass()))
                {
                    int value = Config.getMobGlow(entity);
                    DynamicLights.glowValueMap.put(entity.getClass(), value);
                    minLight = value;
                }
                else
                {
                    minLight = DynamicLights.glowValueMap.get(entity.getClass());
                }

            }           

            if (catchesFire)
            {
                EntityBurningAdaptor adaptor = new EntityBurningAdaptor(entity);
                adaptor.minLight = minLight;
                adaptor.onTick();
                DynamicLights.trackedEntities.add(adaptor);
            }
            else if (minLight > 0)
            {
                BrightAdaptor adaptor = new BrightAdaptor(entity, minLight);
                adaptor.onTick();
                DynamicLights.trackedEntities.add(adaptor);               
            }

            if (Config.lightMobEquipment)
            {
                MobLightAdaptor adapter = new MobLightAdaptor((EntityLivingBase)entity);
                adapter.onTick();
                DynamicLights.trackedEntities.add(adapter);               
            }

        }    
        else if (entity instanceof EntityArrow || entity instanceof EntityFireball)
        {
            if(!Config.lightFlamingArrows)
                return;

            EntityBurningAdaptor adapter = new EntityBurningAdaptor(entity);
            adapter.onTick();
            DynamicLights.trackedEntities.add(adapter);
        }
        else if (entity instanceof EntityXPOrb)
        {
            if(!Config.lightXP)
                return;

            BrightAdaptor adapter = new BrightAdaptor(entity, 10);
            adapter.onTick();
            DynamicLights.trackedEntities.add(adapter);
        }
        else if (entity instanceof EntityOtherPlayerMP)
        {
            if(!Config.lightOtherPlayers)
                return;

            PlayerOtherAdaptor adapter = new PlayerOtherAdaptor((EntityOtherPlayerMP)entity);
            adapter.onTick();
            DynamicLights.trackedEntities.add(adapter);
        }
        else if (entity instanceof EntityPlayerSP)
        {
            if (Config.lightFloodLight)
            {
                FloodLightAdaptor adaptor = new FloodLightAdaptor(entity, Config.simpleMode);
                adaptor.onTick();
                DynamicLights.trackedEntities.add(adaptor);
            }

            if (!Config.lightThisPlayer)
                return;

            DynamicLights.thePlayer = new PlayerSelfAdaptor((EntityPlayer)entity);
            DynamicLights.thePlayer.onTick();
            DynamicLights.trackedEntities.add(DynamicLights.thePlayer);

            checkForOptifine();         

        }
        else
        {
            //Do nothing
        }

    }
    
    private void checkForOptifine() 
    {
        if (FMLClientHandler.instance().hasOptifine() && !Config.optifineOverride)
        {
            DynamicLights.mcinstance.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("Optifine is loaded.  Disabling Atomic Stryker's Dynamic Lights.  Check the config file to override."));         
            DynamicLights.globalLightsOff = true;
        }
    }
    
    @SubscribeEvent
    public void onPlaySoundAtEntity(PlaySoundAtEntityEvent event)
    {
        if (Config.lightChargingCreepers && event.name != null && event.name.equals("creeper.primed") && event.entity != null && event.entity instanceof EntityCreeper)
        {
            if (event.entity.isEntityAlive())
            {
                CreeperAdaptor creeper = new CreeperAdaptor((EntityCreeper) event.entity);
                creeper.onTick();
                DynamicLights.addLightSource(creeper);
            }
        }
    }

}
