package atomicstryker.dynamiclights.client;

import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChatComponentText;

public class FMLEventHandler {
		
    @SuppressWarnings("unchecked")
	@SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent tick) {
        ClientProxy.mcinstance.mcProfiler.startSection("dynamicLightsTick");
        if (tick.phase == Phase.END && ClientProxy.mcinstance.theWorld != null) {
        	
        		boolean forceUpdate = false;

        		//Check for global lights key press
            if (ClientProxy.mcinstance.currentScreen == null && ClientProxy.toggleButton.getIsKeyPressed()
                    && System.currentTimeMillis() >= ClientProxy.nextKeyTriggerTime) {
            		//key-repeat delay
                ClientProxy.nextKeyTriggerTime = System.currentTimeMillis() + 1000l;
                //toggle the setting
                DynamicLights.globalLightsOff = !DynamicLights.globalLightsOff;
                //player notification
                ClientProxy.mcinstance.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(
                        "Dynamic Lights globally " + (DynamicLights.globalLightsOff ? "off" : "on")));
                forceUpdate = true;
            }

            //check every loaded entity and update any that have light sources
            if (forceUpdate || (!DynamicLights.globalLightsOff && System.currentTimeMillis() >= DynamicLights.nextLightUpdateTime)) {
                DynamicLights.nextLightUpdateTime = System.currentTimeMillis() + Config.updateInterval;

                // Update all light sources on all entities
                List<Entity> entities = ClientProxy.mcinstance.theWorld.loadedEntityList;
                for (Entity entity : entities) {
                    DynamicLightSourceContainer sources = (DynamicLightSourceContainer)entity.getExtendedProperties(DynamicLights.modId);                		
                    if (sources != null) {
                        sources.update();
                    }
                }
            }

        }
        ClientProxy.mcinstance.mcProfiler.endSection();
    }

}
