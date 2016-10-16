package atomicstryker.dynamiclights.client;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import atomicstryker.dynamiclights.client.adaptors.BaseAdaptor;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

public class FMLEventHandler 
{
    
    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent tick)
    {   
        DynamicLights.mcinstance.mcProfiler.startSection("dynamicLightsTick");
        if (tick.phase == Phase.END && DynamicLights.mcinstance.theWorld != null)
        {
            if (!DynamicLights.globalLightsOff && System.currentTimeMillis() >= DynamicLights.nextLightUpdateTime)
            {
                DynamicLights.nextLightUpdateTime = System.currentTimeMillis() + Config.updateInterval;

                //Update all the adaptors
                Iterator<BaseAdaptor> entIter = DynamicLights.trackedEntities.iterator();
                BaseAdaptor adaptor;

                while (entIter.hasNext())
                {
                    adaptor = entIter.next();
                    if(adaptor.getAttachmentEntity().isDead)
                    {
                        adaptor.disableLight();
                        entIter.remove();
                    }
                    else
                    {                   
                        adaptor.onTick();
                    }
                }

                //Update all the lights we found
                ConcurrentLinkedQueue<DynamicLightSourceContainer> worldLights = DynamicLights.worldLightsMap.get(DynamicLights.mcinstance.theWorld);

                if (worldLights != null)
                {
                    Iterator<DynamicLightSourceContainer> iter = worldLights.iterator();
                    while (iter.hasNext())
                    {
                        DynamicLightSourceContainer tickedLightContainer = iter.next();
                        if (tickedLightContainer.onUpdate())
                        {
                            iter.remove();
                            DynamicLights.mcinstance.theWorld.updateLightByType(EnumSkyBlock.Block, tickedLightContainer.getX(), tickedLightContainer.getY(), tickedLightContainer.getZ());
                            //System.out.println("Dynamic Lights killing off LightSource on dead Entity "+tickedLightContainer.getLightSource().getAttachmentEntity());
                        }
                    }
                }

            }

            if (DynamicLights.mcinstance.currentScreen == null && DynamicLights.toggleButton.getIsKeyPressed() && System.currentTimeMillis() >= DynamicLights.nextKeyTriggerTime)
            {
                DynamicLights.nextKeyTriggerTime = System.currentTimeMillis() + 1000l;
                DynamicLights.globalLightsOff = !DynamicLights.globalLightsOff;
                DynamicLights.mcinstance.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("Dynamic Lights globally " + (DynamicLights.globalLightsOff ? "off" : "on")));

                World world = DynamicLights.mcinstance.theWorld;
                if (world != null)
                {
                    ConcurrentLinkedQueue<DynamicLightSourceContainer> worldLights = DynamicLights.worldLightsMap.get(DynamicLights.mcinstance.theWorld);

                    if (worldLights != null)
                    {
                        Iterator<DynamicLightSourceContainer> iter = worldLights.iterator();
                        while (iter.hasNext())
                        {
                            DynamicLightSourceContainer c = iter.next();
                            world.updateLightByType(EnumSkyBlock.Block, c.getX(), c.getY(), c.getZ());
                        }
                    }
                }
            }
        }
        DynamicLights.mcinstance.mcProfiler.endSection();
    }
    


}
