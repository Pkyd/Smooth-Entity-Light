package lakmoore.sel.capabilities;

import java.util.ArrayList;

import lakmoore.sel.client.adaptors.BaseAdaptor;
import net.minecraft.util.math.BlockPos;

public interface ILightSourceCapability {

	public void addLightSource(BaseAdaptor adaptor);
	
	public boolean hasSources();
	
	public int getLastLightLevel();
	
	public ArrayList<BlockPos> getBlocksToUpdate();
	
	public boolean isUnderwater();
	
	public void destroy();

}
