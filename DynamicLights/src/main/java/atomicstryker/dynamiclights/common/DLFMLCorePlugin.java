package atomicstryker.dynamiclights.common;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.TransformerExclusions({
    "atomicstryker"
})
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("Dynamic Lights")
@IFMLLoadingPlugin.SortingIndex(1001)   //After Notch to Srg de-obfuscation
public class DLFMLCorePlugin implements IFMLLoadingPlugin
{

    @Override
    public String[] getASMTransformerClass()
    {
        return new String[] {"atomicstryker.dynamiclights.common.DLTransformer"};
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }

}
