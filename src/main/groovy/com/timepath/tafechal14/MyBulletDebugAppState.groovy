package com.timepath.tafechal14

import com.jme3.app.Application
import com.jme3.app.state.AppStateManager
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.debug.BulletDebugAppState
import com.jme3.scene.Geometry
import com.jme3.scene.SceneGraphVisitorAdapter
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class MyBulletDebugAppState extends BulletDebugAppState {

    MyBulletDebugAppState(final PhysicsSpace space) {
        super(space)
    }

    @Override
    void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app)
        viewPort.setClearFlags(false, false, false)
    }

    @Override
    void update(float tpf) {
        super.update(tpf)
        this.physicsDebugRootNode.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            void visit(Geometry geom) {
                geom.getMesh().setLineWidth(10)
            }
        })
        physicsDebugRootNode.updateLogicalState(tpf)
        physicsDebugRootNode.updateGeometricState()
    }
}
