/**
 * A really simple CoreMod that replaces the ClassInheritanceMultiMap, used to track 
 * the Entities in a Chunk, with a super-type that is thread-safe, uses Set instead of
 * List, and checks for equality before checking for inheritance (faster and safer)
 */

var constructorName = "<init>";
var classMultiMapNameOLD = "net/minecraft/util/ClassInheritanceMultiMap";
var classMultiMapNameNEW = "lakmoore/sel/common/SELClassInheritanceMultiMap";

/**
 * This function is called by Forge before any minecraft classes are loaded to
 * setup the coremod.
 *
 * @return {object} All the transformers of this coremod.
 */
function initializeCoreMod() {
	print("Initializing SEL Chunk Coremod");
	return {
		"SEL Chunk Transformer": {
			"target": {
				"type": "CLASS",
				"name": "net.minecraft.world.chunk.Chunk"
			},
			"transformer": function(classNode) {
				print("Patching Chunk");
				
				var methods = classNode.methods;
				for (var i in methods) {
					var method = methods[i];
					if (method.name == constructorName) {
						print("Inside Chunk Init");
						
						for each (var instruction in method.instructions.toArray()) {
							if (
								instruction.desc && 
								instruction.desc.equals(classMultiMapNameOLD)
							) {
								instruction.desc = classMultiMapNameNEW;
								print("Patch 1 of Chunk Init!");
							}
							if (
								instruction.owner && 
								instruction.owner.equals(classMultiMapNameOLD)
							) {
								instruction.owner = classMultiMapNameNEW;
								print("Patch 2 of Chunk Init!");
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