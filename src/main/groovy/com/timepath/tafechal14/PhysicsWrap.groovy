package com.timepath.tafechal14

import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.PhysicsTickListener
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class PhysicsWrap implements PhysicsTickListener {

    float scale

    @Override
    void prePhysicsTick(final PhysicsSpace space, final float tpf) {
        for (prb in space.rigidBodyList) {
            if (!prb.mass) continue
            def pos = prb.physicsLocation
            pos.x = (pos.x + scale) % scale
            pos.y = (pos.y + scale) % scale
            pos.z = (pos.z + scale) % scale
            prb.physicsLocation = pos
        }
    }

    @Override
    void physicsTick(final PhysicsSpace space, final float tpf) {

    }
}
