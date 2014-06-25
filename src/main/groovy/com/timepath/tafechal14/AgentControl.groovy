package com.timepath.tafechal14

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
class AgentControl extends RigidBodyControl {

    Closure<Vector3f> target;

    float speed = 100

    @Override
    void update(float tpf) {
        super.update(tpf)
        if (!target) return
        applyCentralForce target()?.subtract(physicsLocation)?.normalizeLocal()?.multLocal(speed)
    }
}
