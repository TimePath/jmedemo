package com.timepath.tafechal14

import com.jme3.app.FlyCamAppState
import com.jme3.app.SimpleApplication
import com.jme3.bullet.BulletAppState
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.PhysicsTickListener
import com.jme3.bullet.control.BetterCharacterControl
import com.jme3.bullet.control.PhysicsControl
import com.jme3.bullet.control.RigidBodyControl
import com.jme3.bullet.debug.BulletDebugAppState
import com.jme3.collision.CollisionResult
import com.jme3.collision.CollisionResults
import com.jme3.input.MouseInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.MouseButtonTrigger
import com.jme3.light.AmbientLight
import com.jme3.light.PointLight
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Ray
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.SceneGraphVisitorAdapter
import com.jme3.scene.Spatial
import com.jme3.texture.Texture
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

import static com.jme3.bullet.BulletAppState.ThreadingType.PARALLEL

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class Main extends SimpleApplication {

    BulletAppState bullet
    BetterCharacterControl physicsCharacter
    PointLight camLight
    Node levelNode
    BulletDebugAppState debug
    GameObjects objects
    CustomCamera customCamera

    static void main(String[] args) {
        new Main().start()
    }

    void binds() {
        stateManager.detach(stateManager.getState(FlyCamAppState))
        customCamera = new CustomCamera(camera: cam, inputManager: inputManager, sixDof: true, fov: 60)

        String ACTION = "Something"
        inputManager.addMapping(ACTION, new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE))
        inputManager.addListener(new ActionListener() {
            CollisionResults results = new CollisionResults()

            @Override
            void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) return

                levelNode.collideWith(new Ray(cam.location, cam.direction), results)
                if (results.size() == 0) return
                CollisionResult closest = results.closestCollision
                LOG.info(closest.toString())
                add objects.dropBox(closest.contactPoint.add(closest.contactNormal.normalize().multLocal(2)),
                        closest.contactNormal.normalize().multLocal(50))
                results.clear()
            }
        }, ACTION)
        String THROW = "Throw"
        inputManager.addMapping(THROW, new MouseButtonTrigger(MouseInput.BUTTON_LEFT))
        inputManager.addListener(new ActionListener() {
            @Override
            void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) return
                add objects.dropBox(cam.location, cam.direction.normalize().multLocal(50))
            }
        }, THROW)
        String DEBUG = "Debug"
        inputManager.addMapping(DEBUG, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT))
        inputManager.addListener(new ActionListener() {
            @Override
            void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) return
                boolean d = stateManager.detach(debug)
                if (!d) stateManager.attach(debug)
            }
        }, DEBUG)
    }

    void add(Spatial s) {
        rootNode.attachChild(s)
        if (s.getControl(PhysicsControl) != null) bullet.physicsSpace.add(s)
    }

    @Override
    void simpleInitApp() {
        viewPort.backgroundColor = ColorRGBA.White
        bullet = new BulletAppState()
        bullet.broadphaseType = PhysicsSpace.BroadphaseType.AXIS_SWEEP_3
        bullet.worldMin = Vector3f.ZERO
        bullet.worldMax = Vector3f.UNIT_XYZ.mult(100)
        bullet.threadingType = PARALLEL
        stateManager.attach(bullet)
        bullet.physicsSpace.gravity = Vector3f.ZERO
        debug = new MyBulletDebugAppState(bullet.physicsSpace)
        objects = new GameObjects(assetManager)

        renderer.mainFrameBufferSrgb = true
        renderer.linearizeSrgbImages = true

        bullet.physicsSpace.addTickListener(new PhysicsTickListener() {
            @Override
            void prePhysicsTick(final PhysicsSpace space, final float tpf) {
                for (prb in space.rigidBodyList) {
                    if (!(prb instanceof RigidBodyControl)) continue
                    if (prb.mass <= 0) continue
                    def pos = prb.physicsLocation
                    pos.x = (pos.x + 100f) % 100f
                    pos.y = (pos.y + 100f) % 100f
                    pos.z = (pos.z + 100f) % 100f
                    prb.physicsLocation = pos
                }
            }

            @Override
            void physicsTick(final PhysicsSpace space, final float tpf) {

            }
        })

        add objects.pickup()
        add objects.warp(100)

        binds()

        def characterNode = new Node("character node")
        characterNode.with {
            setLocalTranslation(5, 50, 5)
            def model = assetManager.loadModel("Models/Jaime/Jaime.j3o")
            model.with {
                setLocalScale(1.5f)
            }
            attachChild(model)
            addControl physicsCharacter = new BetterCharacterControl(0.4f, 1.8f, 80f)
            physicsCharacter.with {
                setJumpForce new Vector3f(0f, 80f, 0f)
            }
        }
        add characterNode

        // mid
        float o = 100f
        float[] reps = [
                0, 0, 0, // main
                -o, 0, 0, // left
                o, 0, 0, // right
                0, o, 0, // up
                0, -o, 0, // down
                0, 0, o, // in
                0, 0, -o, // out
                // z
                o, o, 0, // tr
                o, -o, 0, // dr
                -o, o, 0, // tl
                -o, -o, 0, // dl
                // x
                0, o, o, // ti
                0, o, -o, // to
                0, -o, o, // di
                0, -o, -o, // do
                // y
                o, 0, o, // ir
                o, 0, -o, // or
                -o, 0, o, // il
                -o, 0, -o, // or
                // top corners
                o, o, o, // tr
                -o, o, o, // tl
                o, o, -o, // dr
                -o, o, -o, // dl
                // bottom corners
                o, -o, o, // tr
                -o, -o, o, // tl
                o, -o, -o, // dr
                -o, -o, -o, // dl
        ]
        def mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md")
        def tex = assetManager.loadTexture("Textures/wall.png")
        tex.setWrap(Texture.WrapMode.Repeat)
        mat.setBoolean("VertexLighting", false)
        mat.setBoolean("HighQuality", true)
        mat.setBoolean("LowQuality", false)
        mat.setTexture("DiffuseMap", tex)
        def world = World.node(mat)
        levelNode = new Node("Level")
        for (int i = 0; i < reps.length; i += 3) {
            def sector = world.clone()
            sector.move(reps[i], reps[i + 1], reps[i + 2])
            levelNode.attachChild sector

            sector.breadthFirstTraversal(new SceneGraphVisitorAdapter() {
                @Override
                void visit(Geometry geom) {
                    bullet.physicsSpace.add(geom)
                }
            })
        }
        rootNode.attachChild(levelNode)

        rootNode.addLight new AmbientLight(color: ColorRGBA.White)
        rootNode.addLight camLight = new PointLight(color: ColorRGBA.White.mult(1), radius: 100)
    }

    @Override
    void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf)
        physicsCharacter.jump()
        camLight?.setPosition(cam.location)

        def pos = cam.location
        pos.x = (pos.x + 100f) % 100f
        pos.y = (pos.y + 100f) % 100f
        pos.z = (pos.z + 100f) % 100f
    }
}
