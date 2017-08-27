package atomicstryker.dynamiclights.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import atomicstryker.dynamiclights.client.adaptors.BaseAdaptor;
import atomicstryker.dynamiclights.client.adaptors.BrightAdaptor;
import atomicstryker.dynamiclights.client.adaptors.CreeperAdaptor;
import atomicstryker.dynamiclights.client.adaptors.EntityBurningAdaptor;
import atomicstryker.dynamiclights.client.adaptors.EntityItemAdaptor;
import atomicstryker.dynamiclights.client.adaptors.FloodLightAdaptor;
import atomicstryker.dynamiclights.client.adaptors.FloodLightAdaptor.DummyEntity;
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
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.world.WorldEvent.Load;

public class ForgeEventHandler 
{    
    
    @SubscribeEvent
    public void onWorldLoad(Load event)
    {
        if (event.world.isRemote)
        {
            DynamicLights.trackedAdaptors = new HashSet<BaseAdaptor>();
            DynamicLights.trackedEntities = new HashSet<Entity>();        
        }
    }

    @SubscribeEvent
    public void onDebugOverlay(RenderGameOverlayEvent.Text event)
    {
        if(Minecraft.getMinecraft().gameSettings.showDebugInfo)
        {
            int lightCount = 0;
            for (ArrayList<DynamicLightSourceContainer> chunks : DynamicLights.worldLightsMap.get(Minecraft.getMinecraft().theWorld).values())
            {
                lightCount += chunks.size();
            }
            event.left.add("DL A:" + DynamicLights.trackedAdaptors.size() 
            + " E:" + DynamicLights.trackedEntities.size()
            + " C:" + DynamicLights.worldLightsMap.get(Minecraft.getMinecraft().theWorld).size()
            + " L:" + lightCount);            
        }
    }

    @SubscribeEvent
    public void onEnteringChunk(EnteringChunk event) 
    {                
        Entity entity = event.entity;
        if(entity == null || !entity.isEntityAlive())
            return;

        World world = entity.worldObj;
        if(world == null || !world.isRemote)
            return;

        //only interested if something actually changed
        if (event.newChunkX == event.oldChunkX && event.newChunkZ == event.oldChunkZ)
            return;

        //if we are not already tracking this entity then we are not interested
        //EntityID of the player gets changed so we have to track that differently
        if (!(entity instanceof EntityPlayer) && !(entity instanceof DummyEntity) && !DynamicLights.trackedEntities.contains(entity))
            return;
        
        ArrayList<DynamicLightSourceContainer> lightListForOldChunk = DynamicLights.getLightListForChunkXZ(world, event.oldChunkX, event.oldChunkZ);        
        DynamicLightSourceContainer lightSource = null;        
        Iterator<DynamicLightSourceContainer> iter = lightListForOldChunk.iterator();
        while (iter.hasNext())
        {
            lightSource = iter.next();
            if (lightSource.getLightSource().getAttachmentEntity() == entity)
            {
                //remove it from the old list
                iter.remove();
                //add it to the new one
                DynamicLights.getLightListForChunkXZ(world, event.newChunkX, event.newChunkZ).add(lightSource);            
                break;
            }
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
        
        if (entity instanceof EntityItem)
        {
            if(!Config.lightDroppedItems)
                return;

            EntityItemAdaptor adaptor = new EntityItemAdaptor((EntityItem)entity);
            adaptor.onTick();
            DynamicLights.trackedAdaptors.add(adaptor);
            DynamicLights.trackedEntities.add(entity);
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
                DynamicLights.trackedAdaptors.add(adaptor);
                DynamicLights.trackedEntities.add(entity);
            }
            else if (minLight > 0)
            {
                BrightAdaptor adaptor = new BrightAdaptor(entity, minLight);
                adaptor.onTick();
                DynamicLights.trackedAdaptors.add(adaptor);
                DynamicLights.trackedEntities.add(entity);
            }

            if (Config.lightMobEquipment)
            {
                MobLightAdaptor adaptor = new MobLightAdaptor((EntityLivingBase)entity);
                adaptor.onTick();
                DynamicLights.trackedAdaptors.add(adaptor);
                DynamicLights.trackedEntities.add(entity);
            }

        }    
        else if (entity instanceof EntityArrow || entity instanceof EntityFireball)
        {
            if(!Config.lightFlamingArrows)
                return;

            EntityBurningAdaptor adaptor = new EntityBurningAdaptor(entity);
            adaptor.onTick();
            DynamicLights.trackedAdaptors.add(adaptor);
            DynamicLights.trackedEntities.add(entity);
        }
        else if (entity instanceof EntityXPOrb)
        {
            if(!Config.lightXP)
                return;

            BrightAdaptor adaptor = new BrightAdaptor(entity, 10);
            adaptor.onTick();
            DynamicLights.trackedAdaptors.add(adaptor);
            DynamicLights.trackedEntities.add(entity);
        }
        else if (entity instanceof EntityOtherPlayerMP)
        {
            if(!Config.lightOtherPlayers)
                return;

            PlayerOtherAdaptor adaptor = new PlayerOtherAdaptor((EntityOtherPlayerMP)entity);
            adaptor.onTick();
            DynamicLights.trackedAdaptors.add(adaptor);
            DynamicLights.trackedEntities.add(entity);
        }
        else if (entity instanceof EntityClientPlayerMP)
        {
            if (Config.lightFloodLight)
            {
                FloodLightAdaptor adaptor = new FloodLightAdaptor(entity, Config.simpleMode);
                adaptor.onTick();
                DynamicLights.trackedAdaptors.add(adaptor);
                DynamicLights.trackedEntities.add(entity);
            }

            if (!Config.lightThisPlayer)
                return;

            DynamicLights.thePlayer = new PlayerSelfAdaptor((EntityPlayer)entity);
            DynamicLights.thePlayer.onTick();
            DynamicLights.trackedAdaptors.add(DynamicLights.thePlayer);
            DynamicLights.trackedEntities.add(entity);

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
            if (event.entity.isEntityAlive())
            {
                CreeperAdaptor creeper = new CreeperAdaptor((EntityCreeper) event.entity);
                creeper.onTick();
                DynamicLights.addLightSource(creeper);
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
