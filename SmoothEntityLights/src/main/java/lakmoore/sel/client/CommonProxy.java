package lakmoore.sel.client;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class CommonProxy 
{

    public void setupCommon(FMLCommonSetupEvent evt) {       
    	// First event - not in the main thread
    	// Registry is now valid
    	// Things to do:  
    	//      Creating and reading the config files
    	//      Registering Capabilities

    }

    public void setupClient(FMLClientSetupEvent evt) {
    	// Second event (sided) - not in the main thread
    	// Client only - Do Keybindings???

    }

}
