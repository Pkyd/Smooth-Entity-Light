package lakmoore.sel.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

public class DirtyRayTrace {

	private IBlockAccess world;

	public DirtyRayTrace(IBlockAccess world) {
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
				RayTraceResult rtr;
				boolean sameBlock = false;
				
				do {
					rtr = blockRayTrace(currentPos, endPos);
					
					if (rtr != null) {												
						currentPos = rtr.hitVec;

						BlockPos thisBlock = rtr.getBlockPos();
						
						// Get the block we hit
						IBlockState state = world.getBlockState(thisBlock);
						if (state != null) {
							Block block = state.getBlock();
							if (block != null && block != Blocks.AIR) {
								result += state.getLightOpacity(world, thisBlock);
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
	private RayTraceResult blockRayTrace(Vec3d startVec, Vec3d endVec) {
		
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
            hitPoint = fromVec.getIntermediateWithXValue(toVec, 0f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.y) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, startBlockPos.add(-1, 0, 0));                	
                }
            }        	
        }
        
        if (toVec.x > 1.0f) {
            // Does the vector cross x == 1?
            hitPoint = fromVec.getIntermediateWithXValue(toVec, 1f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.y) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, startBlockPos.add(1, 0, 0));                	
                }
            }        	
        }

        if (toVec.y < 0.0f) {
            // Does the vector cross y == 0?
            hitPoint = fromVec.getIntermediateWithYValue(toVec, 0.0f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, startBlockPos.add(0, -1, 0));                	
                }
            }        	
        }
        
        if (toVec.y > 1.0f) {
            // Does the vector cross y == 1?
            hitPoint = fromVec.getIntermediateWithYValue(toVec, 1.0f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.UP, startBlockPos.add(0, 1, 0));                	
                }
            }        	
        }

        if (toVec.z < 0.0f) {
            // Does the vector cross z == 0?
            hitPoint = fromVec.getIntermediateWithZValue(toVec, 0.0f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.y)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, startBlockPos.add(0, 0, -1));                	
                }
            }        	
        }
        
        if (toVec.z > 1.0f) {
            // Does the vector cross z == 1?
            hitPoint = fromVec.getIntermediateWithZValue(toVec, 1.0f);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.y)) {
                	hitPoint = hitPoint.add(new Vec3d(startBlockPos));
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, startBlockPos.add(0, 0, 1));                	
                }
            }        	
        }
        
        return null;
	}
	
	private boolean inBounds(double coord) {
		return coord >= 0.0f && coord <= 1.0f;
	}

}
