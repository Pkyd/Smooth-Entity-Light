package lakmoore.sel.common;

import static org.objectweb.asm.Opcodes.*;

import java.util.Iterator;
//
//import net.minecraft.launchwrapper.IClassTransformer;
//import net.minecraft.launchwrapper.Launch;
//import net.minecraftforge.fml.common.FMLLog;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import cpw.mods.modlauncher.api.ITransformer;

/**
 * 
 * @author LakMoore
 *
 */
public class Transformer {
	
	private final String constructorName = "<init>";

	// ----------- Details for WorldClient Upgrade
	private final String classWorldClientName = "net.minecraft.client.multiplayer.WorldClient";		// bsb
	private final String classWorldNameOLD = "net/minecraft/world/World";							// amu
	private final String classWorldNameNEW = "lakmoore/sel/world/WorldSEL";
	
	// ----------- Details for ChunkCache Upgrade -----------
	private final String classRenderChunkName = "net.minecraft.client.renderer.chunk.RenderChunk";  // bxr
	private final String methodCreateRegionRenderCacheName = "createRegionRenderCache";				// Forge method - not de-obf'd
	private final String classChunkCacheNameOLD = "net/minecraft/world/ChunkCache";					// and
	private final String classChunkCacheNameNEW = "lakmoore/sel/world/ChunkCacheSEL";

	// ----------- Details for ClassInheritanceMultiMap Upgrade -----------
	private final String classChunkName = "net.minecraft.world.chunk.Chunk";  						// 
	private final String classMultiMapNameOLD = "net/minecraft/util/ClassInheritanceMultiMap";		// 
	private final String classMultiMapNameNEW = "lakmoore/sel/common/SELClassInheritanceMultiMap";

	// ----------- Details for VertexLighterFlat Upgrade -----------
	private final String classForgeBlockModelRendererName = "net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer";  						// 
	private final String methodNewVertexLighterName = "lambda$new$0";  						// 
	private final String classVertexLighterOLD = "net/minecraftforge/client/model/pipeline/VertexLighterFlat";		// 
	private final String classVertexLighterNEW = "lakmoore/sel/client/model/pipeline/VertexLighterSEL";

	// ----------- Details for VertexLighterSmoothAo Upgrade -----------
	private final String classVertexLighterSmoothAoName = "net.minecraftforge.client.model.pipeline.VertexLighterSmoothAo";  						// 

	private static void log(String message) {
		System.out.format("%s\n", message);
	}

	private static void error(String message) {
		System.out.format("%s\n", message);
//		FMLLog.log("SmoothEntityLights", Level.ERROR, "%s", message);
	}

	public byte[] transform(String name, String newName, byte[] bytes) {
		boolean obf = !name.equals(newName);
		
		if (newName.equals(classWorldClientName)) {
			log("********* INSIDE TRANSFORMER ABOUT TO PATCH: " + name + "|" + newName);
			return handleWorldClientTransform(bytes, obf);
		}  
		if (newName.equals(classRenderChunkName)) {
			log("********* INSIDE TRANSFORMER ABOUT TO PATCH: " + name + "|" + newName);
			return handleChunkCacheTransform(bytes, obf);
		}
		if (newName.equals(classChunkName)) {
			log("********* INSIDE TRANSFORMER ABOUT TO PATCH: " + name + "|" + newName);
			return handleChunkTransform(bytes, obf);
		}
		if (newName.equals(classForgeBlockModelRendererName)) {
			log("********* INSIDE TRANSFORMER ABOUT TO PATCH: " + name + "|" + newName);
			return handleVertexLighterTransform(bytes, obf);
		}
		if (newName.equals(classVertexLighterSmoothAoName)) {
			log("********* INSIDE TRANSFORMER ABOUT TO PATCH: " + name + "|" + newName);
			return handleVertexLighterSmoothAoTransform(bytes, obf);
		}
				
		return bytes;
	}
	
	private byte[] handleChunkTransform(byte[] bytes, boolean obf) {
		log("Patching Chunk, obf: " + obf);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		Boolean found = false;
		
		// find method to inject into
		Iterator<MethodNode> methods = classNode.methods.iterator();
		while (methods.hasNext() && !found) {
			MethodNode m = methods.next();
						
			// constructor
			if (m.name.equals(constructorName)) {

				AbstractInsnNode targetNode = null;
				Iterator<AbstractInsnNode> iter = m.instructions.iterator();

				while (iter.hasNext() && !found) {
					targetNode = (AbstractInsnNode) iter.next();
					if (targetNode instanceof TypeInsnNode && targetNode.getOpcode() == NEW) {
						TypeInsnNode node = (TypeInsnNode) targetNode;
						
						if (node.desc.equals(classMultiMapNameOLD)) {
							node.desc = classMultiMapNameNEW;
							log("Patched MultiMap New!");
						}
					} else if (targetNode instanceof MethodInsnNode && targetNode.getOpcode() == INVOKESPECIAL) {
						MethodInsnNode node = (MethodInsnNode) targetNode;

						if (node.owner.equals(classMultiMapNameOLD)) {
							node.owner = classMultiMapNameNEW;
							found = true;
							log("Patched MultiMap Init!");
						}
					}
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();

	}

	private byte[] handleChunkCacheTransform(byte[] bytes, boolean obf) {
		log("Patching ChunkCache, obf: " + obf);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		Boolean found = false;
		
		// find method to inject into
		Iterator<MethodNode> methods = classNode.methods.iterator();
		while (methods.hasNext() && !found) {
			MethodNode m = methods.next();
						
			if (methodCreateRegionRenderCacheName.equals(m.name)) {
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

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();

	}

	private byte[] handleWorldClientTransform(byte[] bytes, boolean obf) {
		log("Patching WorldClient, obf: " + obf);
		boolean found = false;
				
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		// Don't skip reading the frames
		classReader.accept(classNode, 0);
		
		classNode.superName = classWorldNameNEW;

		// find method to inject into
		Iterator<MethodNode> methods = classNode.methods.iterator();
		while (methods.hasNext() && !found) {
			MethodNode m = methods.next();			
			
			// Constructor
			if (m.name.equals(constructorName)) {
				AbstractInsnNode targetNode = null;
				Iterator<AbstractInsnNode> iter = m.instructions.iterator();

				while (iter.hasNext() && !found) {
					targetNode = (AbstractInsnNode) iter.next();
					if (targetNode instanceof MethodInsnNode) {
						MethodInsnNode node = (MethodInsnNode) targetNode;

						if (node.owner.equals(classWorldNameOLD) && node.name.equals(constructorName)) {
							node.owner = classWorldNameNEW;
							log("Patched World Init!");
							found = true;
							continue;
						}
					}
				}
			}

		}

		SELClassWriter writer = new SELClassWriter(ClassWriter.COMPUTE_FRAMES);
		// The main ClassLoader fails to find Classes referenced in WorldClient 
		// at the time WorldClient is being transformed
		// PlayerEntitySP for example
		// By specifying FML's ClassLoader, getCommonSuperClass() no longer fails
//		writer.setClassLoader(Launch.classLoader);
		classNode.accept(writer);		
		return writer.toByteArray();
	}
	
	private byte[] handleVertexLighterTransform(byte[] bytes, boolean obf) {
		log("Patching ForgeBlockModelRenderer, obf: " + obf);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		Boolean found = false;
		
		Iterator<FieldNode> fields = classNode.fields.iterator();
		while (fields.hasNext() && !found) {
			FieldNode field = fields.next();

			if (field.signature.equals("Ljava/lang/ThreadLocal<L" + this.classVertexLighterOLD + ";>;")) {				
				field.signature = "Ljava/lang/ThreadLocal<L" + this.classVertexLighterNEW + ";>;";
				System.out.println("Descriptor: "+ field.desc.toString());
				found = true;
			}
		}
		
		found = false;
		// find method to inject into
		Iterator<MethodNode> methods = classNode.methods.iterator();
		while (methods.hasNext() && !found) {
			MethodNode m = methods.next();
									
			if (methodNewVertexLighterName.equals(m.name)) {
				// Found the correct method
				
				AbstractInsnNode targetNode = null;
				Iterator<AbstractInsnNode> iter = m.instructions.iterator();

				while (iter.hasNext() && !found) {
					targetNode = (AbstractInsnNode) iter.next();

					if (targetNode instanceof TypeInsnNode && targetNode.getOpcode() == NEW) {
						TypeInsnNode node = (TypeInsnNode) targetNode;

						if (node.desc.equals(classVertexLighterOLD)) {
							node.desc = classVertexLighterNEW;
							log("Patched VertexLighter::new()");
						}
					} else if (targetNode instanceof MethodInsnNode && targetNode.getOpcode() == INVOKESPECIAL) {
						MethodInsnNode node = (MethodInsnNode) targetNode;

						if (node.owner.equals(classVertexLighterOLD)) {
							node.owner = classVertexLighterNEW;
							found = true;
							log("Patched VertexLighter::<init>()");
						}
					}
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();

	}

	private byte[] handleVertexLighterSmoothAoTransform(byte[] bytes, boolean obf) {
		log("Patching VertexLighterSmoothAo, obf: " + obf);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		boolean found = false;
		
		classNode.superName = classVertexLighterNEW;

		// find method to inject into
		Iterator<MethodNode> methods = classNode.methods.iterator();
		while (methods.hasNext() && !found) {
			MethodNode m = methods.next();			
			
			// Constructor
			if (m.name.equals(constructorName)) {
				AbstractInsnNode targetNode = null;
				Iterator<AbstractInsnNode> iter = m.instructions.iterator();

				while (iter.hasNext() && !found) {
					targetNode = (AbstractInsnNode) iter.next();
					if (targetNode instanceof MethodInsnNode) {
						MethodInsnNode node = (MethodInsnNode) targetNode;

						if (node.owner.equals(classVertexLighterOLD) && node.name.equals(constructorName)) {
							node.owner = classVertexLighterNEW;
							log("Patched VertexLighterSmoothAo Init!");
							found = true;
							continue;
						}
					}
				}
			}

		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();

	}

}
