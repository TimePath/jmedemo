package com.timepath.tafechal14
import com.jme3.app.SimpleApplication
import com.jme3.bullet.BulletAppState
import com.jme3.bullet.control.BetterCharacterControl
import com.jme3.bullet.control.PhysicsControl
import com.jme3.bullet.debug.BulletDebugAppState
import com.jme3.collision.CollisionResult
import com.jme3.collision.CollisionResults
import com.jme3.input.MouseInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.MouseButtonTrigger
import com.jme3.light.AmbientLight
import com.jme3.light.PointLight
import com.jme3.math.ColorRGBA
import com.jme3.math.Ray
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.SceneGraphVisitorAdapter
import com.jme3.scene.Spatial
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log
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

    static void main(String[] args) {
        new Main().start()
    }

    void binds() {
        String ACTION = "Something"
        inputManager.addMapping(ACTION, new MouseButtonTrigger(MouseInput.BUTTON_LEFT))
        inputManager.addListener(new ActionListener() {
            CollisionResults results = new CollisionResults()

            @Override
            void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) return

                levelNode.collideWith(new Ray(cam.getLocation(), cam.getDirection()), results)
                if (results.size() == 0) return
                CollisionResult closest = results.closestCollision
                LOG.info(closest.toString())
                add objects.dropBox(closest.contactPoint.add(closest.contactNormal.normalize().multLocal(2)),
                        closest.contactNormal.normalize().multLocal(50))
                results.clear()
            }
        }, ACTION)
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

    @CompileDynamic
    @Override
    void simpleInitApp() {
        bullet = new BulletAppState()
        stateManager.attach(bullet)
        debug = new MyBulletDebugAppState(bullet.physicsSpace)
        objects = new GameObjects(assetManager)

        flyCam.moveSpeed = 30

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
                jumpForce = [0, 80, 0] as Vector3f
            }
        }
        add characterNode

        rootNode.addLight new AmbientLight(color: ColorRGBA.White)
        rootNode.addLight camLight = new PointLight(color: ColorRGBA.White, radius: 20)

        // mid
        Node wmain = World.generate(assetManager)
        int o = 100
        float[] reps = [
                0, 0, 0, // main
//                -o, 0, 0, // left
//                o, 0, 0, // right
//                0, o, 0, // up
//                0, -o, 0, // down
//                0, 0, o, // in
//                0, 0, -o, // out
//                // z
//                o, o, 0, // tr
//                o, -o, 0, // dr
//                -o, o, 0, // tl
//                -o, -o, 0, // dl
//                // x
//                0, o, o, // ti
//                0, o, -o, // to
//                0, -o, o, // di
//                0, -o, -o, // do
//                // y
//                o, 0, o, // ir
//                o, 0, -o, // or
//                -o, 0, o, // il
//                -o, 0, -o, // or
//                // top corners
//                o, o, o, // tr
//                -o, o, o, // tl
//                o, o, -o, // dr
//                -o, o, -o, // dl
//                // bottom corners
//                o, -o, o, // tr
//                -o, -o, o, // tl
//                o, -o, -o, // dr
//                -o, -o, -o, // dl
        ]
        levelNode = new Node("Level")
        for (int i = 0; i < reps.length; i += 3) {
            Spatial gho = wmain.clone()
            Vector3f dir = new Vector3f(reps[i], reps[i + 1], reps[i + 2])
            gho.setLocalTranslation(dir)
            levelNode.attachChild(gho)
            if (i == 0) {
                gho.breadthFirstTraversal(new SceneGraphVisitorAdapter() {
                    @Override
                    void visit(Geometry geom) {
                        bullet.getPhysicsSpace().add(geom)
                    }
                })
            }
        }
        rootNode.attachChild(levelNode)
    }

    @Override
    void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf)
        physicsCharacter.jump()
        camLight.setPosition(cam.getLocation())
    }
}
