package com.timepath.tafechal14

import com.jme3.bullet.collision.PhysicsCollisionEvent
import com.jme3.bullet.collision.PhysicsCollisionListener
import com.jme3.bullet.control.RigidBodyControl
import com.jme3.math.Vector3f
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class BouncyListener implements PhysicsCollisionListener {

    float scale

    @Override
    void collision(PhysicsCollisionEvent event) {
        def a = event.nodeA.getControl(RigidBodyControl)
        def b = event.nodeB.getControl(RigidBodyControl)
        if (!(a?.mass && b?.mass)) return
        a.applyImpulse(b.physicsLocation.subtract(b.physicsLocation).negate().mult(scale), Vector3f.ZERO)
        b.applyImpulse(a.physicsLocation.subtract(b.physicsLocation).negate().mult(scale), Vector3f.ZERO)
    }
}
