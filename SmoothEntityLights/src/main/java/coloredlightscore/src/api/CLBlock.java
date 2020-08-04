package coloredlightscore.src.api;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public abstract class CLBlock extends Block
{
    public CLBlock(Material matt, Block.Properties properties)
    {
        super(properties);
    }

    public abstract int getColorLightValue(int meta);

    public int getLightValue(IBlockReader world, BlockPos pos, int meta)
    {
        return getColorLightValue(meta);
    }
}
