package lakmoore.sel.common;

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
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import cpw.mods.fml.common.FMLLog;

/**
 * 
 * @author LakMoore
 *
 */
public class Transformer implements IClassTransformer {

	// ----------- Details for WorldClient Upgrade
	private final String removeEntityFromWorldMethodName = "removeEntityFromWorld";  // "func_73028_b"; // SRG Method Name
	private final String removeEntityFromWorldMethodNameObf = "func_73028_b";
	private final String classNameWorldClient = "net.minecraft.client.multiplayer.WorldClient";
	private final String removeEntityFromWorldMethodDesc = "(I)Lnet/minecraft/entity/Entity;";
	private final String entityClassName = "net/minecraft/entity/Entity";

	// ----------- Details for ChunkCache Upgrade -----------
	private final String classWorldRendererName = "net.minecraft.client.renderer.WorldRenderer";
	private final String methodUpdateRendererName = "updateRenderer";
	private final String methodUpdateRendererNameObf = "func_147892_a";
	private final String methodUpdateRendererDesc = "(Lnet/minecraft/entity/EntityLivingBase;)V";
	private final String classChunkCacheNameOLD = "net/minecraft/world/ChunkCache";
	private final String classChunkCacheNameNEW = "lakmoore/sel/world/ChunkCacheSEL";


	private static void log(String message) {
		FMLLog.log("SmoothEntityLights", Level.INFO, "%s", message);
	}

	private static void error(String message) {
		FMLLog.log("SmoothEntityLights", Level.ERROR, "%s", message);
	}

	@Override
	public byte[] transform(String name, String newName, byte[] bytes) {
		boolean obf = !name.equals(newName);

		if (newName.equals(classNameWorldClient)) {
			log("********* INSIDE TRANSFORMER ABOUT TO PATCH: " + name + "|" + newName);
			return handleWorldClientTransform(bytes, obf);
		} else if (newName.equals(classWorldRendererName)) {
			log("********* INSIDE TRANSFORMER ABOUT TO PATCH: " + name + "|" + newName);
			return handleChunkCacheTransform(bytes, obf);
		}

		return bytes;
	}

	private byte[] handleChunkCacheTransform(byte[] bytes, boolean obf) {
		log("Patching ChunkCache, obf: " + obf);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		Boolean found = false;
		
		String methodName = methodUpdateRendererName;
		if (obf) {
			methodName = methodUpdateRendererNameObf;
		}

		// find method to inject into
		Iterator<MethodNode> methods = classNode.methods.iterator();
		while (methods.hasNext() && !found) {
			MethodNode m = methods.next();
			
			if (m.name.equals(methodName) && m.desc.equals(methodUpdateRendererDesc)) {
				// Found the correct method
				AbstractInsnNode targetNode = null;
				Iterator<AbstractInsnNode> iter = m.instructions.iterator();

				while (iter.hasNext() && !found) {
					targetNode = (AbstractInsnNode) iter.next();
					if (targetNode instanceof TypeInsnNode && targetNode.getOpcode() == NEW) {
						TypeInsnNode node = (TypeInsnNode) targetNode;
						
						if (node.desc.equals(classChunkCacheNameOLD)) {
							node.desc = classChunkCacheNameNEW;
							log("Patched ChunkCache New!");
						}
					} else if (targetNode instanceof MethodInsnNode && targetNode.getOpcode() == INVOKESPECIAL) {
						MethodInsnNode node = (MethodInsnNode) targetNode;

						if (node.owner.equals(classChunkCacheNameOLD)) {
							node.owner = classChunkCacheNameNEW;
							found = true;
							log("Patched ChunkCache Init!");
						}
					}
				}
			}
		}
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();

	}

	private byte[] handleWorldClientTransform(byte[] bytes, boolean obf) {
		log("Patching WorldClient, obf: " + obf);
		boolean found = false;
		String methodName = removeEntityFromWorldMethodName;
		if (obf) {
			methodName = removeEntityFromWorldMethodNameObf;
		}
		
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		classNode.superName = "lakmoore/sel/world/WorldSEL";

		// find method to inject into
		Iterator<MethodNode> methods = classNode.methods.iterator();
		while (methods.hasNext() && !found) {
			MethodNode m = methods.next();
			// Constructor
			if (m.name.equals("<init>")) {
				AbstractInsnNode targetNode = null;
				Iterator<AbstractInsnNode> iter = m.instructions.iterator();

				while (iter.hasNext()) {
					targetNode = (AbstractInsnNode) iter.next();
					if (targetNode instanceof MethodInsnNode) {
						MethodInsnNode node = (MethodInsnNode) targetNode;
						if (node.owner.equals("net/minecraft/world/World") && node.name.equals("<init>")) {
							node.owner = "lakmoore/sel/world/WorldSEL";
							continue;
						}
					}
				}
			}
			if (m.name.equals(methodName) && m.desc.equals(removeEntityFromWorldMethodDesc)) {
				AbstractInsnNode targetNode = null;
				Iterator<AbstractInsnNode> iter = m.instructions.iterator();

				while (iter.hasNext()) {
					targetNode = (AbstractInsnNode) iter.next();
					if (targetNode instanceof JumpInsnNode) {
						if (targetNode.getOpcode() == IFNULL) {

							// make new instruction list
							InsnList toInject = new InsnList();

							// argument mapping, 2 is the Entity
							toInject.add(new VarInsnNode(ALOAD, 2));

							// INVOKESTATIC lakmoore/sel/client/SEL.onEntityRemoved
							// (Lnet/minecraft/entity/Entity;)V
							AbstractInsnNode node = new MethodInsnNode(INVOKESTATIC, "lakmoore/sel/client/SEL",
									"onEntityRemoved", "(L" + entityClassName + ";)V", false);
							toInject.add(node);

							// inject new instruction list into method instruction list
							m.instructions.insert(targetNode, toInject);
							found = true;
							break;
						}
					}
				}

				log("World Patching Complete! Found = " + found);
				break;
			}

		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

}
