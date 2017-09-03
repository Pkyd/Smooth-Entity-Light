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
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;

public class ForgeEventHandler 
{    
    
    @SubscribeEvent
    public void onDebugOverlay(RenderGameOverlayEvent.Text event)
    {
        if(Minecraft.getMinecraft().gameSettings.showDebugInfo)
        {
        		//There used to be some interesting stats to look at!
        		event.left.add("DL " + (DynamicLights.globalLightsOff ? "OFF" : "ON"));
        }
    }
    
    @SubscribeEvent
    public void onJoinWorld(EntityJoinWorldEvent event)
    {
        Entity entity = event.entity;
        if(entity == null || !entity.isEntityAlive())
            return;

        World world = entity.worldObj;
        if(world == null || !world.isRemote)
            return;
        
        DynamicLightSourceContainer sources = (DynamicLightSourceContainer)entity.getExtendedProperties(DynamicLights.modId);
        if(sources == null) {
        		sources = new DynamicLightSourceContainer(entity, world);
        		entity.registerExtendedProperties(DynamicLights.modId, sources);
        }

        if (entity instanceof EntityItem)
        {
            if(!Config.lightDroppedItems)
                return;

            EntityItemAdaptor adaptor = new EntityItemAdaptor((EntityItem)entity);
            sources.addLightSource(adaptor);
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
                sources.addLightSource(adaptor);
            }
            else if (minLight > 0)
            {
                BrightAdaptor adaptor = new BrightAdaptor(entity, minLight);
                sources.addLightSource(adaptor);
            }

            if (Config.lightMobEquipment)
            {
                MobLightAdaptor adaptor = new MobLightAdaptor((EntityLivingBase)entity);
                sources.addLightSource(adaptor);
            }

        }    
        else if (entity instanceof EntityArrow || entity instanceof EntityFireball)
        {
            if(!Config.lightFlamingArrows)
                return;

            EntityBurningAdaptor adaptor = new EntityBurningAdaptor(entity);
            sources.addLightSource(adaptor);
        }
        else if (entity instanceof EntityXPOrb)
        {
            if(!Config.lightXP)
                return;

            BrightAdaptor adaptor = new BrightAdaptor(entity, 10);
            sources.addLightSource(adaptor);
        }
        else if (entity instanceof EntityOtherPlayerMP)
        {
            if(!Config.lightOtherPlayers)
                return;

            PlayerOtherAdaptor adaptor = new PlayerOtherAdaptor((EntityOtherPlayerMP)entity);
            sources.addLightSource(adaptor);
        }
        else if (entity instanceof EntityClientPlayerMP)
        {
            if (Config.lightFloodLight)
            {
                FloodLightAdaptor adaptor = new FloodLightAdaptor(entity, Config.simpleMode);
                sources.addLightSource(adaptor);
            }

            if (!Config.lightThisPlayer)
                return;

            PlayerSelfAdaptor adaptor = new PlayerSelfAdaptor((EntityPlayer)entity);
            sources.addLightSource(adaptor);

            checkForOptifine();
        }
        else
        {
            //Do nothing
        }

    }
    
    @SubscribeEvent
    public void onPlaySoundAtEntity(PlaySoundAtEntityEvent event)
    {
        if (Config.lightChargingCreepers && event.name != null && event.name.equals("creeper.primed") && event.entity != null && event.entity instanceof EntityCreeper)
        {
            if (event.entity != null && event.entity.isEntityAlive())
            {
                DynamicLightSourceContainer sources = (DynamicLightSourceContainer)event.entity.getExtendedProperties(DynamicLights.modId);                		
                if (sources == null)
                		return;
                CreeperAdaptor creeper = new CreeperAdaptor((EntityCreeper) event.entity);
                sources.addLightSource(creeper);
            }
        }
    }
    
    private void checkForOptifine() 
    {
        if (FMLClientHandler.instance().hasOptifine() && !Config.optifineOverride)
        {
            ClientProxy.mcinstance.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("Optifine is loaded.  Disabling Atomic Stryker's Dynamic Lights.  Check the config file to override."));         
            DynamicLights.globalLightsOff = true;
        }
    }

}
