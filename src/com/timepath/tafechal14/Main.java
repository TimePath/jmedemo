package com.timepath.tafechal14;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.debug.BulletDebugAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import java.util.logging.Logger;

public class Main extends SimpleApplication {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private BulletAppState         bullet;
    private BetterCharacterControl physicsCharacter;
    private Material               warpTex;
    private PointLight             camLight;
    private Node                   levelNode;
    private BulletDebugAppState    debug;

    public static void main(String[] args) {
        new Main().start();
    }

    void pickup() {
        final Geometry g = new Geometry();
        g.setMesh(new Box(.5f, .5f, .5f));
        g.setMaterial(assetManager.loadMaterial("Common/Materials/VertexColor.j3m"));
        g.addControl(new AbstractControl() {
            float offset;

            @Override
            protected void controlUpdate(final float tpf) {
                g.setLocalTranslation(0, FastMath.sin(offset += tpf), 0);
            }

            @Override
            protected void controlRender(final RenderManager rm, final ViewPort vp) {
            }
        });
        rootNode.attachChild(g);
    }

    void bind() {
        final String ACTION = "Something";
        this.inputManager.addMapping(ACTION, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        this.inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(final String name, final boolean isPressed, final float tpf) {
                if(!isPressed) return;
                final CollisionResults results = new CollisionResults();
                levelNode.collideWith(new Ray(cam.getLocation(), cam.getDirection()), results);
                if(results.size() == 0) return;
                CollisionResult closest = results.getClosestCollision();
                LOG.info(closest.toString());
                dropBox(closest.getContactPoint().add(closest.getContactNormal().normalize().mult(2)),
                        closest.getContactNormal().normalize().mult(50));
            }
        }, ACTION);
        final String DEBUG = "Debug";
        this.inputManager.addMapping(DEBUG, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        this.inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(final String name, final boolean isPressed, final float tpf) {
                if(!isPressed) return;
                boolean d = stateManager.detach(debug);
                if(!d) stateManager.attach(debug);
            }
        }, DEBUG);
    }

    void dropBox(final Vector3f pos, final Vector3f vel) {
        final Geometry phy = new Geometry() {{
            setMesh(new Box(.5f, .5f, .5f));
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.randomColor());
            setMaterial(mat);
            setLocalTranslation(0, 1.5f, 0);
        }};
        phy.setLocalTranslation(pos);
        rootNode.attachChild(phy);
        final RigidBodyControl rbc = new RigidBodyControl();
        phy.addControl(rbc);
        rbc.setLinearVelocity(vel);
        bullet.getPhysicsSpace().add(phy);
    }

    @Override
    public void simpleInitApp() {
        bullet = new BulletAppState();
        stateManager.attach(bullet);
        bind();
        //        rootNode.attachChild(warp(100));
        debug = new BulletDebugAppState(bullet.getPhysicsSpace()) {
            @Override
            public void initialize(final AppStateManager stateManager, final Application app) {
                super.initialize(stateManager, app);
                viewPort.setClearFlags(false, false, false);
            }

            @Override
            public void update(float tpf) {
                super.update(tpf);
                this.physicsDebugRootNode.depthFirstTraversal(new SceneGraphVisitorAdapter() {
                    @Override
                    public void visit(final Geometry geom) {
                        geom.getMesh().setLineWidth(10);
                    }
                });
                physicsDebugRootNode.updateLogicalState(tpf);
                physicsDebugRootNode.updateGeometricState();
            }
        };
        stateManager.attach(debug);
        flyCam.setMoveSpeed(30);
        Node characterNode = new Node("character node");
        characterNode.setLocalTranslation(new Vector3f(0, 5, 0));
        physicsCharacter = new BetterCharacterControl(0.3f, 2.5f, 8f);
        characterNode.addControl(physicsCharacter);
        physicsCharacter.warp(new Vector3f(5, 5, 5));
        bullet.getPhysicsSpace().add(physicsCharacter);
        Spatial model = assetManager.loadModel("Models/Jaime/Jaime.j3o");
        model.setLocalScale(1.50f);
        characterNode.attachChild(model);
        rootNode.attachChild(characterNode);
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1));
        rootNode.addLight(al);
        camLight = new PointLight();
        camLight.setColor(ColorRGBA.White);
        camLight.setRadius(20);
        rootNode.addLight(camLight);
        // mid
        Node wmain = World.generate(assetManager);
        final int o = 100;
        float[] reps = {
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
        };
        levelNode = new Node("Level");
        for(int i = 0; i < reps.length; i += 3) {
            Spatial gho = wmain.clone();
            Vector3f dir = new Vector3f(reps[i], reps[i + 1], reps[i + 2]);
            gho.setLocalTranslation(dir);
            levelNode.attachChild(gho);
            if(i == 0) {
                gho.breadthFirstTraversal(new SceneGraphVisitorAdapter() {
                    @Override
                    public void visit(final Geometry geom) {
                        bullet.getPhysicsSpace().add(geom);
                    }
                });
            }
        }
        rootNode.attachChild(levelNode);
    }

    private Spatial warp(int o) {
        warpTex = new Material(assetManager, "MatDefs/Electricity/Electricity3.j3md");
        final Texture tex = assetManager.loadTexture("textures/wall.png");
        tex.setWrap(WrapMode.Repeat);
        warpTex.setTexture("noise", tex);
        warpTex.setColor("color", new ColorRGBA(1f, 0f, 0f, .5f));
        warpTex.setFloat("speed", 0.01f);
        warpTex.setFloat("width", -FastMath.ZERO_TOLERANCE * 10);
        warpTex.setVector2("texScale", new Vector2f(1, 1));
        warpTex.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        warpTex.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        final Geometry warp = new Geometry("warp", new Box(o / 2f, o / 2f, o / 2f));
        warp.setLocalTranslation(50, 50, 50);
        warp.setMaterial(warpTex);
        warp.setQueueBucket(Bucket.Transparent);
        return warp;
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        physicsCharacter.jump();
        camLight.setPosition(cam.getLocation());
    }
}
