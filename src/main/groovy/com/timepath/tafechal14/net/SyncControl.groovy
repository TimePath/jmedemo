package com.timepath.tafechal14.net

import com.jme3.math.ColorRGBA
import com.jme3.network.AbstractMessage
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import com.timepath.tafechal14.net.messages.EntityUpdate
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class SyncControl extends AbstractControl {

    float ticks
    int id
    Closure<Void> broadcast = { AbstractMessage m -> }
    Closure<? extends AbstractMessage> create = {
        new EntityUpdate(id: id, pos: spatial.localTranslation, color: ColorRGBA.randomColor())
    }

    @Override
    protected void controlUpdate(float tpf) {
        ticks += tpf
        if (ticks > 0.1f) {
            ticks = 0
            broadcast(create())
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }
}
