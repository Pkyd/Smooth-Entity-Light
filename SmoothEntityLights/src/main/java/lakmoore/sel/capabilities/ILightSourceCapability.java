package lakmoore.sel.capabilities;

import java.util.Set;

import lakmoore.sel.client.adaptors.BaseAdaptor;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public interface ILightSourceCapability {

	public void addLightSource(BaseAdaptor adaptor);
	
	public boolean hasSources();
	
	public Entity getEntity();

	public int getLastLightLevel();
	
	public Set<BlockPos> getBlocksToUpdate();

	public boolean isUnderwater();
	
	public void destroy();

}
