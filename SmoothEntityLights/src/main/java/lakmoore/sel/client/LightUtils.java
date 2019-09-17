package lakmoore.sel.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lakmoore.sel.world.BlockPos;
import lakmoore.sel.world.DirtyRayTrace;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class LightUtils {
		
	@SideOnly(Side.CLIENT)
	public static HashMap<ChunkCoordIntPair, LightCache> lightCache = new HashMap<ChunkCoordIntPair, LightCache>();
	
    public static int getCombinedLight(float smoothBlockLight0to15, int combinedLight) {
        if (smoothBlockLight0to15 > 0.0f) {
        	int light8bit = (int)(smoothBlockLight0to15 * 16.0f);
        	int blockLight = combinedLight & 0xFF;
        	if (light8bit > blockLight) {
                combinedLight &= 0xFFFFFF00;	// Remove the old Block Light
                combinedLight |= light8bit;		// Add in the new Block Light
            }        	
        }
        return combinedLight;
    }
    
    /**
     * Compatibility extension for CptSpaceToaster's colored lights mod
     */
    public static int maxLight(int a, int b) {
        if (SEL.coloredLights) {
            if ((((0x100000 | b) - a) & 0x84210) > 0) {
                // some color components of A > B
                return a;
            }
            return b;
        } else {
            return Math.max(a, b);
        }
    }
    

    /*
     *  Method that searches for nearby dynamic light sources and calculates the 
     *  smooth light value at the given location being generated from them
     */
	public static float getEntityLightLevel(IBlockAccess world, int x, int y, int z) {
		float maxLight = 0.0f;

        if (SEL.disabled || world == null || world instanceof WorldServer) {
            return 0f;
        }

        if (world instanceof World) {
//        	SEL.mcProfiler.startSection(SEL.modId + ":getEntityLightLevel");
        	
        	int opacity = ((World) world).getBlock(x, y, z).getLightOpacity();

        	// If this block is fully opaque it cannot have a light value!
        	if(opacity > 14) {
        		return 0f;
        	}
        	
        	DirtyRayTrace rayTrace = new DirtyRayTrace(world);
        	Vec3 target = Vec3.createVectorHelper(x + 0.5f, y + 0.5f, z + 0.5f);        	
        
            AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(
        		x - SEL.maxSearchDist, 
        		y - SEL.maxSearchDist, 
        		z - SEL.maxSearchDist, 
        		x + SEL.maxSearchDist, 
        		y + SEL.maxSearchDist, 
        		z + SEL.maxSearchDist
        	);
            
            @SuppressWarnings("unchecked")
			List<Entity> entities = ((World) world).selectEntitiesWithinAABB(Entity.class, bb, selectEntityLights);
            for (Entity entity : entities) {
                SELSourceContainer sources = (SELSourceContainer)entity.getExtendedProperties(SEL.modId);                		
                if (sources == null) continue;

                float lightValue = sources.getLastLightLevel();                
                if (lightValue <= 0) continue;
                
                // if the entity is within a slightly opaque block (think liquids)
                lightValue = Math.max(Math.min(lightValue - opacity, 15.0f), 0.0f);
                if (lightValue <= 0) continue;
                                
                float dx = (float)x + 0.5f - (float)entity.posX;
                float dy = (float)y + 0.5f - (float)entity.posY + entity.getEyeHeight();
                float dz = (float)z + 0.5f - (float)entity.posZ;
                float distSq = dx * dx + dy * dy + dz * dz;
                                
                // light travels less far under water
                if (sources.isUnderwater()) { // && !Config.isClearWater()) {
                    distSq *= 2.0;
                }
                
                if (distSq > 56.25) continue;   //7.5 * 7.5 = 56.25 | 15 * 15 = 255
                
                // Cannot avoid this square root if we want light to fade linearly
                lightValue = ((1.0f - ((float)Math.sqrt(distSq) / 7.5f)) * lightValue);                
                
                if (lightValue <= maxLight) continue;
                
                // if the source of light is not in this block
//                if (x != (int)entity.posX || y != (int)(entity.posY + entity.getEyeHeight()) || z != (int)entity.posZ) {
                	
                    // stop light from travelling through (or around) opaque blocks
                	ArrayList<Block> blocksHit = rayTrace.rayTraceAllBlocks(Vec3.createVectorHelper(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ), target);

                	for (Block block : blocksHit) {
                		lightValue -= block.getLightOpacity();
                		lightValue = Math.max(0f, Math.min(15f, lightValue));
                        // As soon as we are no longer the brightest light, move on
                        if (lightValue <= maxLight) break;
                	}                	                    
//                }
                
                // If this source is not the brightest we have seen, test the next
                if (lightValue <= maxLight) continue;
                               
                maxLight = lightValue;
                //maxLight = maxLight(lightValue, maxLight);

                if (maxLight == 15)
                	break;
            }

//            SEL.mcProfiler.endSection();
        }
        return maxLight;
    }
	
    public static IEntitySelector selectEntityLights = new IEntitySelector()
    {
        /**
         * Return whether the specified entity is applicable to this filter.
         */
        public boolean isEntityApplicable(Entity entity)
        {
            return entity.getExtendedProperties(SEL.modId) != null;
        }
    };
    
    public static ArrayList<BlockPos> getVolumeForRelight(int x, int y, int z, int radius) {
    	ArrayList<BlockPos> result = new ArrayList<BlockPos>();
        int minX = x - radius;
        int minY = y - radius;
        int minZ = z - radius;
        int maxX = x + radius;
        int maxY = y + radius;
        int maxZ = z + radius;

		for (int i = minX; i <= maxX; i++) {
    		for (int j = minY; j <= maxY; j++) {
        		for (int k = minZ; k <= maxZ; k++) {
        			BlockPos pos = new BlockPos(i, j, k);
        			result.add(pos);
    	    	}
    		}            			
		}
		
		return result;
    }
    
}
