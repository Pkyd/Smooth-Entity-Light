var constructorName = "<init>";
var classVertexLighterOLD = "net/minecraftforge/client/model/pipeline/VertexLighterFlat";
var classVertexLighterNEW = "lakmoore/sel/client/model/pipeline/VertexLighterSEL";

/**
 * This function is called by Forge before any minecraft classes are loaded to
 * setup the coremod.
 *
 * @return {object} All the transformers of this coremod.
 */
function initializeCoreMod() {
	print("Initializing SEL VertexLighterSmoothAo Coremod");
	return {
		"SEL VertexLighterSmoothAo Transformer": {
			"target": {
				"type": "CLASS",
				"name": "net.minecraftforge.client.model.pipeline.VertexLighterSmoothAo"
			},
			"transformer": function(classNode) {
				print("Patching VertexLighterSmoothAo");
				classNode.superName = classVertexLighterNEW;
				var methods = classNode.methods;
				for (var i in methods) {
					var method = methods[i];
					if (method.name == constructorName) {
						print("Inside VertexLighterSmoothAo Init");
						
						for (var j in method.instructions) {
							var instruction = method.instructions[j];
							if (
								instruction.owner && 
								instruction.owner.equals(classVertexLighterOLD) &&
								instruction.name &&
								instruction.name.equals(constructorName)
							) {
								instruction.owner = classVertexLighterNEW;
								print("Patched VertexLighterSmoothAo Init!");
								break;
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