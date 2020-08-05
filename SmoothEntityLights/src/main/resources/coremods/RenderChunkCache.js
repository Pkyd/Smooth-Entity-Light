var constructorName = "<init>";
var classChunkCacheNameOLD = "net/minecraft/client/renderer/chunk/RenderChunkCache";
var classChunkCacheNameNEW = "lakmoore/sel/world/ChunkCacheSEL";

/**
 * This function is called by Forge before any minecraft classes are loaded to
 * setup the coremod.
 *
 * @return {object} All the transformers of this coremod.
 */
function initializeCoreMod() {
	print("Initializing SEL RenderChunkCache Coremod");
	return {
		"SEL RenderChunkCache Transformer": {
			"target": {
				"type": "CLASS",
				"name": "net.minecraft.client.renderer.chunk.RenderChunkCache"
			},
			"transformer": function(classNode) {
				print("Patching RenderChunkCache");				
				
				var methods = classNode.methods;
				for (var i in methods) {
					var method = methods[i];
					if (method.name == "generateCache") {
						print("Inside RenderChunkCache::generateCache()");
						
						for each (var instruction in method.instructions.toArray()) {
							if (
								instruction.desc && 
								instruction.desc.equals(classChunkCacheNameOLD)
							) {
								instruction.desc = classChunkCacheNameNEW;
								print("Patched RenderChunkCache Init 1/2");
							}
							if (
								instruction.owner && 
								instruction.owner.equals(classChunkCacheNameOLD)
							) {
								instruction.owner = classChunkCacheNameNEW;
								print("Patched RenderChunkCache Init 2/2");
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