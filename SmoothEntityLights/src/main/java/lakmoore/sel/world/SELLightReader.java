package lakmoore.sel.world;

import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.client.ClientProxy;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import net.minecraft.world.LightType;
import net.minecraft.world.lighting.WorldLightManager;

public interface SELLightReader extends ILightReader {
	WorldLightManager getLightManager();



	default int getLightSubtracted(BlockPos blockPosIn, int amount) {
		return this.getLightManager().getLightSubtracted(blockPosIn, amount);
	}

}