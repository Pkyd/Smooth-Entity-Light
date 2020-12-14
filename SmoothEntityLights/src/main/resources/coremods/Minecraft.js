var targetPackageName = "net.minecraft.client";
var targetClassName = "Minecraft";
var constructorName = "<init>";
var classReplacementNameOLD = "net/minecraft/client/renderer/entity/EntityRendererManager";
var classReplacementNameNEW = "lakmoore/sel/world/SELEntityRendererManager";

/**
 * This function is called by Forge before any minecraft classes are loaded to
 * setup the coremod.
 *
 * @return {object} All the transformers of this coremod.
 */
function initializeCoreMod() {
	print("Initializing SEL " + targetClassName + " Coremod");
	return {
		"SEL Minecraft Transformer": {
			"target": {
				"type": "CLASS",
				"name": targetPackageName + "." + targetClassName
			},
			"transformer": function(classNode) {
				print("Patching " + targetClassName);
				
				var methods = classNode.methods;
				for (var i in methods) {
					var method = methods[i];
					if (method.name == constructorName) {
						print("Inside " + targetClassName + " Init");
						
						for each (var instruction in method.instructions.toArray()) {
							if (
								instruction.desc && 
								instruction.desc.equals(classReplacementNameOLD)
							) {
								instruction.desc = classReplacementNameNEW;
								print("Patch 1 of " + targetClassName + " Init!");
							}
							if (
								instruction.owner && 
								instruction.owner.equals(classReplacementNameOLD)
							) {
								instruction.owner = classReplacementNameNEW;
								print("Patch 2 of " + targetClassName + " Init!");
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