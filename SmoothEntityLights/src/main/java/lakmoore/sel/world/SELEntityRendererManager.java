package lakmoore.sel.world;

import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.client.ClientProxy;
import lakmoore.sel.client.LightUtils;
import lakmoore.sel.client.SEL;
import net.minecraft.client.GameSettings;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.math.BlockPos;

public class SELEntityRendererManager extends EntityRendererManager {
	
	public SELEntityRendererManager(			
	   TextureManager textureManagerIn, 
	   ItemRenderer itemRendererIn, 
	   IReloadableResourceManager resourceManagerIn, 
	   FontRenderer fontRendererIn, 
	   GameSettings gameSettingsIn
	) {
		super(textureManagerIn, itemRendererIn, resourceManagerIn, fontRendererIn, gameSettingsIn);
	}
	
	@Override
	
	public <E extends Entity> int getPackedLight(E entityIn, float partialTicks) {
//		SEL.mcProfiler.startSection(SEL.modId + ":getLightFor");
		int packedLight = this.getRenderer(entityIn).getPackedLight(entityIn, partialTicks);
		int blockLight = packedLight & 0xFF;

		if (!SEL.disabled && SEL.enabledForDimension(ClientProxy.mcinstance.player.dimension)
				&& blockLight < 0xF0) {
			BlockPos entityPos = new BlockPos(entityIn.getEyePosition(partialTicks));
			ILitChunkCache lc = LightUtils.getLitChunkCache(ClientProxy.mcinstance.world, entityPos.getX() >> 4, entityPos.getZ() >> 4);
			if (lc != null) {
				// This is needed to light entities in the world with entity light
				short entityLight = lc.getBlockLight(entityPos.getX(), entityPos.getY(), entityPos.getZ());
				if (entityLight > blockLight) {
					packedLight &= 0xFFFF0000;	// take the old blocklight out
					packedLight |= entityLight;	// add in the new blocklight
				}
			}
		}

//		SEL.mcProfiler.endSection();
		return packedLight;
	}
		
//	@Override
//    public boolean setBlockState(BlockPos pos, BlockState newState, int flags) {
//		// If the change is supposed to cause a re-render 
//		if ((flags & 0x4) == 0) {
//			// search the immediately surrounding blocks
//	        for (BlockPos nearPos : BlockPos.getAllInBoxMutable(pos.add(-1, -1, -1), pos.add(1, 1, 1)))
//	        {	        	
//	        	// for ANY entity light
//				ILitChunkCache lc = LightUtils.getLitChunkCache(this, nearPos.getX() >> 4, nearPos.getZ() >> 4);
//				if (lc != null) {
//					short lightPlayer = lc.getBlockLight(nearPos.getX(), nearPos.getY(), nearPos.getZ());
//
//					if (lightPlayer > 0) {
//		        		// as soon as we see some, mark ALL the surrounding blocks in our radius as "dirty"
//						SEL.lightWorker.reLightAVolume(pos, SEL.maxLightDist);
//						// stop our search
//						break;	        		
//					}
//	        	}
//	        }					
//		}
//		return super.setBlockState(pos, newState, flags);    	
//    }

}
