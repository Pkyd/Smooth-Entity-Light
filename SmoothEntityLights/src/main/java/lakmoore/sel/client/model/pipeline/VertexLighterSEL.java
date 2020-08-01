package lakmoore.sel.client.model.pipeline;

import javax.vecmath.Vector3f;

import lakmoore.sel.capabilities.ILitChunkCache;
import lakmoore.sel.client.ClientProxy;
import lakmoore.sel.client.LightUtils;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.client.model.pipeline.VertexLighterFlat;

public class VertexLighterSEL extends VertexLighterFlat {
	
    protected int tint = -1;
    protected boolean diffuse = true;

    public VertexLighterSEL(BlockColors blockColorsIn) {
		super(blockColorsIn);
	}
    
    @Override
    protected void processQuad()
    {
        float[][] position = quadData[posIndex];
        float[][] normal = null;
        float[][] lightmap = quadData[lightmapIndex];
        float[][] color = quadData[colorIndex];

        if (dataLength[normalIndex] >= 3
            && (quadData[normalIndex][0][0] != -1
            ||  quadData[normalIndex][0][1] != -1
            ||  quadData[normalIndex][0][2] != -1))
        {
            normal = quadData[normalIndex];
        }
        else // normals must be generated
        {
            normal = new float[4][4];
            Vector3f v1 = new Vector3f(position[3]);
            Vector3f t = new Vector3f(position[1]);
            Vector3f v2 = new Vector3f(position[2]);
            v1.sub(t);
            t.set(position[0]);
            v2.sub(t);
            v1.cross(v2, v1);
            v1.normalize();
            for(int v = 0; v < 4; v++)
            {
                normal[v][0] = v1.x;
                normal[v][1] = v1.y;
                normal[v][2] = v1.z;
                normal[v][3] = 0;
            }
        }
                        
        int multiplier = -1;
        if(tint != -1)
        {
            multiplier = blockInfo.getColorMultiplier(tint);
        }
        
        VertexFormat format = parent.getVertexFormat();
        int count = format.getElementCount();
                
        for(int v = 0; v < 4; v++)
        {            
        	position[v][0] += blockInfo.getShx();
            position[v][1] += blockInfo.getShy();
            position[v][2] += blockInfo.getShz();

            float x = position[v][0] - .5f;
            float y = position[v][1] - .5f;
            float z = position[v][2] - .5f;

            //if(blockInfo.getBlock().isFullCube())
            {
                x += normal[v][0] * .5f;
                y += normal[v][1] * .5f;
                z += normal[v][2] * .5f;
            }

        	float blockLight = lightmap[v][0], skyLight = lightmap[v][1];
            updateLightmap(normal[v], lightmap[v], x, y, z);
            if(dataLength[lightmapIndex] > 1)
            {
                if(blockLight > lightmap[v][0]) lightmap[v][0] = blockLight;
                if(skyLight > lightmap[v][1]) lightmap[v][1] = skyLight;
            }
            updateColor(normal[v], color[v], x, y, z, tint, multiplier);
            if(diffuse)
            {
                float d = LightUtil.diffuseLight(normal[v][0], normal[v][1], normal[v][2]);
                for(int i = 0; i < 3; i++)
                {
                    color[v][i] *= d;
                }
            }
            if(EntityRenderer.anaglyphEnable)
            {
                applyAnaglyph(color[v]);
            }
            
            Vec3d vertPos = new Vec3d(this.blockInfo.getBlockPos()).add(position[v][0], position[v][1], position[v][2]);
            ILitChunkCache lcc = LightUtils.getLitChunkCache(ClientProxy.mcinstance.world, (int)Math.round(vertPos.x) >> 4, (int)Math.round(vertPos.z) >> 4);
        	// Save the light value into the cache
            lcc.setMCVertexLight(vertPos.x, vertPos.y, vertPos.z, (short)Math.round(lightmap[v][0] * 0x7FFF)); 
            lightmap[v][0] = (float)lcc.getVertexLight(vertPos.x, vertPos.y, vertPos.z) / 0x7FFF; 

        }
                        
        int[] order = { 0, 1, 2, 3 };
        int[] flipped = { 1, 2, 3, 0 };

        // re-order the triangles in the quad so brightness is always blended smoothly
        if (
        		(
	        		(lightmap[3][0] + lightmap[3][1]) 
	        		-
	        		(lightmap[0][0] + lightmap[0][1])
        		)
        	<
        		(
	        		(lightmap[2][0] + lightmap[2][1]) 
	        		-
	        		(lightmap[1][0] + lightmap[1][1])
        		)
            	
    	) {
        	order = flipped;
        }

        for(int v = 0; v < 4; v++)
        {
            // no need for remapping cause all we could've done is add 1 element to the end
            for(int e = 0; e < count; e++)
            {
                VertexFormatElement element = format.getElement(e);
                switch(element.getUsage())
                {
                    case POSITION:
                        // position adding moved to VertexBufferConsumer due to x and z not fitting completely into a float
                        /*float[] pos = new float[4];
                        System.arraycopy(position[v], 0, pos, 0, position[v].length);
                        pos[0] += blockInfo.getBlockPos().getX();
                        pos[1] += blockInfo.getBlockPos().getY();
                        pos[2] += blockInfo.getBlockPos().getZ();*/
                        parent.put(e, position[order[v]]);
                        break;
                    case NORMAL: if(normalIndex != -1)
                    {
                        parent.put(e, normal[order[v]]);
                        break;
                    }
                    case COLOR:
                    	boolean debug = false;
                    	if (debug) {
                        	switch (order[v]) {
                    		case 0: //Red
                                parent.put(e, new float[]{230f/255f, 25f/255f, 75f/255f, 1f });
                    			break;
                    		case 1: //Yellow
                                parent.put(e, new float[]{1f, 1f, 25f/255f, 1f });
                    			break;
                    		case 2: //Green
                                parent.put(e, new float[]{60f/255f, 180f/255f, 75f/255f, 1f });
                    			break;
                    		case 3: //Blue
                                parent.put(e, new float[]{0f, 130f/255f, 200f/255f, 1f });
                    			break;
                        	};                    		
                    	} else {
                          parent.put(e, color[order[v]]);                    		
                    	}
                        break;
                    case UV: if(element.getIndex() == 1)
                    {
                        parent.put(e, lightmap[order[v]]);
                        break;
                    }
                    default:
                        parent.put(e, quadData[e][order[v]]);
                }
            }

        }

        tint = -1;
    }
   
    @Override
    public void setQuadTint(int tint)
    {
    	super.setQuadTint(tint);
        this.tint = tint;
        if (tint > 0) {
            System.out.println("Tint set to " + tint);        	
        }
    }
    
    @Override
    public void setApplyDiffuseLighting(boolean diffuse)
    {
    	super.setApplyDiffuseLighting(diffuse);
        this.diffuse = diffuse;
    }

    
}
