package lakmoore.sel.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.world.DirtyRayTrace;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LightUtils {
	
	private static final Predicate<Entity> HAS_ENTITY_LIGHT = new Predicate<Entity>()
	{
	    public boolean apply(@Nullable Entity entity)
	    {
	        return entity != null && entity.hasCapability(SEL.LIGHT_SOURCE_CAPABILITY, null);
	    }
	};
		
	@SideOnly(Side.CLIENT)
	public static HashMap<ChunkPos, LightCache> lightCache = new HashMap<ChunkPos, LightCache>();
	
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
     *  Method that searches for nearby Smooth Entity Light sources and calculates the 
     *  smooth light value at the given location being generated from them
     */
	public static float getEntityLightLevel(IBlockAccess world, BlockPos pos) {
		float maxLight = 0.0f;

        if (SEL.disabled || world == null || world instanceof WorldServer) {
            return 0f;
        }

        if (world instanceof World) {
//        	SEL.mcProfiler.startSection(SEL.modId + ":getEntityLightLevel");
        	
        	int opacity = ((World) world).getBlockLightOpacity(pos);

        	// If this block is fully opaque it cannot have a light value!
        	if (opacity > 14) {
        		return 0f;
        	}
        	
        	DirtyRayTrace rayTrace = new DirtyRayTrace(world);
        	Vec3d target = new Vec3d(pos); 
        	target = target.add(0.5, 0.5, 0.5);
        
            AxisAlignedBB aabb = new AxisAlignedBB(pos);
            aabb = aabb.grow(SEL.maxSearchDist, SEL.maxSearchDist, SEL.maxSearchDist);
            
			List<Entity> entities = ((World) world).getEntitiesWithinAABB(Entity.class, aabb, HAS_ENTITY_LIGHT);
            for (Entity entity : entities) {
            	ILightSourceCapability sources = entity.getCapability(SEL.LIGHT_SOURCE_CAPABILITY, null);                		
                if (sources == null) continue;

                float lightValue = sources.getLastLightLevel();                
                if (lightValue <= 0) continue;
                
                // if the entity is within a slightly opaque block (think liquids)
                lightValue = Math.max(Math.min(lightValue - opacity, 15.0f), 0.0f);
                if (lightValue <= 0) continue;
                                
                double distSq = target.squareDistanceTo(entity.getPositionEyes(1f));
                                
                // light travels less far under water
                if (sources.isUnderwater()) { // && !Config.isClearWater()) {
                    distSq *= 2.0;
                }
                
                if (distSq > 56.25) continue;   //7.5 * 7.5 = 56.25 | 15 * 15 = 255
                
                // Cannot avoid this square root if we want light to fade linearly
                lightValue = ((1.0f - ((float)Math.sqrt(distSq) / 7.5f)) * lightValue);                
                
                if (lightValue <= maxLight) continue;
                
                // stop light from travelling through (or around) opaque blocks
            	lightValue -= rayTrace.rayTraceForOpacity(new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ), target);
        		lightValue = Math.max(0f, Math.min(15f, lightValue));                	
                
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
	    
    public static ArrayList<BlockPos> getVolumeForRelight(BlockPos pos, int radius) {
    	ArrayList<BlockPos> result = new ArrayList<BlockPos>();
        int minX = pos.getX() - radius;
        int minY = pos.getY() - radius;
        int minZ = pos.getZ() - radius;
        int maxX = pos.getX() + radius;
        int maxY = pos.getY() + radius;
        int maxZ = pos.getZ() + radius;

		for (int i = minX; i <= maxX; i++) {
    		for (int j = minY; j <= maxY; j++) {
        		for (int k = minZ; k <= maxZ; k++) {
        			BlockPos pos1 = new BlockPos(i, j, k);
        			result.add(pos1);
    	    	}
    		}            			
		}
		
		return result;
    }
    
}
