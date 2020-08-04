package lakmoore.sel.client;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * @author LakMoore
 *
 */
public class CommandSEL {
	
//    private static final SimpleCommandExceptionType INVALID_ACTION 
//    	= new SimpleCommandExceptionType(new TextComponentTranslation("commands.errors.syntax"));
    private static final SimpleCommandExceptionType INVALID_ITEM
    	= new SimpleCommandExceptionType(new TextComponentTranslation("commands.errors.noitem"));
    
    static ArgumentBuilder<CommandSource, ?> register()
    {
        return Commands.literal("sel")
            .requires(cs -> cs.hasPermissionLevel(2)) //permission
            .then(Commands.literal("flood")
            		.executes(ctx -> flood(ctx.getSource()))
            )
            .then(Commands.literal("water")
            		.executes(ctx -> water(ctx.getSource()))
            )
            .then(Commands.literal("hand")
            		.then(Commands.argument("lightVal", IntegerArgumentType.integer(0, 15)))
                    .executes(ctx -> hand(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "lightVal")))
            );
    }

	public static int hand(CommandSource source, Integer lightVal) throws CommandSyntaxException {   
		ItemStack item = getHeldItem(source);
        Config.setHeldLight(item, lightVal);
        source.sendFeedback(new TextComponentTranslation("commands.success"), true);           
        return Command.SINGLE_SUCCESS;
    }
	
	public static int flood(CommandSource source) throws CommandSyntaxException {
		ItemStack item = getHeldItem(source);
        Config.toggleFloodlight(item);
        source.sendFeedback(new TextComponentTranslation("commands.toggle.floodlight"), true);
        return Command.SINGLE_SUCCESS;
	}

	public static int water(CommandSource source) throws CommandSyntaxException {
		ItemStack item = getHeldItem(source);
        Config.toggleWaterproof(item);
        source.sendFeedback(new TextComponentTranslation("commands.toggle.waterproof"), true);
        return Command.SINGLE_SUCCESS;
	}
	
	public static ItemStack getHeldItem(CommandSource source) throws CommandSyntaxException {
        ItemStack item = ((EntityPlayer)source.getEntity()).getHeldItemMainhand();
        if (item == null)
        {
        	throw INVALID_ITEM.create();
        }   
        return item;
	}

}
