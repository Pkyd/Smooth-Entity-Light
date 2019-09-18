package lakmoore.sel.client;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * @author LakMoore
 *
 */
public class Command extends CommandBase {

    public Command() {

    }

    @Override
	public String getName() {
        return "sel";
    }

    @Override
	public String getUsage(ICommandSender sender) {
        return "commands.usage";
    }
    
    /**
     * Return the required permission level for this command.
     */
    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender commandSender) {    		
        if(commandSender instanceof EntityPlayer) {
        	EntityPlayer player = (EntityPlayer) commandSender;
        	return player.capabilities.isCreativeMode;
        }
        return false;
    }

    @Override
	public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) throws CommandException {
    	if(args.length < 1 || (
    		"water".equalsIgnoreCase(args[0]) 
			&& "hand".equalsIgnoreCase(args[0]) 
			&& "flood".equalsIgnoreCase(args[0])
        )) {
            commandSender.sendMessage(new TextComponentTranslation("commands.errors.syntax"));
            commandSender.sendMessage(new TextComponentTranslation("commands.usage"));
            return;
        }
        
        ItemStack item = ((EntityPlayer)commandSender).getHeldItemMainhand();
        if (item == null)
        {
            commandSender.sendMessage(new TextComponentTranslation("commands.errors.noitem"));
            return;          
        }
        
        if("hand".equalsIgnoreCase(args[0])) {
            int val = -1;
            if(args.length > 1) {
                val = parseInt(args[1]);                
            }
            
            if (val < 0 || val > 15) {
                commandSender.sendMessage(new TextComponentTranslation("commands.errors.nolevel"));
                commandSender.sendMessage(new TextComponentTranslation("commands.usage"));
                return;                
            }
            
            Config.setHeldLight(item, val);
            commandSender.sendMessage(new TextComponentTranslation("commands.success"));
            return;            
        }

        if("flood".equalsIgnoreCase(args[0])) {
            Config.toggleFloodlight(item);
            commandSender.sendMessage(new TextComponentTranslation("commands.toggle.floodlight"));
            return;            
        }

        if("water".equalsIgnoreCase(args[0])) {
            Config.toggleWaterproof(item);
            commandSender.sendMessage(new TextComponentTranslation("commands.toggle.waterproof"));
            return;            
        }

    }
    
    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }
    
}
