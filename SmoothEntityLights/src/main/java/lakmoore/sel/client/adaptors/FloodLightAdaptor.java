package lakmoore.sel.client.adaptors;

import java.util.ArrayList;

import lakmoore.sel.client.Config;
import lakmoore.sel.client.SEL;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.client.world.ClientWorld;

/**
 * 
 * @author AtomicStryker, LakMoore
 *
 * Offers Entity Light functionality emulating portable Flood Lights
 *
 */
public class FloodLightAdaptor extends BaseAdaptor
{
	public PlayerEntity thePlayer;
	public final int freeDistance = 8;  //Makes this light source a little more useful/interesting
	public final float beamStrength = 26.0F;
    
	public static ArrayList<PartialLightAdaptor> lights;
    public static int ID = 0;
    public static float[] pitch = {0f, 0f, 0f, -8f, 8f};
    public static float[] yaw = {0f, 8f, -8f, 0f, 0f};
    public static int lightLevel = 0;
    private int lastLight = 0;

    public FloodLightAdaptor(Entity entity, boolean simpleMode) {
		super(entity);
		thePlayer = (PlayerEntity)entity;
		
		if (lights != null) {
			for (PartialLightAdaptor light : lights) {
				light.kill();
			}			
		}
		
		lights = new ArrayList<PartialLightAdaptor>();
		ID = 0;
		
		int count = 5;
		if(simpleMode)
			count = 1;

		Entity dummyEntity;
        for (int i = 0; i < count; i++)
        {
        	//dummyEntity = Items.ACACIA_BOAT.createEntity(entity.world, thePlayer, new ItemStack(Items.ACACIA_BOAT));
//        	dummyEntity = new BoatEntity(entity.world, thePlayer.posX, thePlayer.posY, thePlayer.posZ);  
        	dummyEntity = new DummyEntity(entity.world);
        	dummyEntity.setEntityId(Integer.MAX_VALUE - i);
        	dummyEntity.setPosition(thePlayer.chunkCoordX * 16, thePlayer.chunkCoordY, thePlayer.chunkCoordZ * 16);
            ((ClientWorld)thePlayer.world).addEntity(dummyEntity.getEntityId(), dummyEntity);
        }

	}
    
    //Doesn't get the level of this adaptor but does update the dummy adaptors
	@Override
    public int getLightLevel()
    {		
         if (thePlayer != null && thePlayer.isAlive() && !SEL.disabled)
        {
            lightLevel = 0;
        	for (ItemStack stack : thePlayer.getHeldEquipment()) {
        		if (Config.floodLights.contains(stack.getItem().getRegistryName())) {
        			if (lastLight == 0) {
        				// hack to force two updates when the torch is first turned on
        				lightLevel = 14;
        			} else {
            			lightLevel = 15;        				
        			}
        			break;
        		}
        	}
        	
        	lastLight = lightLevel;
        	
        	// Considers eye-height
        	Vec3d origin = thePlayer.getEyePosition(1.0f);
        	
        	for (PartialLightAdaptor light: lights) {
        		
            	if (lightLevel == 0) {
            		// Keep the lights in the player's chunk and don't move them too often
            		light.entity.setPosition(thePlayer.chunkCoordX * 16, thePlayer.chunkCoordY, thePlayer.chunkCoordZ * 16);
            		light.lightLevel = 0;
            	} else {
                    Vec3d look = getVector(thePlayer.rotationYaw + yaw[light.Id], thePlayer.rotationPitch + pitch[light.Id]);
                    
                    Vec3d beam = origin.add(look.x * beamStrength, look.y * beamStrength, look.z * beamStrength);
                    RayTraceResult rtr = thePlayer.world.rayTraceBlocks(
                    	new RayTraceContext(origin, beam, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, thePlayer)
                    );
                    if (rtr != null && rtr.getType() != RayTraceResult.Type.MISS)
                    {
                    	// Can't avoid this square root (would like to if anyone has any ideas)
                        int dist = (int) Math.round(Math.sqrt(thePlayer.getDistanceSq(rtr.getHitVec())));
                        dist = Math.max(0, dist - freeDistance);
                        light.lightLevel = Math.max(0, lightLevel - dist);
                        
                        light.entity.setPosition(rtr.getHitVec().x - (look.x * 0.7f), rtr.getHitVec().y - (look.y * 0.7f), rtr.getHitVec().z - (look.z * 0.7f));
                    }
                    else
                    {
                    	light.lightLevel = 0;
                		light.entity.setPosition(thePlayer.chunkCoordX * 16, thePlayer.chunkCoordY, thePlayer.chunkCoordZ * 16);
                    }	            		
            	}

        	}
        }               

        //this adaptor does not give off light at the player's location
        return 0;	
    }
	
    public static Vec3d getVector(float rotYaw, float rotPitch)
    {
        float f1 = MathHelper.cos(-rotYaw * 0.017453292F - (float)Math.PI);
        float f2 = MathHelper.sin(-rotYaw * 0.017453292F - (float)Math.PI);
        float f3 = -MathHelper.cos(-rotPitch * 0.017453292F);
        float f4 = MathHelper.sin(-rotPitch * 0.017453292F);
        return new Vec3d((double)(f2 * f3), (double)f4, (double)(f1 * f3));
    }
    
	@Override
	public void kill() {
		for (PartialLightAdaptor light : lights) {
			light.kill();
		}
		super.kill();
		thePlayer = null;
	}
    
    public class DummyEntity extends Entity
    {
        public DummyEntity(World par1World) { 
        	super(EntityType.ITEM, par1World); 
        	this.noClip = true;
        	this.recalculateSize();
        	
        }

		@Override
		public EntitySize getSize(Pose poseIn) {
        	// To make the entity visible for debugging, don't set its size to zero
			return new EntitySize(0f, 0f, true);
		}

		@Override
		protected void registerData() {
		}

		@Override
		protected void readAdditional(CompoundNBT compound) {
		}

		@Override
		protected void writeAdditional(CompoundNBT compound) {
		}

		@Override
		public IPacket<?> createSpawnPacket() {
			return null;
		}
        
    }

}
