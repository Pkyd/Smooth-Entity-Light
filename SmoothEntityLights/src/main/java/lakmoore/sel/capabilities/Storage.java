package lakmoore.sel.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class Storage implements IStorage<ILightSourceCapability>
{
    @Override
    public NBTBase writeNBT(Capability<ILightSourceCapability> capability, ILightSourceCapability instance, EnumFacing side) {
        return null;
    }

    @Override
    public void readNBT(Capability<ILightSourceCapability> capability, ILightSourceCapability instance, EnumFacing side, NBTBase nbt) {
    }
}
