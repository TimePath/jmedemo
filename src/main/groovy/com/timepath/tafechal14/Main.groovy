package com.timepath.tafechal14

import com.jme3.app.SimpleApplication
import com.jme3.bullet.BulletAppState
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.SphereCollisionShape
import com.jme3.bullet.control.PhysicsControl
import com.jme3.bullet.control.RigidBodyControl
import com.jme3.bullet.debug.BulletDebugAppState
import com.jme3.input.KeyInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.KeyTrigger
import com.jme3.light.AmbientLight
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.network.*
import com.jme3.post.FilterPostProcessor
import com.jme3.post.filters.CartoonEdgeFilter
import com.jme3.post.filters.FogFilter
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.SceneGraphVisitorAdapter
import com.jme3.scene.Spatial
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext
import com.jme3.texture.Texture
import com.timepath.tafechal14.net.Server
import com.timepath.tafechal14.net.SharedNetworking
import com.timepath.tafechal14.net.SyncControl
import com.timepath.tafechal14.net.messages.EntityUpdate
import com.timepath.tafechal14.net.messages.HelloMessage
import com.timepath.tafechal14.net.messages.PlayerUpdate
import com.timepath.tafechal14.net.messages.SeedMessage
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

import java.util.concurrent.Callable

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class Main extends SimpleApplication {

    static void main(String[] args) {
        SharedNetworking.instance
        Thread.start { new Server().start(JmeContext.Type.Headless) }
        Thread.sleep(1000)
        new Main().with {
            settings = new AppSettings(true)
            pauseOnLostFocus = false
            settings.title = "HyperCube"
            showSettings = true
            start()
        }
    }

    GameObjects objectFactory
    float scale = 30
    BulletAppState bullet

    ClientListener cl = new ClientListener()

    Client conn

    Map<Integer, Spatial> entities = [:]
    Map<Integer, Spatial> players = [:]

    void enq(Closure c) {
        enqueue new Callable<Void>() {
            @Override
            Void call() throws Exception {
                c()
                return null
            }
        }
    }

    class ClientListener implements MessageListener<Client> {

        @CompileDynamic
        @Override
        void messageReceived(final Client source, final Message m) {
            enq { handle(source, m) }
        }

        void handle(final Client source, final HelloMessage m) {
            println "${m} is connected"
        }

        void handle(final Client source, final PlayerUpdate m) {
            if (!players[m.id]) {
                println "New client at ${m.pos}, ${m.color}"
                players[m.id] = objectFactory.dropBox(m.pos, Vector3f.ZERO, m.color)
                rootNode.attachChild players[m.id]
            }
            players[m.id].localTranslation = m.pos
        }

        void handle(final Client source, final EntityUpdate m) {
            if (!entities[m.id]) {
                println "New enemy at ${m.pos}, ${m.color}"
                entities[m.id] = objectFactory.dropBox(m.pos, Vector3f.ZERO, m.color)
                rootNode.attachChild entities[m.id]
            }
            entities[m.id].localTranslation = m.pos
        }

        void handle(final Client source, final SeedMessage m) {
            initWorld(m.seed)
            debug = new MyBulletDebugAppState(bullet.physicsSpace)

            // Gamma correction
            renderer.mainFrameBufferSrgb = true
            renderer.linearizeSrgbImages = true

            fpp = new FilterPostProcessor(assetManager)
            fpp.addFilter new FogFilter(fogColor: new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f),
                    fogDistance: 100 * scale as float,
                    fogDensity: 10.0f)
            fpp.addFilter new CartoonEdgeFilter(edgeColor: ColorRGBA.Black, edgeWidth: 0.5f, edgeIntensity: 1.0f)
            viewPort.addProcessor(fpp)
            bullet.physicsSpace.addTickListener(new PhysicsWrap(scale: 10 * scale))

            input()

            def height = 2
            def characterNode = new Node("character node")
            characterNode.with {
                setLocalTranslation(5, 0 + height, 5)
                float size = 1.5f
                addControl physicsCharacter = new RigidBodyControl(new SphereCollisionShape(size), 1)
                addControl new SyncControl(broadcast: { AbstractMessage msg -> conn.send(msg) },
                        create: { new PlayerUpdate(pos: characterNode.localTranslation) })
                addControl(new JointControl(target: { Vector3f v -> cam.location = v.add(0, 1, 0) }))
            }
            add characterNode

            rootNode.addLight new AmbientLight(color: ColorRGBA.White)

            init = true
            println "Initialized"
        }
    }

    RigidBodyControl physicsCharacter
    Node levelNode
    BulletDebugAppState debug
    boolean init

    CustomCamera customCamera
    Map<String, Boolean> keys = [:].withDefault { false }
    Vector3f move = new Vector3f()
    FilterPostProcessor fpp
    float maxspeed = 40

    void add(Spatial s) {
        rootNode.attachChild(s)
        if (s.getControl(PhysicsControl) != null) bullet.physicsSpace.add(s)
    }

    @Override
    void simpleInitApp() {
        bullet = new BulletAppState()
        bullet.broadphaseType = PhysicsSpace.BroadphaseType.AXIS_SWEEP_3
        bullet.worldMin = Vector3f.ZERO
        bullet.worldMax = Vector3f.UNIT_XYZ.mult(10 * scale as float)
//        bullet.threadingType = BulletAppState.ThreadingType.PARALLEL
        stateManager.attach(bullet)
        bullet.physicsSpace.gravity = Vector3f.ZERO

        objectFactory = new GameObjects(assetManager)

        conn = Network.connectToServer("127.0.0.1", SharedNetworking.PORT)
        conn.addMessageListener(cl)
        conn.start()
        conn.send new HelloMessage(name: InetAddress.getLocalHost().getHostName())
    }

    def initWorld(long seed) {
        // mid
        float o = 10 * scale
        float[] reps = [0, 0, 0,
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
        mat.with {
            setBoolean("VertexLighting", false)
            setBoolean("HighQuality", false)
            setBoolean("LowQuality", false)
            setTexture("DiffuseMap", tex)
        }
        def world = World.node(mat, scale, seed)
        levelNode = new Node("Level")
        for (int i = 0; i < reps.length; i += 3) {
            def sector = world.clone()
            sector.move(reps[i], reps[i + 1], reps[i + 2])
            levelNode.attachChild sector
            if (!i)
                sector.breadthFirstTraversal(new SceneGraphVisitorAdapter() {
                    @Override
                    void visit(Geometry geom) {
                        bullet.physicsSpace.add(geom)
                    }
                })
        }
        levelNode.attachChild objectFactory.warp(10 * scale as float)
        rootNode.attachChild(levelNode)
    }

    static String W = "W", S = "S", A = "A", D = "D"

    void input() {
//        stateManager.detach(stateManager.getState(FlyCamAppState))
        customCamera = new CustomCamera(camera: cam, inputManager: inputManager, sixDof: true, fov: 60)

        String DEBUG = "Debug"
        inputManager.addMapping(DEBUG, new KeyTrigger(KeyInput.KEY_BACK))
        inputManager.addListener(new ActionListener() {
            @Override
            void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) return
                boolean d = stateManager.detach(debug)
                if (!d) stateManager.attach(debug)
            }
        }, DEBUG)
        inputManager.addMapping(W, new KeyTrigger(KeyInput.KEY_W))
        inputManager.addMapping(A, new KeyTrigger(KeyInput.KEY_A))
        inputManager.addMapping(D, new KeyTrigger(KeyInput.KEY_D))
        inputManager.addListener(new ActionListener() {
            @Override
            void onAction(String name, boolean isPressed, float tpf) {
                keys[name] = isPressed
            }
        }, W, A, D)

    }

    @Override
    void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf)

        if (!init) return

        move.zero()
        def fwd = false
        for (Map.Entry<String, Boolean> e in keys.entrySet()) {
            if (!e.value) continue

            switch (e.key) {
                case W: fwd = true; break
                case A: move.addLocal cam.left.normalize(); break
                case D: move.addLocal cam.left.normalize().negate(); break
                default: break
            }
        }
        def speed = 10

        def thrust = cam.direction.normalize().multLocal(fwd ? speed : 0)
        def dot = thrust.dot(cam.direction)
        thrust.addLocal(move.mult(speed))
        physicsCharacter.applyCentralForce thrust.mult(dot)

        if (physicsCharacter.linearVelocity.lengthSquared() > maxspeed**2) {
            def clamped = physicsCharacter.linearVelocity.normalize().multLocal(maxspeed)
            physicsCharacter.linearVelocity = clamped
        }
        def fovTarget = (60 + (physicsCharacter.linearVelocity.length())) as float
        def fov = customCamera.fov
        def zt = FastMath.ZERO_TOLERANCE * 1000
        customCamera.fov = fov + tpf * (fovTarget + zt < fov ? -1 : fovTarget - zt > fov ? 1 : 0) * 50
    }
}
