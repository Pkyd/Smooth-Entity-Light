package lakmoore.sel.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import lakmoore.sel.capabilities.DummyChunkCache;
import lakmoore.sel.capabilities.ILightSourceCapability;
import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.world.DirtyRayTrace;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

public class LightUtils {
	
	public static final Predicate<Entity> HAS_ENTITY_LIGHT = new Predicate<Entity>()
	{
	    public boolean apply(@Nullable Entity entity)
	    {
	    	//TODO: check is .isPresent() is slow!?
	        return entity != null && entity.getCapability(SEL.LIGHT_SOURCE_CAPABILITY, null).isPresent();
	    }
	};	
	
    public static int getCombinedLight(short smoothBlockLight0to240, int combinedLight) {
        if (smoothBlockLight0to240 > 0) {
        	int blockLight = combinedLight & 0xFF;
        	if (smoothBlockLight0to240 > blockLight) {
                combinedLight &= 0xFFFF0000;	// Remove the old Block Light
                combinedLight |= (smoothBlockLight0to240 & 0xFF);		// Add in the new Block Light
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
     *  generated smooth light value at the given VERTEX location
     */
	public static short getEntityLightLevel(IBlockReader world, BlockPos pos, float partialTicks) {
		float maxLight = 0.0f;

        if (SEL.disabled || world == null || world instanceof WorldServer) {
            return 0;
        }

        if (world instanceof World) {
//        	SEL.mcProfiler.startSection(SEL.modId + ":getEntityLightLevel");
        	
        	int opacity = ((World) world).getBlockState(pos).getOpacity(world, pos);

        	// If this block is fully opaque it cannot have a light value!
        	if (opacity > 14) {
        		return 0;
        	}
        	
        	DirtyRayTrace rayTrace = new DirtyRayTrace(world);
        	Vec3d target = new Vec3d(pos); 
        	target = target.add(0.5, 0.5, 0.5);
        
            AxisAlignedBB aabb = new AxisAlignedBB(pos).grow(SEL.maxLightDist);
            
			List<Entity> entities = ((World) world).getEntitiesWithinAABB(Entity.class, aabb, HAS_ENTITY_LIGHT);
            for (Entity entity : entities) {
            	
            	ILightSourceCapability sources = entity.getCapability(SEL.LIGHT_SOURCE_CAPABILITY, null).orElse(null);        
            	if (sources == null) continue;
            	            			
            	float lightValue =  sources.getLastLightLevel();                                 
                if (lightValue <= 0) continue;
                
                // if the entity is within a slightly opaque block (think liquids)
                lightValue = Math.max(Math.min(lightValue - opacity, 15.0f), 0.0f);
                if (lightValue <= 0) continue;
                                
                double distSq = target.squareDistanceTo(entity.getEyePosition(partialTicks));
                                
                // light travels less far under water
                if (sources.isUnderwater()) { // && !Config.isClearWater()) {
                    distSq *= 2.0;
                }
                
                if (distSq > SEL.maxLightDistSq) continue;   //7.5 * 7.5 = 56.25 | 15 * 15 = 255
                
                // Cannot avoid this square root if we want light to fade linearly
                lightValue = ((1.0f - ((float)Math.sqrt(distSq) / SEL.maxLightDist)) * lightValue);                
                
                if (lightValue <= maxLight) continue;
                
                // stop light from travelling through (or around) opaque blocks
            	lightValue -= rayTrace.rayTraceForOpacity(entity.getEyePosition(partialTicks), target);
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
        return (short)Math.round(maxLight);
    }
	
    /*
     *  Method that searches for nearby Smooth Entity Light sources and calculates the 
     *  smooth light value at the given location being generated from them
     */
	public static short getEntityLightLevel(IBlockReader world, List<Entity> interestingEntities, Vec3d vertexPos, float partialTicks) {
		float maxLight = 0.0f;

        if (SEL.disabled || world == null || world instanceof WorldServer) {
            return 0;
        }

        if (world instanceof World) {
//        	SEL.mcProfiler.startSection(SEL.modId + ":getEntityLightLevel");
        	        	
        	DirtyRayTrace rayTrace = new DirtyRayTrace(world);
        	Vec3d target = vertexPos; 
                    
            for (Entity entity : interestingEntities) {
            	ILightSourceCapability sources = entity.getCapability(SEL.LIGHT_SOURCE_CAPABILITY, null).orElse(null);        
            	if (sources == null) continue;
            	
                float lightValue = sources.getLastLightLevel();                
                if (lightValue <= 0) continue;
                                                
                double distSq = target.squareDistanceTo(entity.getEyePosition(partialTicks));
                                
                // light travels less far under water
                if (sources.isUnderwater()) { // && !Config.isClearWater()) {
                    distSq *= 2.0;
                }
                
                if (distSq > SEL.maxLightDistSq) continue;   //7.5 * 7.5 = 56.25 | 15 * 15 = 255
                
                // Cannot avoid this square root if we want light to fade linearly
                lightValue = ((1.0f - ((float)Math.sqrt(distSq) / SEL.maxLightDist)) * lightValue);                
                
                if (lightValue <= maxLight) continue;
                
                // stop light from travelling through (or around) opaque blocks
            	lightValue -= rayTrace.rayTraceForOpacity(entity.getEyePosition(partialTicks), target);
                
                // If this source is not the brightest we have seen, test the next
                if (lightValue <= maxLight) continue;
                               
                maxLight = lightValue;
                //maxLight = maxLight(lightValue, maxLight);

                if (maxLight >= 15f)
                	break;
            }

//            SEL.mcProfiler.endSection();
        }
                
        return (short) Math.min(0xF0, Math.round(16f * maxLight));
    }
		    
    public static ArrayList<BlockPos> getVolumeForRelight(BlockPos pos, int radius) {
    	ArrayList<BlockPos> result = new ArrayList<BlockPos>();
    	
    	radius++;

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
        			if (pos.distanceSq(pos1) < SEL.maxLightDistSq) {
            			result.add(pos1);        				        				
        			}
    	    	}
    		}            			
		}
		
		return result;
    }
    
    public static boolean hasLineOfSight(BlockPos blockPos, Vec3d camPos) {
    	RayTraceResult rtr = ClientProxy.mcinstance.world.rayTraceBlocks(
			new Vec3d(blockPos).add(0.5, 0.5, 0.5), 
			camPos
		);
		return rtr == null || rtr.type != RayTraceResult.Type.BLOCK;  
    }
    
    public static <T extends Entity> List<T> getEntitiesWithinAABB(Class <? extends T > clazz, AxisAlignedBB aabb, @Nullable Predicate <? super T > filter)
    {
        int j2 = MathHelper.floor((aabb.minX - ClientProxy.mcinstance.world.getMaxEntityRadius()) / 16.0D);
        int k2 = MathHelper.ceil((aabb.maxX + ClientProxy.mcinstance.world.getMaxEntityRadius()) / 16.0D);
        int l2 = MathHelper.floor((aabb.minZ - ClientProxy.mcinstance.world.getMaxEntityRadius()) / 16.0D);
        int i3 = MathHelper.ceil((aabb.maxZ + ClientProxy.mcinstance.world.getMaxEntityRadius()) / 16.0D);
        List<T> list = Lists.<T>newArrayList();

        for (int j3 = j2; j3 < k2; ++j3)
        {
            for (int k3 = l2; k3 < i3; ++k3)
            {
            	LightUtils.getEntitiesOfTypeWithinAABB(ClientProxy.mcinstance.world.getChunk(j3, k3), clazz, aabb, list, filter);
            }
        }

        return list;
    }
    
    /**
     * Gets all entities that can be assigned to the specified class.
     */
    public static <T extends Entity> void getEntitiesOfTypeWithinAABB(Chunk chunk, Class <? extends T > entityClass, AxisAlignedBB aabb, List<T> listToFill, Predicate <? super T > filter)
    {
        int i = MathHelper.floor((aabb.minY - ClientProxy.mcinstance.world.getMaxEntityRadius()) / 16.0D);
        int j = MathHelper.floor((aabb.maxY + ClientProxy.mcinstance.world.getMaxEntityRadius()) / 16.0D);
        i = MathHelper.clamp(i, 0, chunk.getEntityLists().length - 1);
        j = MathHelper.clamp(j, 0, chunk.getEntityLists().length - 1);

        for (int k = i; k <= j; ++k)
        {
        	Iterator<Entity> iter = chunk.getEntityLists()[k].iterator();
            while (iter.hasNext())
            {
            	T entity = (T)iter.next();
                if (entity.getBoundingBox().intersects(aabb) && (filter == null || filter.apply(entity)))
                {
                    listToFill.add(entity);
                }
            }
        }
    }
    
    public static ILitChunkCache getLitChunkCache(World world, int chunkX, int chunkZ) {
    	
    	if (world != null) {
        	Chunk chunk = world.getChunk(chunkX, chunkZ);
        	ILitChunkCache litChunkCache = chunk.getCapability(SEL.LIT_CHUNK_CACHE_CAPABILITY, null).orElse(new DummyChunkCache());;                		
        	return litChunkCache;    		
    	}
    	// If the lightworker is still running but the world is not loaded
    	return new DummyChunkCache();
    }
    
    private static Boolean hasOptifine;
    public static boolean hasOptifine() {
        if (hasOptifine == null) {
            try {
                hasOptifine = Class.forName("optifine.OptiFineTweaker") != null;
            } catch (ClassNotFoundException e) {
                hasOptifine = false;
            }
        }
        return hasOptifine;
    }
            
}
