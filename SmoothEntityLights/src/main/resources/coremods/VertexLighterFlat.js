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
						
						for (var j in method.instructions) {
							var instruction = method.instructions[j];
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
						break;
					}
				}

				return classNode;
			}
		}
	};
}