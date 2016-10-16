package atomicstryker.dynamiclights.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.input.Keyboard;

import atomicstryker.dynamiclights.client.adaptors.BaseAdaptor;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy
{
    
    public void preInit(FMLPreInitializationEvent evt)
    {
        
        Config.doConfig(evt.getSuggestedConfigurationFile());
     
        DynamicLights.globalLightsOff = false;
        DynamicLights.mcinstance = FMLClientHandler.instance().getClient();
        DynamicLights.worldLightsMap = new ConcurrentHashMap<World, ConcurrentLinkedQueue<DynamicLightSourceContainer>>();
        DynamicLights.lightValueMap = new HashMap<Class<? extends Entity>, Boolean>();
        DynamicLights.glowValueMap = new HashMap<Class<? extends Entity>, Integer>();
        
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
        FMLCommonHandler.instance().bus().register(new FMLEventHandler());

        DynamicLights.nextKeyTriggerTime = System.currentTimeMillis();
        DynamicLights.nextLightUpdateTime = System.currentTimeMillis();

    }
    
    public void init()
    {
        DynamicLights.trackedEntities = new ArrayList<BaseAdaptor>();
        DynamicLights.toggleButton = new KeyBinding("Dynamic Lights toggle", Keyboard.KEY_L, "key.categories.gameplay");
        ClientRegistry.registerKeyBinding(DynamicLights.toggleButton);
        DynamicLights.coloredLights = Loader.isModLoaded("easycoloredlights");
    }

}
