package lakmoore.sel.world;

import java.util.ArrayList;

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
	 * Return a list of all non-air blocks between the start and end
	 * Do NOT return the block at "start"
	 */
	public ArrayList<Block> rayTraceAllBlocks(Vec3d start, Vec3d end) {

		ArrayList<Block> result = new ArrayList<Block>();

		if (!Double.isNaN(start.x) && !Double.isNaN(start.y) && !Double.isNaN(start.z)) {
			if (!Double.isNaN(end.x) && !Double.isNaN(end.y) && !Double.isNaN(end.z)) {
				
				BlockPos endBlock = new BlockPos(end);
				BlockPos thisBlock = new BlockPos(start);				
				Vec3d currentPos = new Vec3d(start.x, start.y, start.z);
				
				while (!thisBlock.equals(endBlock)) {
					RayTraceResult rtr = blockRayTrace(thisBlock, currentPos, end);

					if (rtr == null) {
						break;
					}

					currentPos = rtr.hitVec;
					thisBlock = rtr.getBlockPos();
					
					// Get the block we hit
					IBlockState state = world.getBlockState(thisBlock);
					if (state != null) {
						Block block = state.getBlock();
						if (block != null && block != Blocks.AIR) {
							result.add(block);
						}						
					}
				}
			}
		}
		return result;
	}

	/*
	 * Return the total opacity of the blocks along the ray
	 * Do NOT consider the block at "start"
	 */
	public int rayTraceForOpacity(Vec3d start, Vec3d end) {

		int result = 0;

		if (!Double.isNaN(start.x) && !Double.isNaN(start.y) && !Double.isNaN(start.z)) {
			if (!Double.isNaN(end.x) && !Double.isNaN(end.y) && !Double.isNaN(end.z)) {
				
				BlockPos endBlock = new BlockPos(end);
				BlockPos thisBlock = new BlockPos(start);				
				Vec3d currentPos = new Vec3d(start.x, start.y, start.z);
				
				while (!thisBlock.equals(endBlock) && result < 15) {
					RayTraceResult rtr = blockRayTrace(thisBlock, currentPos, end);

					if (rtr == null) {
						break;
					}

					currentPos = rtr.hitVec;
					thisBlock = rtr.getBlockPos();
					
					// Get the block we hit
					IBlockState state = world.getBlockState(thisBlock);
					if (state != null) {
						Block block = state.getBlock();
						if (block != null && block != Blocks.AIR) {
							result += state.getLightOpacity(world, thisBlock);
						}						
					}
				}
			}
		}
		return Math.min(15, result);
	}
	
	/*
	 * A RayTrace routine that I can understand.
	 * Resultant side-hit is not calculated
	 */
	private RayTraceResult blockRayTrace(BlockPos pos, Vec3d startVec, Vec3d endVec) {

		// re-base the vectors to 0,0,0
		double dX = (double)pos.getX();
		double dY = (double)pos.getY();
		double dZ = (double)pos.getZ();
		startVec = startVec.add(-dX, -dY, -dZ);
        endVec = endVec.add(-dX, -dY, -dZ);

        Vec3d hitPoint = null;

        // Does the vector cross x == 0?
        if (startVec.x > 0.0) {
            hitPoint = startVec.getIntermediateWithXValue(endVec, 0.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.y) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(dX, dY, dZ);
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, pos.add(-1, 0, 0));                	
                }
            }        	
        }
        
        if (startVec.x < 1.0) {
            // Does the vector cross x == 1?
            hitPoint = startVec.getIntermediateWithXValue(endVec, 1.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.y) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(dX, dY, dZ);
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, pos.add(1, 0, 0));                	
                }
            }        	
        }

        if (startVec.y > 0.0) {
            // Does the vector cross y == 0?
            hitPoint = startVec.getIntermediateWithYValue(endVec, 0.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(dX, dY, dZ);
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, pos.add(0, -1, 0));                	
                }
            }        	
        }
        
        if (startVec.y < 1.0) {
            // Does the vector cross y == 1?
            hitPoint = startVec.getIntermediateWithYValue(endVec, 1.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.z)) {
                	hitPoint = hitPoint.add(dX, dY, dZ);
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, pos.add(0, 1, 0));                	
                }
            }        	
        }

        if (startVec.z > 0.0) {
            // Does the vector cross z == 0?
            hitPoint = startVec.getIntermediateWithZValue(endVec, 0.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.y)) {
                	hitPoint = hitPoint.add(dX, dY, dZ);
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, pos.add(0, 0, -1));                	
                }
            }        	
        }
        
        if (startVec.z < 1.0) {
            // Does the vector cross z == 1?
            hitPoint = startVec.getIntermediateWithZValue(endVec, 1.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.x) && inBounds(hitPoint.y)) {
                	hitPoint = hitPoint.add(dX, dY, dZ);
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, hitPoint, EnumFacing.DOWN, pos.add(0, 0, 1));                	
                }
            }        	
        }
        
        return null;
	}
	
	private boolean inBounds(double x) {
		return x >= 0.0 && x <= 1.0;
	}

}
