package lakmoore.sel.world;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;

public class DirtyRayTrace {

	private IBlockAccess world;

	private final double sample_size = 16.0;
	private double lastDistSq;

	public DirtyRayTrace(IBlockAccess world) {
		this.world = world;
		lastDistSq = 2.0 / sample_size;
		lastDistSq *= lastDistSq;
	}

	public ArrayList<Block> getBlocksAlongRay(Vec3 start, Vec3 end) {
		ArrayList<Block> result = new ArrayList<Block>();

		// Would be nice to get rid of the square root in the following call
		Vec3 direction = start.subtract(end).normalize();
		direction = Vec3.createVectorHelper(direction.xCoord / sample_size, direction.yCoord / sample_size,
				direction.zCoord / sample_size);

		int thisX = 0;
		int thisY = 0;
		int thisZ = 0;
		int lastX = 0;
		int lastY = 0;
		int lastZ = 0;
		Vec3 currentPos = Vec3.createVectorHelper(start.xCoord, start.yCoord, start.zCoord);

		// Until we are within 2 sample sizes of the end position
		while (end.squareDistanceTo(currentPos) > lastDistSq) {
			// creep forward along the vector
			currentPos = currentPos.addVector(direction.xCoord, direction.yCoord, direction.zCoord);
			thisX = MathHelper.floor_double(currentPos.xCoord);
			thisY = MathHelper.floor_double(currentPos.yCoord);
			thisZ = MathHelper.floor_double(currentPos.zCoord);

			// if we are inside a new Block Position
			if (thisX != lastX || thisY != lastY || thisZ != lastZ) {
				// Add the block to the list
				Block thisBlock = world.getBlock(thisX, thisY, thisZ);
				if (thisBlock != null) {
					result.add(thisBlock);
				}

				// Update
				lastX = thisX;
				lastY = thisY;
				lastZ = thisZ;
			}

		}

		return result;
	}

	/*
	 * Return a list of all non-air blocks between the start and end
	 * Do NOT return the block at "start"
	 */
	public ArrayList<Block> rayTraceAllBlocks(Vec3 start, Vec3 end) {

		ArrayList<Block> result = new ArrayList<Block>();

		if (!Double.isNaN(start.xCoord) && !Double.isNaN(start.yCoord) && !Double.isNaN(start.zCoord)) {
			if (!Double.isNaN(end.xCoord) && !Double.isNaN(end.yCoord) && !Double.isNaN(end.zCoord)) {
				int endX = MathHelper.floor_double(end.xCoord);
				int endY = MathHelper.floor_double(end.yCoord);
				int endZ = MathHelper.floor_double(end.zCoord);

				Vec3 currentPos = Vec3.createVectorHelper(start.xCoord, start.yCoord, start.zCoord);
				int thisX = MathHelper.floor_double(currentPos.xCoord);
				int thisY = MathHelper.floor_double(currentPos.yCoord);
				int thisZ = MathHelper.floor_double(currentPos.zCoord);
				
				while (!(thisX == endX && thisY == endY && thisZ == endZ)) {
					MovingObjectPosition mop = blockRayTrace(thisX, thisY, thisZ, currentPos, end);

					if (mop == null) {
						break;
					}

					currentPos = mop.hitVec;
					thisX = mop.blockX;
					thisY = mop.blockY;
					thisZ = mop.blockZ;

					// Get the block we hit
					Block block = world.getBlock(thisX, thisY, thisZ);
					if (block != null && block != Blocks.air) {
						result.add(block);
					}
				}
			}
		}
		return result;
	}
	
	private MovingObjectPosition blockRayTrace(int x, int y, int z, Vec3 startVec, Vec3 endVec) {

		// re-base the vectors to 0,0,0
		double dX = (double)x;
		double dY = (double)y;
		double dZ = (double)z;
		startVec = startVec.addVector(-dX, -dY, -dZ);
        endVec = endVec.addVector(-dX, -dY, -dZ);
        Vec3 hitPoint = null;

        // Does the vector cross x == 0?
        if (startVec.xCoord > 0.0) {
            hitPoint = startVec.getIntermediateWithXValue(endVec, 0.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.yCoord) && inBounds(hitPoint.zCoord)) {
                	hitPoint = hitPoint.addVector(dX, dY, dZ);
                    return new MovingObjectPosition(x - 1, y, z, 0, hitPoint, true);                	
                }
            }        	
        }
        
        if (startVec.xCoord < 1.0) {
            // Does the vector cross x == 1?
            hitPoint = startVec.getIntermediateWithXValue(endVec, 1.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.yCoord) && inBounds(hitPoint.zCoord)) {
                	hitPoint = hitPoint.addVector(dX, dY, dZ);
                	return new MovingObjectPosition(x + 1, y, z, 0, hitPoint, true);
                }
            }        	
        }

        if (startVec.yCoord > 0.0) {
            // Does the vector cross y == 0?
            hitPoint = startVec.getIntermediateWithYValue(endVec, 0.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.xCoord) && inBounds(hitPoint.zCoord)) {
                	hitPoint = hitPoint.addVector(dX, dY, dZ);
                	return new MovingObjectPosition(x, y - 1, z, 0, hitPoint, true);
                }
            }        	
        }
        
        if (startVec.yCoord < 1.0) {
            // Does the vector cross y == 1?
            hitPoint = startVec.getIntermediateWithYValue(endVec, 1.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.xCoord) && inBounds(hitPoint.zCoord)) {
                	hitPoint = hitPoint.addVector(dX, dY, dZ);
                	return new MovingObjectPosition(x, y + 1, z, 0, hitPoint, true);
                }
            }        	
        }

        if (startVec.zCoord > 0.0) {
            // Does the vector cross z == 0?
            hitPoint = startVec.getIntermediateWithZValue(endVec, 0.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.xCoord) && inBounds(hitPoint.yCoord)) {
                	hitPoint = hitPoint.addVector(dX, dY, dZ);
                	return new MovingObjectPosition(x, y, z - 1, 0, hitPoint, true);
                }
            }        	
        }
        
        if (startVec.zCoord < 1.0) {
            // Does the vector cross z == 1?
            hitPoint = startVec.getIntermediateWithZValue(endVec, 1.0);
            if (hitPoint != null) {
            	// within the same block?
                if (inBounds(hitPoint.xCoord) && inBounds(hitPoint.yCoord)) {
                	hitPoint = hitPoint.addVector(dX, dY, dZ);
                	return new MovingObjectPosition(x, y, z + 1, 0, hitPoint, true);
                }
            }        	
        }
        
        return null;
	}
	
	private boolean inBounds(double x) {
		return x >= 0.0 && x <= 1.0;
	}

}
