package atomicstryker.dynamiclights.client.adaptors;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
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

    public FloodLightAdaptor(Entity entity, boolean simpleMode) {
		super(entity);
		thePlayer = (EntityPlayer)entity;
		this.simpleMode = simpleMode;
		if(simpleMode)
	        partialLights = new PartialLightAdaptor[1];
		else
			partialLights = new PartialLightAdaptor[5];
	}
    
    public void onTick()
    {
        if (thePlayer != null && thePlayer.isEntityAlive() && !DynamicLights.globalLightsOff)
        {
            int lightLevel = DynamicLights.floodLights.getLightFromItemStack(thePlayer.getCurrentEquippedItem());
            
            checkDummyInit(thePlayer.worldObj);
            
            if (lightLevel > 0)
            {
            	
                handleLight(partialLights[0], lightLevel, 0f, 0f);
                
                if (!simpleMode)
                {
                    handleLight(partialLights[1], lightLevel, 15f, 15f);
                    handleLight(partialLights[2], lightLevel, -15f, 15f);
                    handleLight(partialLights[3], lightLevel, 15f, -15f);
                    handleLight(partialLights[4], lightLevel, -15f, -15f);
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
        Vec3 pos = thePlayer.getPosition(1.0f);
        thePlayer.rotationPitch += pitchRot;
        thePlayer.rotationYaw += yawRot;
        Vec3 look = thePlayer.getLook(1.0f);
        thePlayer.rotationPitch -= pitchRot;
        thePlayer.rotationYaw -= yawRot;
        look = pos.addVector(look.xCoord * 16d, look.yCoord * 16d, look.zCoord * 16d);
        MovingObjectPosition mop = thePlayer.worldObj.rayTraceBlocks(pos, look);
        if (mop != null)
        {
            int dist = (int) Math.round(thePlayer.getDistance(mop.blockX+0.5d, mop.blockY+0.5d, mop.blockZ+0.5d));
            source.lightLevel = light - dist;
            source.entity.posX = mop.blockX+0.5d;
            source.entity.posY = mop.blockY+0.5d;
            source.entity.posZ = mop.blockZ+0.5d;
        }
        else
        {
            source.lightLevel = 0;
        }
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
            
            if (!DynamicLights.simpleMode)
            {
                for (PartialLightAdaptor p : partialLights)
                {
                    if (newEnabled)
                    {
                        DynamicLights.addLightSource(p);
                    }
                    else
                    {
                        DynamicLights.removeLightSource(p);
                    }
                }
            }
            else
            {
                if (newEnabled)
                {
                    DynamicLights.addLightSource(partialLights[0]);
                }
                else
                {
                    DynamicLights.removeLightSource(partialLights[0]);
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
                DynamicLights.addLightSource(partialLights[i]);
            }
        }
    }
    
    private class PartialLightAdaptor extends BaseAdaptor
    {
        PartialLightAdaptor(Entity entity) {
			super(entity);
		}
    }
    
    private class DummyEntity extends Entity
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
