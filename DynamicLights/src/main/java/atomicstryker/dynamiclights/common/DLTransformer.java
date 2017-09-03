package atomicstryker.dynamiclights.common;

import static org.objectweb.asm.Opcodes.*;

import java.util.Iterator;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import cpw.mods.fml.common.FMLLog;

/**
 * 
 * @author AtomicStryker
 * 
 * Magic happens in here. MAGIC.
 * Obfuscated names will have to be updated with each Obfuscation change.
 *
 */
public class DLTransformer implements IClassTransformer
{    
    
    /* net/minecraft/World.computeLightValue */
    private String computeLightValueMethodName = "func_98179_a";   //SRG Method Name

    private final String classNameWorld = "net.minecraft.world.World";
    private final String targetMethodDesc = "(IIILnet/minecraft/world/EnumSkyBlock;)I";
    private final String blockAccessJava = "net/minecraft/world/IBlockAccess";    
    private final String blockJava = "net/minecraft/block/Block";
    
    //------------------------------
    
    /* net/minecraft/client/multiplayer/WorldClient.removeEntityFromWorld */
    private String removeEntityFromWorldMethodName = "func_73028_b";   //SRG Method Name
    
    private final String classNameWorldClient = "net.minecraft.client.multiplayer.WorldClient";
    private final String removeEntityFromWorldMethodDesc = "(I)Lnet/minecraft/entity/Entity;";    
    private final String entityClassName = "net/minecraft/entity/Entity"; 
    
	private static void log(String message)
	{
		FMLLog.log("DynamicLights", Level.INFO, "%s", message);
	}

	private static void error(String message)
	{
		FMLLog.log("DynamicLights", Level.ERROR, "%s", message);
	}
    
    @Override
    public byte[] transform(String name, String newName, byte[] bytes)
    {
    		boolean obf = name.equals(newName);
    		
        if (newName.equals(classNameWorld))
        {
            if (obf) computeLightValueMethodName = "computeLightValue";
            return handleWorldTransform(bytes, obf);
        }
        else if (newName.equals(classNameWorldClient))
        {
        		if (obf) removeEntityFromWorldMethodName = "removeEntityFromWorld";
            return handleWorldClientTransform(bytes, obf);
        }
        
        return bytes;
    }

    private byte[] handleWorldClientTransform(byte[] bytes, boolean obf)
    {
	    	log("Patching WorldClient, obf: " + obf);
	    	boolean found = false;
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        
        // find method to inject into
        Iterator<MethodNode> methods = classNode.methods.iterator();
        while(methods.hasNext())
        {
            MethodNode m = methods.next();
            if (m.name.equals(removeEntityFromWorldMethodName) && m.desc.equals(removeEntityFromWorldMethodDesc))
            {                
                AbstractInsnNode targetNode = null;
                Iterator<AbstractInsnNode> iter = m.instructions.iterator();

                while (iter.hasNext())
                {
                    targetNode = (AbstractInsnNode) iter.next();
                    if (targetNode instanceof JumpInsnNode)
                    {
                    		if (targetNode.getOpcode() == IFNULL) {
                    			
                                // make new instruction list
                                InsnList toInject = new InsnList();
                                
                                // argument mapping, 2 is the Entity
                                toInject.add(new VarInsnNode(ALOAD, 2));
                                
                                //INVOKESTATIC atomicstryker/dynamiclights/client/DynamicLights.onEntityRemoved (Lnet/minecraft/entity/Entity;)V
                                AbstractInsnNode node = new MethodInsnNode(INVOKESTATIC, "atomicstryker/dynamiclights/client/DynamicLights", "onEntityRemoved", "(L" + entityClassName + ";)V", false);
                                toInject.add(node);

                                // inject new instruction list into method instruction list
                                m.instructions.insert(targetNode, toInject);  
                                found = true;
                                break;
                    		}
                    }
                }
                
                log("Patching Complete! Found = " + found);
	            break;
            }
        	
        }
    
	    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
	    classNode.accept(writer);
	    return writer.toByteArray();
	}

        	
    private byte[] handleWorldTransform(byte[] bytes, boolean obf)
    {
    	log("Patching World, obf: " + obf);
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        
        // find method to inject into
        Iterator<MethodNode> methods = classNode.methods.iterator();
        while(methods.hasNext())
        {
            MethodNode m = methods.next();
            if (m.name.equals(computeLightValueMethodName) && m.desc.equals(targetMethodDesc))
            {                
                AbstractInsnNode targetNode = null;
                Iterator<AbstractInsnNode> iter = m.instructions.iterator();
                boolean deleting = false;
                boolean replacing = false;
                while (iter.hasNext())
                {
                    targetNode = (AbstractInsnNode) iter.next();
                    
                    if (targetNode instanceof VarInsnNode)
                    {
                        VarInsnNode vNode = (VarInsnNode) targetNode;
                        if (vNode.var == 6)
                        {
                            if (vNode.getOpcode() == ASTORE)
                            {
                                deleting = true;
                                continue;
                            }
                            else if (vNode.getOpcode() == ISTORE)
                            {
                                replacing = true;
                                targetNode = (AbstractInsnNode) iter.next();
                                break;
                            }
                        }
                        
                        if (vNode.var == 7 && deleting)
                        {
                            break;
                        }
                    }
                    
                    if (deleting)
                    {
                        System.out.println("Removing "+targetNode);
                        iter.remove();
                    }
                }
                
                // make new instruction list
                InsnList toInject = new InsnList();
                
                // argument mapping! 0 is World, 5 is block, 123 are xyz
                toInject.add(new VarInsnNode(ALOAD, 0));
                toInject.add(new VarInsnNode(ALOAD, 5));
                toInject.add(new VarInsnNode(ILOAD, 1));
                toInject.add(new VarInsnNode(ILOAD, 2));
                toInject.add(new VarInsnNode(ILOAD, 3));
                
                try
                {
                    try
                    {
                        AbstractInsnNode node = MethodInsnNode.class.getConstructor(int.class, String.class, String.class, String.class).newInstance(
                                INVOKESTATIC, "atomicstryker/dynamiclights/client/DynamicLights", "getLightValue", "(L"+blockAccessJava+";L"+blockJava+";III)I");
                        toInject.add(node);
                    }
                    catch (NoSuchMethodException e)
                    {
                        AbstractInsnNode node = MethodInsnNode.class.getConstructor(int.class, String.class, String.class, String.class, boolean.class).newInstance(
                                INVOKESTATIC, "atomicstryker/dynamiclights/client/DynamicLights", "getLightValue", "(L"+blockAccessJava+";L"+blockJava+";III)I", false);
                        toInject.add(node);
                    }
                }
                catch (Exception e)
                {
                	error("Dynamic Lights ASM transform failed T_T");
                    e.printStackTrace();
                    return bytes;
                }
                
                if (replacing)
                {
                    toInject.add(new VarInsnNode(ISTORE, 6));
                }
                
                // inject new instruction list into method instruction list
                m.instructions.insertBefore(targetNode, toInject);
                
            	log("Patching Complete!");
                break;
            }

        }
        
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
