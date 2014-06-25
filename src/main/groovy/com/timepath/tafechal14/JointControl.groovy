package com.timepath.tafechal14

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class JointControl extends AbstractControl {

    Closure target

    @Override
    protected void controlUpdate(float tpf) {
        target(spatial.localTranslation)
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }
}
