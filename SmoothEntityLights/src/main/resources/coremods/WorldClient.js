var constructorName = "<init>";
var classWorldNameOLD = "net/minecraft/world/World";
var classWorldNameNEW = "lakmoore/sel/world/WorldSEL";

/**
 * This function is called by Forge before any minecraft classes are loaded to
 * setup the coremod.
 *
 * @return {object} All the transformers of this coremod.
 */
function initializeCoreMod() {
	print("Initializing SEL WorldClient Coremod");
	return {
		"SEL WorldClient Transformer": {
			"target": {
				"type": "CLASS",
				"name": "net.minecraft.client.multiplayer.WorldClient"
			},
			"transformer": function(classNode) {
				print("Patching WorldClient");
				
				classNode.superName = classWorldNameNEW;
				
				var methods = classNode.methods;
				for (var i in methods) {
					var method = methods[i];
					if (method.name == constructorName) {
						print("Inside WorldClient Init");
						
						for each (var instruction in method.instructions.toArray()) {
							if (
								instruction.owner && 
								instruction.owner.equals(classWorldNameOLD) &&
								instruction.name && 
								instruction.name.equals(constructorName)
							) {
								instruction.owner = classWorldNameNEW;
								print("Patched WorldClient Init!");
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