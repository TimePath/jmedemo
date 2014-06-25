package com.timepath.tafechal14

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import com.jme3.scene.shape.Box
import com.jme3.texture.Texture
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class GameObjects {

    private AssetManager assetManager

    GameObjects(AssetManager assetManager) {
        this.assetManager = assetManager
    }

    Geometry pickup() {
        final Geometry g = new Geometry()
        g.with {
            setMesh([0.5f, 0.5f, 0.5f] as Box)
            setMaterial(assetManager.loadMaterial("Common/Materials/VertexColor.j3m"))
        }
        g.addControl(new AbstractControl() {
            float time

            @Override
            void controlUpdate(float tpf) {
                time += tpf
                time %= FastMath.TWO_PI
                g.setLocalTranslation(0, FastMath.sin(time), 0)
            }

            @Override
            void controlRender(RenderManager rm, ViewPort vp) {
            }
        })
        return g
    }

    Geometry dropBox(Vector3f pos, Vector3f vel = null, ColorRGBA color = ColorRGBA.randomColor(), float s = 3f) {
        def mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        mat.with {
            setColor("Color", color)
        }
        def phy = new Geometry(localTranslation: pos, material: mat, mesh: [0.5f * s as float, 0.5f * s as float, 0.5f * s as float] as Box)
        if (vel) {
            def rbc = new AgentControl()
            phy.with {
                addControl(rbc)
            }
            rbc.with {
                setLinearVelocity(vel)
            }
        }
        return phy
    }

    Spatial warp(float scale) {
        def warpTex = new Material(assetManager, "MatDefs/Electricity/Electricity3.j3md")
        Texture tex = assetManager.loadTexture("Textures/wall.png")
        tex.setWrap(Texture.WrapMode.Repeat)
        warpTex.setTexture("noise", tex)
        warpTex.setColor("color", new ColorRGBA(1f, 0f, 0f, 0.5f))
        warpTex.setFloat("speed", 0.01f)
        warpTex.setFloat("width", FastMath.ZERO_TOLERANCE * 100 as float)
        warpTex.setVector2("texScale", new Vector2f(1, 1))
        warpTex.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off)
        warpTex.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha)
        float half = scale / 2 as float
        Geometry warp = new Geometry("warp", [scale / 2 as float, scale / 2 as float, scale / 2 as float] as Box)
        warp.setLocalTranslation(half, half, half)
        warp.setMaterial(warpTex)
        warp.setQueueBucket(RenderQueue.Bucket.Transparent)
        return warp
    }

}
