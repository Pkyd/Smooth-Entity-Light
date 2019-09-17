package lakmoore.sel.client;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

/**
 * @author LakMoore
 *
 */
public class Command extends CommandBase {

    public Command() {

    }

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "sel";
    }

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#getCommandUsage(net.minecraft.command.ICommandSender)
     */
    @Override
    public String getCommandUsage(ICommandSender sender) {
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

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#getCommandAliases()
     */
    @Override
    public List<String> getCommandAliases() {
        return null;
    }

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#canCommandSenderUseCommand(net.minecraft.command.ICommandSender)
     */
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender commandSender) {    		
        if(commandSender instanceof EntityPlayer) {
        	EntityPlayer player = (EntityPlayer) commandSender;
        	return player.capabilities.isCreativeMode;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#addTabCompletionOptions(net.minecraft.command.ICommandSender, java.lang.String[])
     */
    @SuppressWarnings("rawtypes")
	@Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        return null;
    }

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#isUsernameIndex(java.lang.String[], int)
     */
    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }
    
    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#processCommand(net.minecraft.command.ICommandSender, java.lang.String[])
     */
    @Override
    public void processCommand(ICommandSender commandSender, String[] args) {        
        if(args.length < 1 || (
    		"water".equalsIgnoreCase(args[0]) 
			&& "hand".equalsIgnoreCase(args[0]) 
			&& "flood".equalsIgnoreCase(args[0])
        )) {
            commandSender.addChatMessage(new ChatComponentTranslation("commands.errors.syntax"));
            commandSender.addChatMessage(new ChatComponentTranslation("commands.usage"));
            return;
        }
        
        ItemStack item = ((EntityPlayer)commandSender).getCurrentEquippedItem();
        if (item == null)
        {
            commandSender.addChatMessage(new ChatComponentTranslation("commands.errors.noitem"));
            return;          
        }
        

        if("hand".equalsIgnoreCase(args[0])) {
            int val = -1;
            if(args.length > 1) {
                val = parseInt(commandSender, args[1]);                
            }
            
            if (val < 0 || val > 15) {
                commandSender.addChatMessage(new ChatComponentTranslation("commands.errors.nolevel"));
                commandSender.addChatMessage(new ChatComponentTranslation("commands.usage"));
                return;                
            }
            
            Config.setHeldLight(item, val);
            commandSender.addChatMessage(new ChatComponentTranslation("commands.success"));
            return;            
        }

        if("flood".equalsIgnoreCase(args[0])) {
            Config.toggleFloodlight(item);
            commandSender.addChatMessage(new ChatComponentTranslation("commands.toggle.floodlight"));
            return;            
        }

        if("water".equalsIgnoreCase(args[0])) {
            Config.toggleWaterproof(item);
            commandSender.addChatMessage(new ChatComponentTranslation("commands.toggle.waterproof"));
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
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Object o) {
        return 0;
    }


}
