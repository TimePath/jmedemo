package com.timepath.tafechal14.net

import com.jme3.app.SimpleApplication
import com.jme3.bullet.BulletAppState
import com.jme3.bullet.PhysicsSpace
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.network.*
import com.jme3.network.HostedConnection as Connection
import com.jme3.network.Server as JmeServer
import com.jme3.scene.Geometry
import com.jme3.system.JmeContext
import com.timepath.tafechal14.AgentControl
import com.timepath.tafechal14.BouncyListener
import com.timepath.tafechal14.GameObjects
import com.timepath.tafechal14.PhysicsWrap
import com.timepath.tafechal14.net.Server
import com.timepath.tafechal14.net.messages.EntityUpdate
import com.timepath.tafechal14.net.messages.HelloMessage
import com.timepath.tafechal14.net.messages.PlayerUpdate
import com.timepath.tafechal14.net.messages.SeedMessage
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
class Server extends SimpleApplication {

    int lastId

    static void main(String[] args) {
        SharedNetworking.instance
        new Server().start(JmeContext.Type.Headless);
    }

    ServerListener sl = new ServerListener()
    JmeServer server
    GameObjects objectFactory
    float scale = 30
    BulletAppState bullet
    float ticks

    def createServer() {
        server = Network.createServer(SharedNetworking.PORT)
        server.addMessageListener(sl);
        server.start()
    }

    @Override
    void simpleInitApp() {
        createServer()

        bullet = new BulletAppState()
        bullet.broadphaseType = PhysicsSpace.BroadphaseType.AXIS_SWEEP_3
        bullet.worldMin = Vector3f.ZERO
        bullet.worldMax = Vector3f.UNIT_XYZ.mult(10 * scale as float)
//        bullet.threadingType = BulletAppState.ThreadingType.PARALLEL
        stateManager.attach(bullet)
        bullet.physicsSpace.gravity = Vector3f.ZERO

        bullet.physicsSpace.addCollisionListener(new BouncyListener(scale: 10))
        bullet.physicsSpace.addTickListener(new PhysicsWrap(scale: 10 * scale))

        objectFactory = new GameObjects(assetManager)
    }

    long seed = System.nanoTime()

    @Override
    void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf)
        def cycle = 5
        ticks += tpf
        if (ticks > cycle) {
            ticks %= cycle
            Geometry box = objectFactory.dropBox(Vector3f.UNIT_XYZ.mult(5), Vector3f.ZERO)
            box.addControl new SyncControl(id: lastId++, broadcast: { AbstractMessage m -> server.broadcast(m) })
            box.getControl(AgentControl).target = {
                float last = Float.MAX_VALUE
                Vector3f closest = null
                for (Map.Entry<Integer, Player> e in players.entrySet()) {
                    def next = box.localTranslation.distance(e.value.localTranslation)
                    if (next < last) closest = box.localTranslation
                }
                return closest
            }
            rootNode.attachChild box
            bullet.physicsSpace.add box
        }
    }

    Map<Integer, Player> players = [:]

    static class Player {
        String name
        Vector3f localTranslation = new Vector3f()
        ColorRGBA color = ColorRGBA.randomColor()
    }

    class ServerListener implements MessageListener<Connection> {

        @CompileDynamic
        @Override
        void messageReceived(final Connection source, final Message m) {
            handle(source, m)
        }

        void handle(final Connection source, final HelloMessage m) {
            m.with { id = source.id }
            players[source.id] = new Player(name: m.name)
            // Send existing players
            for (conn in server.connections) {
                source.send(new HelloMessage(id: conn.id, name: players[conn.id].name))
            }
            server.broadcast(Filters.notEqualTo(source), new HelloMessage(id: source.id, name: m.name))
            source.send(new SeedMessage(seed: seed))
        }

        void handle(final Connection source, final EntityUpdate m) {
        }

        void handle(final Connection source, final PlayerUpdate m) {
            m.with {
                id = source.id
                color = players[id].color
            }
            server.broadcast(Filters.notEqualTo(source), m)
        }
    }
}
