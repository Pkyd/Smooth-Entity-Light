package lakmoore.sel.client.adaptors;

import java.util.ArrayList;

import lakmoore.sel.client.Config;
import lakmoore.sel.client.SEL;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 
 * @author AtomicStryker, LakMoore
 *
 * Offers Entity Light functionality emulating portable Flood Lights
 *
 */
public class FloodLightAdaptor extends BaseAdaptor
{
	public EntityPlayer thePlayer;
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
		thePlayer = (EntityPlayer)entity;
		
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
        	dummyEntity = new DummyEntity(entity.world);      
            entity.setPosition(thePlayer.chunkCoordX * 16, thePlayer.chunkCoordY, thePlayer.chunkCoordZ * 16);
            thePlayer.world.spawnEntity(dummyEntity);
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
                    RayTraceResult rtr = thePlayer.world.rayTraceBlocks(origin, beam);
                    if (rtr != null)
                    {
                        int dist = (int) Math.round(thePlayer.getDistance(rtr.hitVec.x, rtr.hitVec.y, rtr.hitVec.z));
                        dist = Math.max(0, dist - freeDistance);
                        light.lightLevel = Math.max(0, lightLevel - dist);
                        
                        light.entity.setPosition(rtr.hitVec.x - (look.x * 0.7f), rtr.hitVec.y - (look.y * 0.7f), rtr.hitVec.z - (look.z * 0.7f));
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
        	this.height = 0f;
        	this.width = 0f;
        	
        	// To make the entity visible for debugging, give it size
//        	this.height = 0.5f;
//        	this.width = 0.5f;        	
        }
        
        @Override
		protected void registerData() {
		}
		@Override
		protected void readAdditional(NBTTagCompound compound) {
		}
		@Override
		protected void writeAdditional(NBTTagCompound compound) {
		}
    }

}
