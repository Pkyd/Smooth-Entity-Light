package atomicstryker.dynamiclights.client.adaptors;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import atomicstryker.dynamiclights.client.Config;
import atomicstryker.dynamiclights.client.DynamicLights;

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
    private final float beamStrength = 64;

    public FloodLightAdaptor(Entity entity, boolean simpleMode) {
		super(entity);
		thePlayer = (EntityPlayer)entity;
		this.simpleMode = simpleMode;
		if(simpleMode)
	        partialLights = new PartialLightAdaptor[1];
		else
			partialLights = new PartialLightAdaptor[5];

		checkDummyInit(thePlayer.worldObj);

	}
    
    public void onTick()
    {
        if (thePlayer != null && thePlayer.isEntityAlive() && !DynamicLights.globalLightsOff)
        {
            int lightLevel = Config.floodLights.getLightFromItemStack(thePlayer.getCurrentEquippedItem());
                        
            if (lightLevel > 0)
            {
            	
                handleLight(partialLights[0], lightLevel, 0f, 0f);
                
                if (!simpleMode)
                {
                    handleLight(partialLights[1], lightLevel, 12f, 9f);
                    handleLight(partialLights[2], lightLevel, 9f, -12f);
                    handleLight(partialLights[3], lightLevel, -12f, -9f);
                    handleLight(partialLights[4], lightLevel, -9f, 12f);
                }
                setLightsEnabled(true);
            }
            else
            {
                setLightsEnabled(false);
            }
        }
	
    }
    
    private void handleLight(PartialLightAdaptor source, int light, float yawRot, float pitchRot)
    {
        Vec3 origin = thePlayer.getPosition(1.0f);

        Vec3 look = getVector(thePlayer.rotationYaw + yawRot, thePlayer.rotationPitch + pitchRot);
        
        look = origin.addVector(look.xCoord * beamStrength, look.yCoord * beamStrength, look.zCoord * beamStrength);
        MovingObjectPosition mop = thePlayer.worldObj.rayTraceBlocks(origin, look);
        if (mop != null)
        {
            int dist = (int) Math.round(thePlayer.getDistance(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord));
            source.lightLevel = Math.max(0, light - dist);
            source.entity.posX = mop.hitVec.xCoord;
            source.entity.posY = mop.hitVec.yCoord;
            source.entity.posZ = mop.hitVec.zCoord;
        }
        else
        {
            source.lightLevel = 0;
        }
        source.onTick();
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
    public void disableLight()
    {
    	setLightsEnabled(false);
    	super.disableLight();
    }

    private void setLightsEnabled(boolean newEnabled)
    {
        if (newEnabled != enabled)
        {
            enabled = newEnabled;
            
            for (PartialLightAdaptor p : partialLights)
            {
                if (newEnabled)
                {
                    p.onTick();
                }
                else
                {
                    p.lightLevel = 0;
                    p.onTick();
                }
            }
        }
    }

    private void checkDummyInit(World world)
    {
        if (partialLights[0] == null)
        {
            for (int i = 0; i < partialLights.length; i++)
            {
                partialLights[i] = new PartialLightAdaptor(new DummyEntity(world));
                world.spawnEntityInWorld(partialLights[i].entity);
                partialLights[i].onTick();
            }
        }
    }
    
    private class PartialLightAdaptor extends BaseAdaptor
    {
        PartialLightAdaptor(Entity entity) {
			super(entity);
		}
    }
    
    public class DummyEntity extends Entity
    {
        public DummyEntity(World par1World) { super(par1World); }
        @Override
        protected void entityInit(){}
        @Override
        protected void readEntityFromNBT(NBTTagCompound var1){}
        @Override
        protected void writeEntityToNBT(NBTTagCompound var1){}
    }

}
