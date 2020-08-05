var LocalVariableNode = Java.type("org.objectweb.asm.tree.LocalVariableNode")
var constructorName = "<init>";
var methodNewVertexLighterName = "lambda$new$0"; 
var classVertexLighterOLD = "net/minecraftforge/client/model/pipeline/VertexLighterFlat";
var classVertexLighterNEW = "lakmoore/sel/client/model/pipeline/VertexLighterSEL";

/**
 * This function is called by Forge before any minecraft classes are loaded to
 * setup the coremod.
 *
 * @return {object} All the transformers of this coremod.
 */
function initializeCoreMod() {
	print("Initializing SEL ForgeBlockModelRenderer Coremod");
	return {
		"SEL ForgeBlockModelRenderer Transformer": {
			"target": {
				"type": "CLASS",
				"name": "net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer"
			},
			"transformer": function(classNode) {
				print("Patching ForgeBlockModelRenderer");
				var fields = classNode.fields;
				for (var i in fields) {
					var field = fields[i];
					if (
						field.signature &&
						field.signature.equals("Ljava/lang/ThreadLocal<L" + classVertexLighterOLD + ";>;")
					) {				
						field.signature = "Ljava/lang/ThreadLocal<L" + classVertexLighterNEW + ";>;";						
						print("Patched ForgeBlockModelRenderer field type");
						break;
					}
				}
				
				var methods = classNode.methods;
				for (var i in methods) {
					var method = methods[i];
					if (method.name == methodNewVertexLighterName) {
						print("Inside ForgeBlockModelRenderer Init");
						
						for each (var instruction in method.instructions.toArray()) {
							if (
								instruction.desc && 
								instruction.desc.equals(classVertexLighterOLD)
							) {
								instruction.desc = classVertexLighterNEW;
								print("Patch 1 of ForgeBlockModelRenderer Init!");
							}
							if (
								instruction.owner && 
								instruction.owner.equals(classVertexLighterOLD)
							) {
								instruction.owner = classVertexLighterNEW;
								print("Patch 2 of ForgeBlockModelRenderer Init!");
							}
						}
					}
					
					if (
						method.name &&
						(method.name == "renderModelSmooth" || method.name == "renderModelFlat") &&
						method.desc &&
						method.desc == "(Lnet/minecraft/world/IWorldReader;Lnet/minecraft/client/renderer/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZLjava/util/Random;J)Z"
					) {
						method.desc = "(Lnet/minecraft/world/IWorldReader;Lnet/minecraft/client/renderer/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZLjava/util/Random;JLnet/minecraftforge/client/model/data/IModelData;)Z";
					}
				}

				return classNode;
			}
		}
	};
}