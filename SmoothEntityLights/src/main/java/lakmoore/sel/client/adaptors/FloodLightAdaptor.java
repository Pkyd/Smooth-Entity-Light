package lakmoore.sel.client.adaptors;

import lakmoore.sel.client.Config;
import lakmoore.sel.client.SEL;
import lakmoore.sel.client.SELSourceContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * 
 * @author AtomicStryker
 *
 * Offers Dynamic Light functionality emulating portable or static Flood Lights
 *
 */
public class FloodLightAdaptor extends BaseAdaptor
{
	private EntityPlayer thePlayer;
    private final PartialLightAdaptor[] partialLights;
    private final boolean simpleMode;
    private final int freeDistance = 8;  //Makes this light source a little more useful/interesting
    private final float beamStrength = 26.0F;

    public FloodLightAdaptor(Entity entity, boolean simpleMode) {
		super(entity);
		thePlayer = (EntityPlayer)entity;
		this.simpleMode = simpleMode;
		if(simpleMode)
	        partialLights = new PartialLightAdaptor[1];
		else
			partialLights = new PartialLightAdaptor[5];

		Entity dummyEntity;
		SELSourceContainer sources;
        for (int i = 0; i < partialLights.length; i++)
        {
        	dummyEntity = new DummyEntity(entity.worldObj);            
    		sources = new SELSourceContainer(dummyEntity, entity.worldObj);
    		dummyEntity.registerExtendedProperties(SEL.modId, sources);
            partialLights[i] = new PartialLightAdaptor(dummyEntity);
            sources.addLightSource(partialLights[i]);        		
            thePlayer.worldObj.spawnEntityInWorld(dummyEntity);
        }

	}
    
    //Doesn't get the level of this adaptor but does update the dummy adaptors
	@Override
    public int getLightLevel()
    {
        if (thePlayer != null && thePlayer.isEntityAlive() && !SEL.disabled)
        {
            int lightLevel = Config.floodLights.getLightFromItemStack(thePlayer.getCurrentEquippedItem());
                        
            handleLight(partialLights[0], lightLevel, 0f, 0f);
            
            if (!simpleMode)
            {
                handleLight(partialLights[1], lightLevel, 0f, 8f);
                handleLight(partialLights[2], lightLevel, 0f, -8f);
                handleLight(partialLights[3], lightLevel, -8f, 0f);
                handleLight(partialLights[4], lightLevel, 8f, 0f);
            }
        }

        //this adaptor does not give off light at the player's location
        return 0;	
    }
    
    private void handleLight(PartialLightAdaptor source, int light, float yawRot, float pitchRot)
    {
    	if (light == 0) {
            source.lightLevel = 0;
            source.entity.setPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
            return;
    	}

    	// Considers eye-height
    	Vec3 origin = thePlayer.getPosition(1.0f);

        Vec3 look = getVector(thePlayer.rotationYaw + yawRot, thePlayer.rotationPitch + pitchRot);
        
        look = origin.addVector(look.xCoord * beamStrength, look.yCoord * beamStrength, look.zCoord * beamStrength);
        MovingObjectPosition mop = thePlayer.worldObj.rayTraceBlocks(origin, look);
        if (mop != null)
        {
            int dist = (int) Math.round(thePlayer.getDistance(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord));
            dist = Math.max(0, dist - freeDistance);
            source.lightLevel = Math.max(0, light - dist);
            source.entity.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
        }
        else
        {
            source.lightLevel = 0;
            source.entity.setPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        }
    }
    
    public static Vec3 getVector(float rotYaw, float rotPitch)
    {
        float f1 = MathHelper.cos(-rotYaw * 0.017453292F - (float)Math.PI);
        float f2 = MathHelper.sin(-rotYaw * 0.017453292F - (float)Math.PI);
        float f3 = -MathHelper.cos(-rotPitch * 0.017453292F);
        float f4 = MathHelper.sin(-rotPitch * 0.017453292F);
        return Vec3.createVectorHelper((double)(f2 * f3), (double)f4, (double)(f1 * f3));
    }
    
	@Override
	public void kill() {
		super.kill();
		thePlayer = null;
	}

    
    private class PartialLightAdaptor extends BaseAdaptor
    {
		public int lightLevel = 0;

		PartialLightAdaptor(Entity entity) {
			super(entity);
		}

		@Override
		public int getLightLevel() {
			return lightLevel;
		}
    }
    
    public class DummyEntity extends Entity
    {
        public DummyEntity(World par1World) { 
        	super(par1World); 
        	this.height = 0f;
        	this.width = 0f;
        	
        	// To make the entity visible for debugging, give it size
//        	this.height = 0.5f;
//        	this.width = 0.5f;        	
        }
        @Override
        protected void entityInit(){}
        @Override
        protected void readEntityFromNBT(NBTTagCompound var1){}
        @Override
        protected void writeEntityToNBT(NBTTagCompound var1){}
    }

}
