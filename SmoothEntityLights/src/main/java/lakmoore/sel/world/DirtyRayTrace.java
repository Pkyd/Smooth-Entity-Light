package lakmoore.sel.world;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockReader;

public class DirtyRayTrace {

	private IBlockReader world;

	public DirtyRayTrace(IBlockReader world) {
		this.world = world;
	}

	/*
	 * Return the total opacity of intersected blocks along the ray
	 */
	public int rayTraceForOpacity(Vec3d startPos, Vec3d endPos) {
		
		// start and end Pos could be anywhere, but endPos is probably a Vertex

		int result = 0;

		if (!Double.isNaN(startPos.x) && !Double.isNaN(startPos.y) && !Double.isNaN(startPos.z)) {
			if (!Double.isNaN(endPos.x) && !Double.isNaN(endPos.y) && !Double.isNaN(endPos.z)) {
				
				Vec3d currentPos = new Vec3d(startPos.x, startPos.y, startPos.z);
				BlockRayTraceResult rtr;
				boolean sameBlock = false;
				
				do {
					rtr = blockRayTrace(currentPos, endPos);
					
					if (rtr != null) {												
						currentPos = rtr.getHitVec();

						BlockPos thisBlock = rtr.getPos();
						
						// Get the block we hit
						BlockState state = world.getBlockState(thisBlock);
						if (state != null) {
							Block block = state.getBlock();
							if (block != null && !state.isAir(world, thisBlock)) {
								result += state.getOpacity(world, thisBlock);
							}						
						}													

						sameBlock = (new BlockPos(currentPos)).equals(new BlockPos(endPos));
					}

				} while (result < 15 && rtr != null && !sameBlock);
			}
		}
		return Math.max(0, Math.min(15, result));
	}
	
	/*
	 * A RayTrace routine that I can understand.
	 * Resultant side-hit is not calculated
	 */
	private BlockRayTraceResult blockRayTrace(Vec3d startVec, Vec3d endVec) {
		
		BlockPos startBlockPos = new BlockPos(startVec);
		BlockPos endBlockPos = new BlockPos(endVec);
		
		if (startBlockPos.equals(endBlockPos)) {
			return null;
		}
		
		// Re-base so we are working within the unit cube
        Vec3d fromVec = startVec.subtract(startBlockPos.getX(), startBlockPos.getY(), startBlockPos.getZ());
        Vec3d toVec = endVec.subtract(startBlockPos.getX(), startBlockPos.getY(), startBlockPos.getZ());

        Vec3d hitPoint = null;

        if (toVec.x < 0f && fromVec.x == 0f) {
        	// Shift everything up the x-axis
        	fromVec = fromVec.add(1f, 0f, 0f);
        	toVec = toVec.add(1f, 0f, 0f);
        	startBlockPos = startBlockPos.add(-1, 0, 0);
        }

        if (toVec.y < 0f && fromVec.y == 0f) {
        	// Shift everything up the y-axis
        	fromVec = fromVec.add(0f, 1f, 0f);
        	toVec = toVec.add(0f, 1f, 0f);
        	startBlockPos = startBlockPos.add(0, -1, 0);
        }

        if (toVec.z < 0f && fromVec.z == 0f) {
        	// Shift everything up the z-axis
        	fromVec = fromVec.add(0f, 0f, 1f);
        	toVec = toVec.add(0f, 0f, 1f);
        	startBlockPos = startBlockPos.add(0, 0, -1);
        }

        // Does the vector cross x == 0?
        if (toVec.x < 0f) {
            hitPoint = getIntermediateWithXValue(fromVec, toVec, 0f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.y) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new BlockRayTraceResult(hitPoint, Direction.DOWN, startBlockPos.add(-1, 0, 0), false);                	
                }
            }        	
        }
        
        if (toVec.x > 1.0f) {
            // Does the vector cross x == 1?
            hitPoint = getIntermediateWithXValue(fromVec, toVec, 1f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.y) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new BlockRayTraceResult(hitPoint, Direction.DOWN, startBlockPos.add(1, 0, 0), false);                	
                }
            }        	
        }

        if (toVec.y < 0.0f) {
            // Does the vector cross y == 0?
            hitPoint = getIntermediateWithYValue(fromVec, toVec, 0.0f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new BlockRayTraceResult(hitPoint, Direction.DOWN, startBlockPos.add(0, -1, 0), false);                	
                }
            }        	
        }
        
        if (toVec.y > 1.0f) {
            // Does the vector cross y == 1?
            hitPoint = getIntermediateWithYValue(fromVec, toVec, 1.0f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new BlockRayTraceResult(hitPoint, Direction.UP, startBlockPos.add(0, 1, 0), false);                	
                }
            }        	
        }

        if (toVec.z < 0.0f) {
            // Does the vector cross z == 0?
            hitPoint = getIntermediateWithZValue(fromVec, toVec, 0.0f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.y)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new BlockRayTraceResult(hitPoint, Direction.DOWN, startBlockPos.add(0, 0, -1), false);                	
                }
            }        	
        }
        
        if (toVec.z > 1.0f) {
            // Does the vector cross z == 1?
            hitPoint = getIntermediateWithZValue(fromVec, toVec, 1.0f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.y)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new BlockRayTraceResult(hitPoint, Direction.DOWN, startBlockPos.add(0, 0, 1), false);                	
                }
            }        	
        }
        
        return null;
	}
	
	private boolean inBounds(double coord) {
		return coord >= 0.0f && coord <= 1.0f;
	}
	
	/**
     * Returns a new vector with x value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    @Nullable
    private Vec3d getIntermediateWithXValue(Vec3d from, Vec3d vec, double x)
    {
        double d0 = vec.x - from.x;
        double d1 = vec.y - from.y;
        double d2 = vec.z - from.z;

        if (d0 * d0 < 1.0000000116860974E-7D)
        {
            return null;
        }
        else
        {
            double d3 = (x - from.x) / d0;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vec3d(from.x + d0 * d3, from.y + d1 * d3, from.z + d2 * d3) : null;
        }
    }

    /**
     * Returns a new vector with y value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    @Nullable
    private Vec3d getIntermediateWithYValue(Vec3d from, Vec3d vec, double y)
    {
        double d0 = vec.x - from.x;
        double d1 = vec.y - from.y;
        double d2 = vec.z - from.z;

        if (d1 * d1 < 1.0000000116860974E-7D)
        {
            return null;
        }
        else
        {
            double d3 = (y - from.y) / d1;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vec3d(from.x + d0 * d3, from.y + d1 * d3, from.z + d2 * d3) : null;
        }
    }

    /**
     * Returns a new vector with z value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    @Nullable
    private Vec3d getIntermediateWithZValue(Vec3d from, Vec3d vec, double z)
    {
        double d0 = vec.x - from.x;
        double d1 = vec.y - from.y;
        double d2 = vec.z - from.z;

        if (d2 * d2 < 1.0000000116860974E-7D)
        {
            return null;
        }
        else
        {
            double d3 = (z - from.z) / d2;
            return d3 >= 0.0D && d3 <= 1.0D ? new Vec3d(from.x + d0 * d3, from.y + d1 * d3, from.z + d2 * d3) : null;
        }
    }

}
