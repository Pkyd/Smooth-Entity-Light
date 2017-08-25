/**
 * 
 */
package atomicstryker.dynamiclights.client;

import java.util.List;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

/**
 * @author LakMoore
 *
 */
public class Command implements ICommand {

    public Command() {

    }

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "dlights";
    }

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#getCommandUsage(net.minecraft.command.ICommandSender)
     */
    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/dlights hand 0-15 | flood | water";
    }

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#getCommandAliases()
     */
    @Override
    public List getCommandAliases() {
        return null;
    }

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#canCommandSenderUseCommand(net.minecraft.command.ICommandSender)
     */
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender commandSender) {
        if(commandSender instanceof EntityPlayer) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see net.minecraft.command.ICommand#addTabCompletionOptions(net.minecraft.command.ICommandSender, java.lang.String[])
     */
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
        if(args.length < 1 || ("water".equalsIgnoreCase(args[0]) && "hand".equalsIgnoreCase(args[0]) && "flood".equalsIgnoreCase(args[0]))) {
            commandSender.addChatMessage(new ChatComponentText("Need to specify 'hand', 'flood' or 'water'"));
            commandSender.addChatMessage(new ChatComponentText(this.getCommandUsage(commandSender)));
            return;
        }
        
        ItemStack item = ((EntityPlayer)commandSender).getCurrentEquippedItem();
        if (item == null)
        {
            commandSender.addChatMessage(new ChatComponentText("You need to be holding an item!"));
            return;          
        }
        

        if("hand".equalsIgnoreCase(args[0])) {
            int val = -1;
            if(args.length > 1 && isInteger(args[1], 10)) {
                val = Integer.parseInt(args[1], 10);                
            }
            
            if (val < 0 || val > 15) {
                commandSender.addChatMessage(new ChatComponentText("Need to specify a light level between 0 and 15."));
                commandSender.addChatMessage(new ChatComponentText(this.getCommandUsage(commandSender)));
                return;                
            }
            
            Config.setHeldLight(item, val);
            commandSender.addChatMessage(new ChatComponentText("Value set."));
            return;            
        }

        if("flood".equalsIgnoreCase(args[0])) {
            Config.toggleFloodlight(item);
            commandSender.addChatMessage(new ChatComponentText("Floodlight toggled."));
            return;            
        }

        if("water".equalsIgnoreCase(args[0])) {
            Config.toggleWaterproof(item);
            commandSender.addChatMessage(new ChatComponentText("Waterproof toggled."));
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
